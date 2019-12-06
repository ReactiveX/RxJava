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

package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.flowables.GroupedFlowable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

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
        } catch (Throwable e) {
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
    extends AtomicLong
    implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -3688291656102519502L;

        final Subscriber<? super GroupedFlowable<K, V>> downstream;
        final Function<? super T, ? extends K> keySelector;
        final Function<? super T, ? extends V> valueSelector;
        final int bufferSize;
        final int limit;
        final boolean delayError;
        final Map<Object, GroupedUnicast<K, V>> groups;
        final Queue<GroupedUnicast<K, V>> evictedGroups;

        static final Object NULL_KEY = new Object();

        Subscription upstream;

        final AtomicBoolean cancelled = new AtomicBoolean();

        long emittedGroups;

        final AtomicInteger groupCount = new AtomicInteger(1);

        final AtomicLong groupConsumed = new AtomicLong();

        boolean done;

        public GroupBySubscriber(Subscriber<? super GroupedFlowable<K, V>> actual, Function<? super T, ? extends K> keySelector,
                Function<? super T, ? extends V> valueSelector, int bufferSize, boolean delayError,
                Map<Object, GroupedUnicast<K, V>> groups, Queue<GroupedUnicast<K, V>> evictedGroups) {
            this.downstream = actual;
            this.keySelector = keySelector;
            this.valueSelector = valueSelector;
            this.bufferSize = bufferSize;
            this.limit = bufferSize - (bufferSize >> 2);
            this.delayError = delayError;
            this.groups = groups;
            this.evictedGroups = evictedGroups;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
                s.request(bufferSize);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            K key;
            try {
                key = keySelector.apply(t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.cancel();
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
                v = ExceptionHelper.nullCheck(valueSelector.apply(t), "The valueSelector returned a null value.");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.cancel();
                if (newGroup) {
                    if (emittedGroups != get()) {
                        downstream.onNext(group);
                    } else {
                        MissingBackpressureException mbe = new MissingBackpressureException(groupHangWarning(emittedGroups));
                        mbe.initCause(ex);
                        onError(mbe);
                        return;
                    }
                }
                onError(ex);
                return;
            }

            group.onNext(v);

            completeEvictions();

            if (newGroup) {
                if (emittedGroups != get()) {
                    emittedGroups++;
                    downstream.onNext(group);
                    if (group.state.tryAbandon()) {
                        cancel(key);
                        group.onComplete();

                        requestGroup(1);
                    }
                } else {
                    upstream.cancel();
                    onError(new MissingBackpressureException(groupHangWarning(emittedGroups)));
                }
            }
        }

        static String groupHangWarning(long n) {
            return "Unable to emit a new group (#" + n + ") due to lack of requests. Please make sure the downstream can always accept a new group as well as each group is consumed in order for the whole operator to be able to proceed.";
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            for (GroupedUnicast<K, V> g : groups.values()) {
                g.onError(t);
            }
            groups.clear();
            if (evictedGroups != null) {
                evictedGroups.clear();
            }
            downstream.onError(t);
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
                downstream.onComplete();
            }
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(this, n);
            }
        }

        @Override
        public void cancel() {
            // cancelling the main source means we don't want any more groups
            // but running groups still require new values
            if (cancelled.compareAndSet(false, true)) {
                completeEvictions();
                if (groupCount.decrementAndGet() == 0) {
                    upstream.cancel();
                }
            }
        }

        private void completeEvictions() {
            if (evictedGroups != null) {
                int count = 0;
                GroupedUnicast<K, V> evictedGroup;
                while ((evictedGroup = evictedGroups.poll()) != null) {
                    evictedGroup.onComplete();
                    count++;
                }
                if (count != 0) {
                    groupCount.addAndGet(-count);
                }
            }
        }

        public void cancel(K key) {
            Object mapKey = key != null ? key : NULL_KEY;
            groups.remove(mapKey);
            if (groupCount.decrementAndGet() == 0) {
                upstream.cancel();
            }
        }

        void requestGroup(long n) {
            // lots of atomics, save local
            AtomicLong groupConsumed = this.groupConsumed;
            int limit = this.limit;
            // Concurrent groups can request at once, a CAS loop is needed
            for (;;) {
                long currentConsumed = groupConsumed.get();
                long newConsumed = BackpressureHelper.addCap(currentConsumed, n);
                // Accumulate the consumed amounts and atomically update the total
                if (groupConsumed.compareAndSet(currentConsumed, newConsumed)) {
                    // if successful, let's see if the prefetch limit has been surpassed
                    for (;;) {
                        if (newConsumed < limit) {
                            // no further actions to be taken
                            return;
                        }

                        // Yes, remove one limit from total consumed
                        long newConsumedAfterLimit = newConsumed - limit;
                        // Only one thread should subtract
                        if (groupConsumed.compareAndSet(newConsumed, newConsumedAfterLimit)) {
                            // Then request up to limit
                            upstream.request(limit);
                        }
                        // We don't quit but loop to see if we are still above the prefetch limit
                        newConsumed = groupConsumed.get();
                    }
                }
            }
        }
    }

    static final class EvictionAction<K, V> implements Consumer<GroupedUnicast<K, V>> {

        final Queue<GroupedUnicast<K, V>> evictedGroups;

        EvictionAction(Queue<GroupedUnicast<K, V>> evictedGroups) {
            this.evictedGroups = evictedGroups;
        }

        @Override
        public void accept(GroupedUnicast<K, V> value) {
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

        boolean outputFused;
        int produced;

        final AtomicInteger once = new AtomicInteger();

        static final int FRESH = 0;
        static final int HAS_SUBSCRIBER = 1;
        static final int ABANDONED = 2;
        static final int ABANDONED_HAS_SUBSCRIBER = ABANDONED | HAS_SUBSCRIBER;

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
                cancelParent();
            }
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            for (;;) {
                int s = once.get();
                if ((s & HAS_SUBSCRIBER) != 0) {
                    break;
                }
                int u = s | HAS_SUBSCRIBER;
                if (once.compareAndSet(s, u)) {
                    subscriber.onSubscribe(this);
                    actual.lazySet(subscriber);
                    if (cancelled.get()) {
                        actual.lazySet(null);
                    } else {
                        drain();
                    }
                    return;
                }
            }
            EmptySubscription.error(new IllegalStateException("Only one Subscriber allowed!"), subscriber);
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

        void cancelParent() {
            if ((once.get() & ABANDONED) == 0) {
                parent.cancel(key);
            }
        }

        boolean tryAbandon() {
            return once.get() == FRESH && once.compareAndSet(FRESH, ABANDONED);
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

                        if (checkTerminated(d, empty, a, delayError, e)) {
                            return;
                        }

                        if (empty) {
                            break;
                        }

                        a.onNext(v);

                        e++;
                    }

                    if (e == r && checkTerminated(done, q.isEmpty(), a, delayError, e)) {
                        return;
                    }

                    if (e != 0L) {
                        BackpressureHelper.produced(requested, e);
                        // replenish based on this batch run
                        requestParent(e);
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

        void requestParent(long e) {
            if ((once.get() & ABANDONED) == 0) {
                parent.requestGroup(e);
            }
        }

        boolean checkTerminated(boolean d, boolean empty, Subscriber<? super T> a, boolean delayError, long emitted) {
            if (cancelled.get()) {
                // if this group is canceled, all accumulated emissions and
                // remaining items in the queue should be requested
                // so that other groups can proceed
                while (queue.poll() != null) {
                    emitted++;
                }
                if (emitted != 0L) {
                    requestParent(emitted);
                }
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

        void tryReplenish() {
            int p = produced;
            if (p != 0) {
                produced = 0;
                requestParent(p);
            }
        }

        @Nullable
        @Override
        public T poll() {
            T v = queue.poll();
            if (v != null) {
                produced++;
                return v;
            }
            tryReplenish();
            return null;
        }

        @Override
        public boolean isEmpty() {
            if (queue.isEmpty()) {
                tryReplenish();
                return true;
            }
            tryReplenish();
            return false;
        }

        @Override
        public void clear() {
            SpscLinkedArrayQueue<T> q = queue;
            // queue.clear() would drop submitted items and not replenish, possibly hanging other groups
            while (q.poll() != null) {
                produced++;
            }
            tryReplenish();
        }
    }
}
