/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.flowable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Flowable.Operator;
import io.reactivex.flowables.GroupedFlowable;
import io.reactivex.functions.Function;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class OperatorGroupBy<T, K, V> implements Operator<GroupedFlowable<K, V>, T>{
    final Function<? super T, ? extends K> keySelector;
    final Function<? super T, ? extends V> valueSelector;
    final int bufferSize;
    final boolean delayError;
    
    public OperatorGroupBy(Function<? super T, ? extends K> keySelector, Function<? super T, ? extends V> valueSelector, int bufferSize, boolean delayError) {
        this.keySelector = keySelector;
        this.valueSelector = valueSelector;
        this.bufferSize = bufferSize;
        this.delayError = delayError;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super GroupedFlowable<K, V>> t) {
        return new GroupBySubscriber<T, K, V>(t, keySelector, valueSelector, bufferSize, delayError);
    }
    
    public static final class GroupBySubscriber<T, K, V> 
    extends AtomicInteger
    implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -3688291656102519502L;
        
        final Subscriber<? super GroupedFlowable<K, V>> actual;
        final Function<? super T, ? extends K> keySelector;
        final Function<? super T, ? extends V> valueSelector;
        final int bufferSize;
        final boolean delayError;
        final Map<Object, GroupedUnicast<K, V>> groups;
        final Queue<GroupedFlowable<K, V>> queue;
        
        static final Object NULL_KEY = new Object();
        
        Subscription s;
        
        final AtomicBoolean cancelled = new AtomicBoolean();

        final AtomicLong requested = new AtomicLong();
        
        final AtomicInteger groupCount = new AtomicInteger(1);
        
        Throwable error;
        volatile boolean done;
        
        public GroupBySubscriber(Subscriber<? super GroupedFlowable<K, V>> actual, Function<? super T, ? extends K> keySelector, Function<? super T, ? extends V> valueSelector, int bufferSize, boolean delayError) {
            this.actual = actual;
            this.keySelector = keySelector;
            this.valueSelector = valueSelector;
            this.bufferSize = bufferSize;
            this.delayError = delayError;
            this.groups = new ConcurrentHashMap<Object, GroupedUnicast<K, V>>();
            this.queue = new SpscLinkedArrayQueue<GroupedFlowable<K, V>>(bufferSize);
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            
            this.s = s;
            actual.onSubscribe(this);
            s.request(bufferSize);
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            final Queue<GroupedFlowable<K, V>> q = this.queue;
            final Subscriber<? super GroupedFlowable<K, V>> a = this.actual;

            K key;
            try {
                key = keySelector.apply(t);
            } catch (Throwable ex) {
                s.cancel();
                errorAll(a, q, ex);
                return;
            }
            
            boolean notNew = true;
            Object mapKey = key != null ? key : NULL_KEY;
            GroupedUnicast<K, V> group = groups.get(mapKey);
            if (group == null) {
                // if the main has been cancelled, stop creating groups
                // and skip this value
                if (!cancelled.get()) {
                    group = GroupedUnicast.createWith(key, bufferSize, this, delayError);
                    groups.put(mapKey, group);
                    
                    groupCount.getAndIncrement();
                    
                    notNew = false;
                    q.offer(group);
                    drain();
                } else {
                    return;
                }
            }
            
            V v;
            try {
                v = valueSelector.apply(t);
            } catch (Throwable ex) {
                s.cancel();
                errorAll(a, q, ex);
                return;
            }
            
            if (v == null) {
                s.cancel();
                errorAll(a, q, new NullPointerException("The valueSelector returned null"));
                return;
            }

            group.onNext(v);

            if (notNew) {
                s.request(1);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            error = t;
            done = true;
            groupCount.decrementAndGet();
            drain();
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            groupCount.decrementAndGet();
            drain();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            
            BackpressureHelper.add(requested, n);
            drain();
        }
        
        @Override
        public void cancel() {
            // cancelling the main source means we don't want any more groups
            // but running groups still require new values
            if (cancelled.compareAndSet(false, true)) {
                if (groupCount.decrementAndGet() == 0) {
                    s.cancel();
                }
            }
        }
        
        public void cancel(K key) {
            Object mapKey = key != null ? key : NULL_KEY;
            groups.remove(mapKey);
            if (groupCount.decrementAndGet() == 0) {
                s.cancel();
            }
        }
        
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            
            int missed = 1;
            
            final Queue<GroupedFlowable<K, V>> q = this.queue;
            final Subscriber<? super GroupedFlowable<K, V>> a = this.actual;
            
            for (;;) {
                
                if (checkTerminated(done, q.isEmpty(), a, q)) {
                    return;
                }
                
                long r = requested.get();
                boolean unbounded = r == Long.MAX_VALUE;
                long e = 0L;
                
                while (r != 0) {
                    boolean d = done;
                    
                    GroupedFlowable<K, V> t = q.poll();
                    
                    boolean empty = t == null;
                    
                    if (checkTerminated(d, empty, a, q)) {
                        return;
                    }
                    
                    if (empty) {
                        break;
                    }

                    a.onNext(t);
                    
                    r--;
                    e--;
                }
                
                if (e != 0L) {
                    if (!unbounded) {
                        requested.addAndGet(e);
                    }
                    s.request(-e);
                }
                
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        void errorAll(Subscriber<? super GroupedFlowable<K, V>> a, Queue<?> q, Throwable ex) {
            q.clear();
            List<GroupedUnicast<K, V>> list = new ArrayList<GroupedUnicast<K, V>>(groups.values());
            groups.clear();
            
            for (GroupedUnicast<K, V> e : list) {
                e.onError(ex);
            }
            
            a.onError(ex);
        }
        
        boolean checkTerminated(boolean d, boolean empty, 
                Subscriber<? super GroupedFlowable<K, V>> a, Queue<?> q) {
            if (d) {
                Throwable err = error;
                if (err != null) {
                    errorAll(a, q, err);
                    return true;
                } else
                if (empty) {
                    List<GroupedUnicast<K, V>> list = new ArrayList<GroupedUnicast<K, V>>(groups.values());
                    groups.clear();
                    
                    for (GroupedUnicast<K, V> e : list) {
                        e.onComplete();
                    }
                    
                    actual.onComplete();
                    return true;
                }
            }
            return false;
        }
    }
    
    static final class GroupedUnicast<K, T> extends GroupedFlowable<K, T> {
        
        public static <T, K> GroupedUnicast<K, T> createWith(K key, int bufferSize, GroupBySubscriber<?, K, T> parent, boolean delayError) {
            State<T, K> state = new State<T, K>(bufferSize, parent, key, delayError);
            return new GroupedUnicast<K, T>(key, state);
        }
        
        final State<T, K> state;
        
        protected GroupedUnicast(K key, State<T, K> state) {
            super(state, key);
            this.state = state;
        }
        
        public void onNext(T t) {
            state.onNext(t);
        }
        
        public void onError(Throwable e) {
            state.onError(e);
        }
        
        public void onComplete() {
            state.onComplete();
        }
    }
    
    static final class State<T, K> extends AtomicInteger implements Subscription, Publisher<T> {
        /** */
        private static final long serialVersionUID = -3852313036005250360L;

        final K key;
        final Queue<T> queue;
        final GroupBySubscriber<?, K, T> parent;
        final boolean delayError;
        
        final AtomicLong requested = new AtomicLong();
        
        volatile boolean done;
        Throwable error;
        
        final AtomicBoolean cancelled = new AtomicBoolean();
        
        final AtomicReference<Subscriber<? super T>> actual = new AtomicReference<Subscriber<? super T>>();

        final AtomicBoolean once = new AtomicBoolean();

        public State(int bufferSize, GroupBySubscriber<?, K, T> parent, K key, boolean delayError) {
            this.queue = new SpscLinkedArrayQueue<T>(bufferSize);
            this.parent = parent;
            this.key = key;
            this.delayError = delayError;
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            BackpressureHelper.add(requested, n);
            drain();
        }
        
        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                if (getAndIncrement() == 0) {
                    parent.cancel(key);
                }
            }
        }
        
        @Override
        public void subscribe(Subscriber<? super T> s) {
            if (once.compareAndSet(false, true)) {
                s.onSubscribe(this);
                actual.lazySet(s);
                drain();
            } else {
                EmptySubscription.error(new IllegalStateException("Only one Subscriber allowed!"), s);
            }
        }

        public void onNext(T t) {
            if (t == null) {
                error = new NullPointerException();
                done = true;
            } else {
                queue.offer(t);
            }
            drain();
        }
        
        public void onError(Throwable e) {
            error = e;
            done = true;
            drain();
        }
        
        public void onComplete() {
            done = true;
            drain();
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            int missed = 1;
            
            final Queue<T> q = queue;
            final boolean delayError = this.delayError;
            Subscriber<? super T> a = actual.get();
            for (;;) {
                if (a != null) {
                    if (checkTerminated(done, q.isEmpty(), a, delayError)) {
                        return;
                    }
                    
                    long r = requested.get();
                    boolean unbounded = r == Long.MAX_VALUE;
                    long e = 0;
                    
                    while (r != 0L) {
                        boolean d = done;
                        T v = q.poll();
                        boolean empty = v == null;
                        
                        if (checkTerminated(d, empty, a, delayError)) {
                            return;
                        }
                        
                        if (empty) {
                            break;
                        }
                        
                        a.onNext(v);
                        
                        r--;
                        e--;
                    }
                    
                    if (e != 0L) {
                        if (!unbounded) {
                            requested.addAndGet(e);
                        }
                        parent.s.request(-e);
                    }
                }
                
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
                if (a == null) {
                    a = actual.get();
                }
            }
        }
        
        boolean checkTerminated(boolean d, boolean empty, Subscriber<? super T> a, boolean delayError) {
            if (cancelled.get()) {
                queue.clear();
                parent.cancel(key);
                return true;
            }
            
            if (d) {
                if (delayError) {
                    if (empty) {
                        Throwable e = error;
                        if (e != null) {
                            a.onError(e);
                        } else {
                            a.onComplete();
                        }
                        return true;
                    }
                } else {
                    Throwable e = error;
                    if (e != null) {
                        queue.clear();
                        a.onError(e);
                        return true;
                    } else
                    if (empty) {
                        a.onComplete();
                        return true;
                    }
                }
            }
            
            return false;
        }
    }
}