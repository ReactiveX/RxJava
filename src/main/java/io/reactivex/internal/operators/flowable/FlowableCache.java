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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * An observable which auto-connects to another observable, caches the elements
 * from that observable but allows terminating the connection and completing the cache.
 *
 * @param <T> the source element type
 */
public final class FlowableCache<T> extends AbstractFlowableWithUpstream<T, T> {
    /** The cache and replay state. */
    final CacheState<T> state;

    final AtomicBoolean once;

    /**
     * Private constructor because state needs to be shared between the Observable body and
     * the onSubscribe function.
     * @param source the upstream source whose signals to cache
     * @param capacityHint the capacity hint
     */
    public FlowableCache(Flowable<T> source, int capacityHint) {
        super(source);
        this.state = new CacheState<T>(source, capacityHint);
        this.once = new AtomicBoolean();
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> t) {
        // we can connect first because we replay everything anyway
        ReplaySubscription<T> rp = new ReplaySubscription<T>(t, state);
        state.addChild(rp);

        t.onSubscribe(rp);

        // we ensure a single connection here to save an instance field of AtomicBoolean in state.
        if (!once.get() && once.compareAndSet(false, true)) {
            state.connect();
        }

        // no need to call rp.replay() here because the very first request will trigger it anyway
    }

    /**
     * Check if this cached observable is connected to its source.
     * @return true if already connected
     */
    /* public */boolean isConnected() {
        return state.isConnected;
    }

    /**
     * Returns true if there are observers subscribed to this observable.
     * @return true if the cache has Subscribers
     */
    /* public */ boolean hasSubscribers() {
        return state.subscribers.get().length != 0;
    }

    /**
     * Returns the number of events currently cached.
     * @return the number of currently cached event count
     */
    /* public */ int cachedEventCount() {
        return state.size();
    }

    /**
     * Contains the active child subscribers and the values to replay.
     *
     * @param <T> the value type of the cached items
     */
    static final class CacheState<T> extends LinkedArrayList implements FlowableSubscriber<T> {
        /** The source observable to connect to. */
        final Flowable<T> source;
        /** Holds onto the subscriber connected to source. */
        final AtomicReference<Subscription> connection = new AtomicReference<Subscription>();
        /** Guarded by connection (not this). */
        final AtomicReference<ReplaySubscription<T>[]> subscribers;
        /** The default empty array of subscribers. */
        @SuppressWarnings("rawtypes")
        static final ReplaySubscription[] EMPTY = new ReplaySubscription[0];
        /** The default empty array of subscribers. */
        @SuppressWarnings("rawtypes")
        static final ReplaySubscription[] TERMINATED = new ReplaySubscription[0];

        /** Set to true after connection. */
        volatile boolean isConnected;
        /**
         * Indicates that the source has completed emitting values or the
         * Observable was forcefully terminated.
         */
        boolean sourceDone;

        @SuppressWarnings("unchecked")
        CacheState(Flowable<T> source, int capacityHint) {
            super(capacityHint);
            this.source = source;
            this.subscribers = new AtomicReference<ReplaySubscription<T>[]>(EMPTY);
        }
        /**
         * Adds a ReplaySubscription to the subscribers array atomically.
         * @param p the target ReplaySubscription wrapping a downstream Subscriber with state
         */
        public void addChild(ReplaySubscription<T> p) {
            // guarding by connection to save on allocating another object
            // thus there are two distinct locks guarding the value-addition and child come-and-go
            for (;;) {
                ReplaySubscription<T>[] a = subscribers.get();
                if (a == TERMINATED) {
                    return;
                }
                int n = a.length;
                @SuppressWarnings("unchecked")
                ReplaySubscription<T>[] b = new ReplaySubscription[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = p;
                if (subscribers.compareAndSet(a, b)) {
                    return;
                }
            }
        }
        /**
         * Removes the ReplaySubscription (if present) from the subscribers array atomically.
         * @param p the target ReplaySubscription wrapping a downstream Subscriber with state
         */
        @SuppressWarnings("unchecked")
        public void removeChild(ReplaySubscription<T> p) {
            for (;;) {
                ReplaySubscription<T>[] a = subscribers.get();
                int n = a.length;
                if (n == 0) {
                    return;
                }
                int j = -1;
                for (int i = 0; i < n; i++) {
                    if (a[i].equals(p)) {
                        j = i;
                        break;
                    }
                }
                if (j < 0) {
                    return;
                }

                ReplaySubscription<T>[] b;
                if (n == 1) {
                    b = EMPTY;
                } else {
                    b = new ReplaySubscription[n - 1];
                    System.arraycopy(a, 0, b, 0, j);
                    System.arraycopy(a, j + 1, b, j, n - j - 1);
                }
                if (subscribers.compareAndSet(a, b)) {
                    return;
                }
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(connection, s)) {
                s.request(Long.MAX_VALUE);
            }
        }

        /**
         * Connects the cache to the source.
         * Make sure this is called only once.
         */
        public void connect() {
            source.subscribe(this);
            isConnected = true;
        }
        @Override
        public void onNext(T t) {
            if (!sourceDone) {
                Object o = NotificationLite.next(t);
                add(o);
                for (ReplaySubscription<?> rp : subscribers.get()) {
                    rp.replay();
                }
            }
        }
        @SuppressWarnings("unchecked")
        @Override
        public void onError(Throwable e) {
            if (!sourceDone) {
                sourceDone = true;
                Object o = NotificationLite.error(e);
                add(o);
                SubscriptionHelper.cancel(connection);
                for (ReplaySubscription<?> rp : subscribers.getAndSet(TERMINATED)) {
                    rp.replay();
                }
            } else {
                RxJavaPlugins.onError(e);
            }
        }
        @SuppressWarnings("unchecked")
        @Override
        public void onComplete() {
            if (!sourceDone) {
                sourceDone = true;
                Object o = NotificationLite.complete();
                add(o);
                SubscriptionHelper.cancel(connection);
                for (ReplaySubscription<?> rp : subscribers.getAndSet(TERMINATED)) {
                    rp.replay();
                }
            }
        }
    }

