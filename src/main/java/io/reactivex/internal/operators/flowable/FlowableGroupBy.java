/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.annotations.Nullable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.flowables.GroupedFlowable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.internal.util.EmptyComponent;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableGroupBy<T, K, V> extends AbstractFlowableWithUpstream<T, GroupedFlowable<K, V>> {
    final Function<? super T, ? extends K> keySelector;
    final Function<? super T, ? extends V> valueSelector;
    final int bufferSize;
    final boolean delayError;
    final Function<? super Consumer<Object>, ? extends Map<K, Object>> mapFactory;

    public FlowableGroupBy(Flowable<T> source, Function<? super T, ? extends K> keySelector, Function<? super T, ? extends V> valueSelector,
            int bufferSize, boolean delayError, Function<? super Consumer<Object>, ? extends Map<K, Object>> mapFactory) {
        super(source);
        this.keySelector = keySelector;
        this.valueSelector = valueSelector;
        this.bufferSize = bufferSize;
        this.delayError = delayError;
        this.mapFactory = mapFactory;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void subscribeActual(Subscriber<? super GroupedFlowable<K, V>> s) {

        final Map<Object, GroupedUnicast<K, V>> groups;
        final Queue<GroupedUnicast<K, V>> evictedGroups;

        try {
            if (mapFactory == null) {
                evictedGroups = null;
                groups = new ConcurrentHashMap<Object, GroupedUnicast<K, V>>();
            } else {
                evictedGroups = new ConcurrentLinkedQueue<GroupedUnicast<K, V>>();
                Consumer<Object> evictionAction = (Consumer) new EvictionAction<K, V>(evictedGroups);
                groups = (Map) mapFactory.apply(evictionAction);
            }
        } catch (Exception e) {
            Exceptions.throwIfFatal(e);
            s.onSubscribe(EmptyComponent.INSTANCE);
            s.onError(e);
            return;
        }
        GroupBySubscriber<T, K, V> subscriber =
                new GroupBySubscriber<T, K, V>(s, keySelector, valueSelector, bufferSize, delayError, groups, evictedGroups);
        source.subscribe(subscriber);
    }

    public static final class GroupBySubscriber<T, K, V>
    extends BasicIntQueueSubscription<GroupedFlowable<K, V>>
    implements FlowableSubscriber<T> {

        private static final long serialVersionUID = -3688291656102519502L;

        final Subscriber<? super GroupedFlowable<K, V>> actual;
        final Function<? super T, ? extends K> keySelector;
        final Function<? super T, ? extends V> valueSelector;
        final int bufferSize;
        final boolean delayError;
        final Map<Object, GroupedUnicast<K, V>> groups;
        final SpscLinkedArrayQueue<GroupedFlowable<K, V>> queue;
        final Queue<GroupedUnicast<K, V>> evictedGroups;

        static final Object NULL_KEY = new Object();

        Subscription s;

        final AtomicBoolean cancelled = new AtomicBoolean();

        final AtomicLong requested = new AtomicLong();

        final AtomicInteger groupCount = new AtomicInteger(1);

        Throwable error;
        volatile boolean done;

        boolean outputFused;

        public GroupBySubscriber(Subscriber<? super GroupedFlowable<K, V>> actual, Function<? super T, ? extends K> keySelector,
                Function<? super T, ? extends V> valueSelector, int bufferSize, boolean delayError,
                Map<Object, GroupedUnicast<K, V>> groups, Queue<GroupedUnicast<K, V>> evictedGroups) {
            this.actual = actual;
            this.keySelector = keySelector;
            this.valueSelector = valueSelector;
            this.bufferSize = bufferSize;
            this.delayError = delayError;
            this.groups = groups;
            this.evictedGroups = evictedGroups;
            this.queue = new SpscLinkedArrayQueue<GroupedFlowable<K, V>>(bufferSize);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                s.request(bufferSize);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            final SpscLinkedArrayQueue<GroupedFlowable<K, V>> q = this.queue;

            K key;
            try {
                key = keySelector.apply(t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.cancel();
                onError(ex);
                return;
            }

            boolean newGroup = false;
            Object mapKey = key != null ? key : NULL_KEY;
            GroupedUnicast<K, V> group = groups.get(mapKey);
            if (group == null) {
                // if the main has been cancelled, stop creating groups
                // and skip this value
                if (cancelled.get()) {
                    return;
                }

                group = GroupedUnicast.createWith(key, bufferSize, this, delayError);
                groups.put(mapKey, group);

                groupCount.getAndIncrement();

                newGroup = true;
            }

            V v;
            try {
                v = ObjectHelper.requireNonNull(valueSelector.apply(t), "The valueSelector returned null");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.cancel();
                onError(ex);
                return;
            }

            group.onNext(v);

            if (evictedGroups != null) {
                GroupedUnicast<K, V> evictedGroup;
                while ((evictedGroup = evictedGroups.poll()) != null) {
                    evictedGroup.onComplete();
                }
            }

            if (newGroup) {
                q.offer(group);
                drain();
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            for (GroupedUnicast<K, V> g : groups.values()) {
                g.onError(t);
            }
            groups.clear();
            if (evictedGroups != null) {
                evictedGroups.clear();
            }
            error = t;
            done = true;
            drain();
        }

        @Override
        public void onComplete() {
            if (!done) {
                for (GroupedUnicast<K, V> g : groups.values()) {
                    g.onComplete();
                }
                groups.clear();
                if (evictedGroups != null) {
                    evictedGroups.clear();
                }
                done = true;
                drain();
            }
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
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

                if (getAndIncrement() == 0) {
                    queue.clear();
                }
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            if (outputFused) {
                drainFused();
            } else {
                drainNormal();
            }
        }

        void drainFused() {
            int missed = 1;

            final SpscLinkedArrayQueue<GroupedFlowable<K, V>> q = this.queue;
            final Subscriber<? super GroupedFlowable<K, V>> a = this.actual;

            for (;;) {
                if (cancelled.get()) {
                    q.clear();
                    return;
                }

                boolean d = done;

                if (d && !delayError) {
                    Throwable ex = error;
                    if (ex != null) {
                        q.clear();
                        a.onError(ex);
                        return;
                    }
                }

                a.onNext(null);

                if (d) {
                    Throwable ex = error;
                    if (ex != null) {
                        a.onError(ex);
                    } else {
                        a.onComplete();
                    }
                    return;
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    return;
                }
            }
        }

        void drainNormal() {
            int missed = 1;

            final SpscLinkedArrayQueue<GroupedFlowable<K, V>> q = this.queue;
            final Subscriber<? super GroupedFlowable<K, V>> a = this.actual;

            for (;;) {

                long r = requested.get();
                long e = 0L;

                while (e != r) {
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

                    e++;
                }

                if (e == r && checkTerminated(done, q.isEmpty(), a, q)) {
                    return;
                }

                if (e != 0L) {
                    if (r != Long.MAX_VALUE) {
                        requested.addAndGet(-e);
                    }
                    s.request(e);
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        boolean checkTerminated(boolean d, boolean empty, Subscriber<?> a, SpscLinkedArrayQueue<?> q) {
            if (cancelled.get()) {
                q.clear();
                return true;
            }

            if (delayError) {
                if (d && empty) {
                    Throwable ex = error;
                    if (ex != null) {
                        a.onError(ex);
                    } else {
                        a.onComplete();
                    }
                    return true;
                }
            } else {
                if (d) {
                    Throwable ex = error;
                    if (ex != null) {
                        q.clear();
                        a.onError(ex);
                        return true;
                    } else if (empty) {
                        a.onComplete();
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & ASYNC) != 0) {
                outputFused = true;
                return ASYNC;
            }
            return NONE;
        }

        @Nullable
        @Override
        public GroupedFlowable<K, V> poll() {
            return queue.poll();
        }

        @Override
        public void clear() {
            queue.clear();
        }

        @Override
        public boolean isEmpty() {
            return queue.isEmpty();
        }
    }

    static final class EvictionAction<K, V> implements Consumer<GroupedUnicast<K,V>> {

        final Queue<GroupedUnicast<K, V>> evictedGroups;

        EvictionAction(Queue<GroupedUnicast<K, V>> evictedGroups) {
            this.evictedGroups = evictedGroups;
        }

        @Override
        public void accept(GroupedUnicast<K,V> value) {
            evictedGroups.offer(value);
        }
    }

    static final class GroupedUnicast<K, T> extends GroupedFlowable<K, T> {

        final State<T, K> state;

        public static <T, K> GroupedUnicast<K, T> createWith(K key, int bufferSize, GroupBySubscriber<?, K, T> parent, boolean delayError) {
            State<T, K> state = new State<T, K>(bufferSize, parent, key, delayError);
            return new GroupedUnicast<K, T>(key, state);
        }

        protected GroupedUnicast(K key, State<T, K> state) {
            super(key);
            this.state = state;
        }

        @Override
        protected void subscribeActual(Subscriber<? super T> s) {
            state.subscribe(s);
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

    static final class State<T, K> extends BasicIntQueueSubscription<T> implements Publisher<T> {

        private static final long serialVersionUID = -3852313036005250360L;

        final K key;
        final SpscLinkedArrayQueue<T> queue;
        final GroupBySubscriber<?, K, T> parent;
        final boolean delayError;

        final AtomicLong requested = new AtomicLong();

        volatile boolean done;
        Throwable error;

        final AtomicBoolean cancelled = new AtomicBoolean();

        final AtomicReference<Subscriber<? super T>> actual = new AtomicReference<Subscriber<? super T>>();

        final AtomicBoolean once = new AtomicBoolean();

        boolean outputFused;

        int produced;

        State(int bufferSize, GroupBySubscriber<?, K, T> parent, K key, boolean delayError) {
            this.queue = new SpscLinkedArrayQueue<T>(bufferSize);
            this.parent = parent;
            this.key = key;
            this.delayError = delayError;
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                parent.cancel(key);
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
            queue.offer(t);
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
            if (outputFused) {
                drainFused();
            } else {
                drainNormal();
            }
        }

        void drainFused() {
            int missed = 1;

            final SpscLinkedArrayQueue<T> q = this.queue;
            Subscriber<? super T> a = this.actual.get();

            for (;;) {
                if (a != null) {
                    if (cancelled.get()) {
                        q.clear();
                        return;
                    }

                    boolean d = done;

                    if (d && !delayError) {
                        Throwable ex = error;
                        if (ex != null) {
                            q.clear();
                            a.onError(ex);
                            return;
                        }
                    }

                    a.onNext(null);

                    if (d) {
                        Throwable ex = error;
                        if (ex != null) {
                            a.onError(ex);
                        } else {
                            a.onComplete();
                        }
                        return;
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    return;
                }

                if (a == null) {
                    a = this.actual.get();
                }
            }
        }

        void drainNormal() {
            int missed = 1;

            final SpscLinkedArrayQueue<T> q = queue;
            final boolean delayError = this.delayError;
            Subscriber<? super T> a = actual.get();
            for (;;) {
                if (a != null) {
                    long r = requested.get();
                    long e = 0;

                    while (e != r) {
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

                        e++;
                    }

                    if (e == r && checkTerminated(done, q.isEmpty(), a, delayError)) {
                        return;
                    }

                    if (e != 0L) {
                        if (r != Long.MAX_VALUE) {
                            requested.addAndGet(-e);
                        }
                        parent.s.request(e);
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

        @Override
        public int requestFusion(int mode) {
            if ((mode & ASYNC) != 0) {
                outputFused = true;
                return ASYNC;
            }
            return NONE;
        }

        @Nullable
        @Override
        public T poll() {
            T v = queue.poll();
            if (v != null) {
                produced++;
                return v;
            }
            int p = produced;
            if (p != 0) {
                produced = 0;
                parent.s.request(p);
            }
            return null;
        }

        @Override
        public boolean isEmpty() {
            return queue.isEmpty();
        }

        @Override
        public void clear() {
            queue.clear();
        }
    }
}
