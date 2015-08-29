/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators;

import java.util.Queue;
import java.util.concurrent.atomic.*;
import java.util.function.Function;

import org.reactivestreams.*;

import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class PublisherCombineLatest<T, R> implements Publisher<R> {
    final Publisher<? extends T>[] sources;
    final Iterable<? extends Publisher<? extends T>> sourcesIterable;
    final Function<? super Object[], ? extends R> combiner;
    final int bufferSize;
    final boolean delayError;
    
    public PublisherCombineLatest(Publisher<? extends T>[] sources,
            Iterable<? extends Publisher<? extends T>> sourcesIterable,
            Function<? super Object[], ? extends R> combiner, int bufferSize,
            boolean delayError) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
        this.combiner = combiner;
        this.bufferSize = bufferSize;
        this.delayError = delayError;
    }

    
    @Override
    @SuppressWarnings("unchecked")
    public void subscribe(Subscriber<? super R> s) {
        Publisher<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new Publisher[8];
            for (Publisher<? extends T> p : sourcesIterable) {
                if (count == sources.length) {
                    Publisher<? extends T>[] b = new Publisher[count + count >> 2];
                    System.arraycopy(sources, 0, b, 0, count);
                    sources = b;
                }
                sources[count++] = p;
            }
        } else {
            count = sources.length;
        }
        
        if (count == 0) {
            EmptySubscription.complete(s);
            return;
        }
        
        LatestCoordinator<T, R> lc = new LatestCoordinator<>(s, combiner, count, bufferSize, delayError);
        lc.subscribe(sources);
    }
    
    static final class LatestCoordinator<T, R> extends AtomicInteger implements Subscription {
        /** */
        private static final long serialVersionUID = 8567835998786448817L;
        final Subscriber<? super R> actual;
        final Function<? super Object[], ? extends R> combiner;
        final int count;
        final CombinerSubscriber<T, R>[] subscribers;
        final int bufferSize;
        final Object[] latest;
        final SpscLinkedArrayQueue<Object> queue;
        final boolean delayError;
        
        volatile boolean cancelled;
        
        volatile boolean done;
        
        volatile long requested;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<LatestCoordinator> REQUESTED =
                AtomicLongFieldUpdater.newUpdater(LatestCoordinator.class, "requested");

        volatile Throwable error;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<LatestCoordinator, Throwable> ERROR =
                AtomicReferenceFieldUpdater.newUpdater(LatestCoordinator.class, Throwable.class, "error");
        
        
        int active;
        int complete;
        
        @SuppressWarnings("unchecked")
        public LatestCoordinator(Subscriber<? super R> actual, 
                Function<? super Object[], ? extends R> combiner, 
                int count, int bufferSize, boolean delayError) {
            this.actual = actual;
            this.combiner = combiner;
            this.count = count;
            this.bufferSize = bufferSize;
            this.delayError = delayError;
            this.latest = new Object[count];
            this.subscribers = new CombinerSubscriber[count];
            this.queue = new SpscLinkedArrayQueue<>(bufferSize);
        }
        
        public void subscribe(Publisher<? extends T>[] sources) {
            Subscriber<T>[] as = subscribers;
            int len = as.length;
            for (int i = 0; i < len; i++) {
                as[i] = new CombinerSubscriber<>(this, i);
            }
            lazySet(0); // release array contents
            actual.onSubscribe(this);
            for (int i = 0; i < len; i++) {
                if (cancelled) {
                    return;
                }
                sources[i].subscribe(as[i]);
            }
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            BackpressureHelper.add(REQUESTED, this, n);
            drain();
        }
        
        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                
                if (getAndIncrement() == 0) {
                    cancel(queue);
                }
            }
        }
        
        void cancel(Queue<?> q) {
            q.clear();
            for (CombinerSubscriber<T, R> s : subscribers) {
                s.cancel();
            }
        }
        
        void combine(T value, int index) {
            
            CombinerSubscriber<T, R> cs = subscribers[index];
            
            int a;
            int c;
            int len;
            boolean empty;
            boolean f;
            synchronized (this) {
                len = latest.length;
                Object o = latest[index];
                a = active;
                if (o == null) {
                    active = ++a;
                }
                c = complete;
                if (value == null) {
                    complete = ++c;
                } else {
                    latest[index] = value;
                }
                f = a == len;
                empty = c == len;
                if (!empty) {
                    queue.offer(cs, latest.clone());
                } else {
                    done = true;
                }
            }
            if (!f && value != null) {
                cs.request(1);
                return;
            }
            drain();
        }
        
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }
            
            final Queue<Object> q = queue;
            final Subscriber<? super R> a = actual;
            final boolean delayError = this.delayError;
            
            int missed = 1;
            for (;;) {
                
                if (checkTerminated(done, q.isEmpty(), a, q, delayError)) {
                    return;
                }
                
                long r = requested;
                boolean unbounded = r == Long.MAX_VALUE;
                long e = 0L;
                
                while (r != 0L) {
                    
                    boolean d = done;
                    @SuppressWarnings("unchecked")
                    CombinerSubscriber<T, R> cs = (CombinerSubscriber<T, R>)q.peek();
                    boolean empty = cs == null;
                    
                    if (checkTerminated(d, empty, a, q, delayError)) {
                        return;
                    }
                    
                    if (empty) {
                        break;
                    }

                    q.poll();
                    Object[] array = (Object[])q.poll();
                    
                    if (array == null) {
                        cancelled = true;
                        cancel(q);
                        a.onError(new IllegalStateException("Broken queue?! Sender received but not the array."));
                        return;
                    }
                    
                    R v;
                    try {
                        v = combiner.apply(array);
                    } catch (Throwable ex) {
                        cancelled = true;
                        cancel(q);
                        a.onError(ex);
                        return;
                    }
                    
                    a.onNext(v);
                    
                    cs.request(1);
                    
                    r--;
                    e--;
                }
                
                if (e != 0) {
                    if (!unbounded) {
                        REQUESTED.addAndGet(this, e);
                    }
                }
                
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
        
        
        boolean checkTerminated(boolean d, boolean empty, Subscriber<?> a, Queue<?> q, boolean delayError) {
            if (cancelled) {
                cancel(q);
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
                        cancel(q);
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
        
        void onError(Throwable e) {
            for (;;) {
                Throwable curr = error;
                if (curr != null) {
                    e.addSuppressed(curr);
                }
                Throwable next = e;
                if (ERROR.compareAndSet(this, curr, next)) {
                    return;
                }
            }
        }
    }
    
    static final class CombinerSubscriber<T, R> implements Subscriber<T>, Subscription {
        final LatestCoordinator<T, R> parent;
        final int index;
        
        boolean done;
        
        volatile Subscription s;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<CombinerSubscriber, Subscription> S =
                AtomicReferenceFieldUpdater.newUpdater(CombinerSubscriber.class, Subscription.class, "s");
        
        static final Subscription CANCELLED = new Subscription() {
            @Override
            public void request(long n) {
                
            }
            
            @Override
            public void cancel() {
                
            }
        };
        
        public CombinerSubscriber(LatestCoordinator<T, R> parent, int index) {
            this.parent = parent;
            this.index = index;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (!S.compareAndSet(this, null, s)) {
                s.cancel();
                if (s != CANCELLED) {
                    SubscriptionHelper.reportSubscriptionSet();
                }
                return;
            }
            s.request(parent.bufferSize);
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            parent.combine(t, index);
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            parent.onError(t);
            done = true;
            parent.combine(null, index);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            parent.combine(null, index);
        }
        
        @Override
        public void request(long n) {
            s.request(n);
        }
        
        @Override
        public void cancel() {
            Subscription a = s;
            if (a != CANCELLED) {
                a = S.getAndSet(this, CANCELLED);
                if (a != CANCELLED && a != null) {
                    a.cancel();
                }
            }
        }
    }
}
