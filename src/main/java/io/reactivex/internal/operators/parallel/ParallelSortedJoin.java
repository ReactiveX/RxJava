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

package io.reactivex.internal.operators.parallel;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.parallel.ParallelFlowable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Given sorted rail sequences (according to the provided comparator) as List
 * emit the smallest item from these parallel Lists to the Subscriber.
 * <p>
 * It expects the source to emit exactly one list (which could be empty).
 *
 * @param <T> the value type
 */
public final class ParallelSortedJoin<T> extends Flowable<T> {

    final ParallelFlowable<List<T>> source;

    final Comparator<? super T> comparator;

    public ParallelSortedJoin(ParallelFlowable<List<T>> source, Comparator<? super T> comparator) {
        this.source = source;
        this.comparator = comparator;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        SortedJoinSubscription<T> parent = new SortedJoinSubscription<T>(s, source.parallelism(), comparator);
        s.onSubscribe(parent);

        source.subscribe(parent.subscribers);
    }

    static final class SortedJoinSubscription<T>
    extends AtomicInteger
    implements Subscription {

        private static final long serialVersionUID = 3481980673745556697L;

        final Subscriber<? super T> actual;

        final SortedJoinInnerSubscriber<T>[] subscribers;

        final List<T>[] lists;

        final int[] indexes;

        final Comparator<? super T> comparator;

        final AtomicLong requested = new AtomicLong();

        volatile boolean cancelled;

        final AtomicInteger remaining = new AtomicInteger();

        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

        @SuppressWarnings("unchecked")
        SortedJoinSubscription(Subscriber<? super T> actual, int n, Comparator<? super T> comparator) {
            this.actual = actual;
            this.comparator = comparator;

            SortedJoinInnerSubscriber<T>[] s = new SortedJoinInnerSubscriber[n];

            for (int i = 0; i < n; i++) {
                s[i] = new SortedJoinInnerSubscriber<T>(this, i);
            }
            this.subscribers = s;
            this.lists = new List[n];
            this.indexes = new int[n];
            remaining.lazySet(n);
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                if (remaining.get() == 0) {
                    drain();
                }
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                cancelAll();
                if (getAndIncrement() == 0) {
                    Arrays.fill(lists, null);
                }
            }
        }

        void cancelAll() {
            for (SortedJoinInnerSubscriber<T> s : subscribers) {
                s.cancel();
            }
        }

        void innerNext(List<T> value, int index) {
            lists[index] = value;
            if (remaining.decrementAndGet() == 0) {
                drain();
            }
        }

        void innerError(Throwable e) {
            if (error.compareAndSet(null, e)) {
                drain();
            } else {
                if (e != error.get()) {
                    RxJavaPlugins.onError(e);
                }
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            Subscriber<? super T> a = actual;
            List<T>[] lists = this.lists;
            int[] indexes = this.indexes;
            int n = indexes.length;

            for (;;) {

                long r = requested.get();
                long e = 0L;

                while (e != r) {
                    if (cancelled) {
                        Arrays.fill(lists, null);
                        return;
                    }

                    Throwable ex = error.get();
                    if (ex != null) {
                        cancelAll();
                        Arrays.fill(lists, null);
                        a.onError(ex);
                        return;
                    }

                    T min = null;
                    int minIndex = -1;

                    for (int i = 0; i < n; i++) {
                        List<T> list = lists[i];
                        int index = indexes[i];

                        if (list.size() != index) {
                            if (min == null) {
                                min = list.get(index);
                                minIndex = i;
                            } else {
                                T b = list.get(index);

                                boolean smaller;

                                try {
                                    smaller = comparator.compare(min, b) > 0;
                                } catch (Throwable exc) {
                                    Exceptions.throwIfFatal(exc);
                                    cancelAll();
                                    Arrays.fill(lists, null);
                                    if (!error.compareAndSet(null, exc)) {
                                        RxJavaPlugins.onError(exc);
                                    }
                                    a.onError(error.get());
                                    return;
                                }
                                if (smaller) {
                                    min = b;
                                    minIndex = i;
                                }
                            }
                        }
                    }

                    if (min == null) {
                        Arrays.fill(lists, null);
                        a.onComplete();
                        return;
                    }

                    a.onNext(min);

                    indexes[minIndex]++;

                    e++;
                }

                if (e == r) {
                    if (cancelled) {
                        Arrays.fill(lists, null);
                        return;
                    }

                    Throwable ex = error.get();
                    if (ex != null) {
                        cancelAll();
                        Arrays.fill(lists, null);
                        a.onError(ex);
                        return;
                    }

                    boolean empty = true;

                    for (int i = 0; i < n; i++) {
                        if (indexes[i] != lists[i].size()) {
                            empty = false;
                            break;
                        }
                    }

                    if (empty) {
                        Arrays.fill(lists, null);
                        a.onComplete();
                        return;
                    }
                }

                if (e != 0 && r != Long.MAX_VALUE) {
                    requested.addAndGet(-e);
                }

                int w = get();
                if (w == missed) {
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                } else {
                    missed = w;
                }
            }
        }
    }

    static final class SortedJoinInnerSubscriber<T>
    extends AtomicReference<Subscription>
    implements FlowableSubscriber<List<T>> {


        private static final long serialVersionUID = 6751017204873808094L;

        final SortedJoinSubscription<T> parent;

        final int index;

        SortedJoinInnerSubscriber(SortedJoinSubscription<T> parent, int index) {
            this.parent = parent;
            this.index = index;
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.setOnce(this, s, Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<T> t) {
            parent.innerNext(t, index);
        }

        @Override
        public void onError(Throwable t) {
            parent.innerError(t);
        }

        @Override
        public void onComplete() {
            // ignored
        }

        void cancel() {
            SubscriptionHelper.cancel(this);
        }
    }
}
