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

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.internal.fuseable.ConditionalSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.BackpressureHelper;

import java.util.Objects;

public final class FlowableFromArray<T> extends Flowable<T> {
    final T[] array;

    public FlowableFromArray(T[] array) {
        this.array = array;
    }

    @Override
    public void subscribeActual(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            s.onSubscribe(new ArrayConditionalSubscription<T>(
                    (ConditionalSubscriber<? super T>)s, array));
        } else {
            s.onSubscribe(new ArraySubscription<T>(s, array));
        }
    }

    abstract static class BaseArraySubscription<T> extends BasicQueueSubscription<T> {
        private static final long serialVersionUID = -2252972430506210021L;

        final T[] array;

        int index;

        volatile boolean cancelled;

        BaseArraySubscription(T[] array) {
            this.array = array;
        }

        @Override
        public final int requestFusion(int mode) {
            return mode & SYNC;
        }

        @Nullable
        @Override
        public final T poll() {
            int i = index;
            T[] arr = array;
            if (i == arr.length) {
                return null;
            }

            index = i + 1;
            return Objects.requireNonNull(arr[i], "array element is null");
        }

        @Override
        public final boolean isEmpty() {
            return index == array.length;
        }

        @Override
        public final void clear() {
            index = array.length;
        }

        @Override
        public final void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                if (BackpressureHelper.add(this, n) == 0L) {
                    if (n == Long.MAX_VALUE) {
                        fastPath();
                    } else {
                        slowPath(n);
                    }
                }
            }
        }

        @Override
        public final void cancel() {
            cancelled = true;
        }

        abstract void fastPath();

        abstract void slowPath(long r);
    }

    static final class ArraySubscription<T> extends BaseArraySubscription<T> {

        private static final long serialVersionUID = 2587302975077663557L;

        final Subscriber<? super T> downstream;

        ArraySubscription(Subscriber<? super T> actual, T[] array) {
            super(array);
            this.downstream = actual;
        }

        @Override
        void fastPath() {
            T[] arr = array;
            int f = arr.length;
            Subscriber<? super T> a = downstream;

            for (int i = index; i != f; i++) {
                if (cancelled) {
                    return;
                }
                T t = arr[i];
                if (t == null) {
                    a.onError(new NullPointerException("The element at index " + i + " is null"));
                    return;
                } else {
                    a.onNext(t);
                }
            }
            if (cancelled) {
                return;
            }
            a.onComplete();
        }

        @Override
        void slowPath(long r) {
            long e = 0;
            T[] arr = array;
            int f = arr.length;
            int i = index;
            Subscriber<? super T> a = downstream;

            for (;;) {

                while (e != r && i != f) {
                    if (cancelled) {
                        return;
                    }

                    T t = arr[i];

                    if (t == null) {
                        a.onError(new NullPointerException("The element at index " + i + " is null"));
                        return;
                    } else {
                        a.onNext(t);
                    }

                    e++;
                    i++;
                }

                if (i == f) {
                    if (!cancelled) {
                        a.onComplete();
                    }
                    return;
                }

                r = get();
                if (e == r) {
                    index = i;
                    r = addAndGet(-e);
                    if (r == 0L) {
                        return;
                    }
                    e = 0L;
                }
            }
        }
    }

    static final class ArrayConditionalSubscription<T> extends BaseArraySubscription<T> {

        private static final long serialVersionUID = 2587302975077663557L;

        final ConditionalSubscriber<? super T> downstream;

        ArrayConditionalSubscription(ConditionalSubscriber<? super T> actual, T[] array) {
            super(array);
            this.downstream = actual;
        }

        @Override
        void fastPath() {
            T[] arr = array;
            int f = arr.length;
            ConditionalSubscriber<? super T> a = downstream;

            for (int i = index; i != f; i++) {
                if (cancelled) {
                    return;
                }
                T t = arr[i];
                if (t == null) {
                    a.onError(new NullPointerException("The element at index " + i + " is null"));
                    return;
                } else {
                    a.tryOnNext(t);
                }
            }
            if (cancelled) {
                return;
            }
            a.onComplete();
        }

        @Override
        void slowPath(long r) {
            long e = 0;
            T[] arr = array;
            int f = arr.length;
            int i = index;
            ConditionalSubscriber<? super T> a = downstream;

            for (;;) {

                while (e != r && i != f) {
                    if (cancelled) {
                        return;
                    }

                    T t = arr[i];

                    if (t == null) {
                        a.onError(new NullPointerException("The element at index " + i + " is null"));
                        return;
                    } else {
                        if (a.tryOnNext(t)) {
                            e++;
                        }

                        i++;
                    }
                }

                if (i == f) {
                    if (!cancelled) {
                        a.onComplete();
                    }
                    return;
                }

                r = get();
                if (e == r) {
                    index = i;
                    r = addAndGet(-e);
                    if (r == 0L) {
                        return;
                    }
                    e = 0L;
                }
            }
        }
    }
}