    /**
     * Keeps track of the current request amount and the replay position for a child Subscriber.
     *
     * @param <T>
     */
    static final class ReplaySubscription<T>
    extends AtomicInteger implements Subscription {

        private static final long serialVersionUID = -2557562030197141021L;
        private static final long CANCELLED = -1;
        /** The actual child subscriber. */
        final Subscriber<? super T> child;
        /** The cache state object. */
        final CacheState<T> state;

        final AtomicLong requested;

        /**
         * Contains the reference to the buffer segment in replay.
         * Accessed after reading state.size() and when emitting == true.
         */
        Object[] currentBuffer;
        /**
         * Contains the index into the currentBuffer where the next value is expected.
         * Accessed after reading state.size() and when emitting == true.
         */
        int currentIndexInBuffer;
        /**
         * Contains the absolute index up until the values have been replayed so far.
         */
        int index;

        ReplaySubscription(Subscriber<? super T> child, CacheState<T> state) {
            this.child = child;
            this.state = state;
            this.requested = new AtomicLong();
        }
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                for (;;) {
                    long r = requested.get();
                    if (r == CANCELLED) {
                        return;
                    }
                    long u = BackpressureHelper.addCap(r, n);
                    if (requested.compareAndSet(r, u)) {
                        replay();
                        return;
                    }
                }
            }
        }

        @Override
        public void cancel() {
            if (requested.getAndSet(CANCELLED) != CANCELLED) {
                state.removeChild(this);
            }
        }

        /**
         * Continue replaying available values if there are requests for them.
         */
        public void replay() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            final Subscriber<? super T> child = this.child;
            AtomicLong rq = requested;

            for (;;) {

                long r = rq.get();

                if (r < 0L) {
                    return;
                }

                // read the size, if it is non-zero, we can safely read the head and
                // read values up to the given absolute index
                int s = state.size();
                if (s != 0) {
                    Object[] b = currentBuffer;

                    // latch onto the very first buffer now that it is available.
                    if (b == null) {
                        b = state.head();
                        currentBuffer = b;
                    }
                    final int n = b.length - 1;
                    int j = index;
                    int k = currentIndexInBuffer;
                    int valuesProduced = 0;

                    while (j < s && r > 0) {
                        if (rq.get() == CANCELLED) {
                            return;
                        }
                        if (k == n) {
                            b = (Object[])b[n];
                            k = 0;
                        }
                        Object o = b[k];

                        if (NotificationLite.accept(o, child)) {
                            return;
                        }

                        k++;
                        j++;
                        r--;
                        valuesProduced++;
                    }

                    if (rq.get() == CANCELLED) {
                        return;
                    }

                    if (r == 0) {
                        Object o = b[k];
                        if (NotificationLite.isComplete(o)) {
                            child.onComplete();
                            return;
                        } else
                        if (NotificationLite.isError(o)) {
                            child.onError(NotificationLite.getError(o));
                            return;
                        }
                    }

                    if (valuesProduced != 0) {
                        BackpressureHelper.producedCancel(rq, valuesProduced);
                    }

                    index = j;
                    currentIndexInBuffer = k;
                    currentBuffer = b;
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }
}
