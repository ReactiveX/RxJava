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

import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.annotations.Nullable;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;

/**
 * Emits a range of long values.
 */
public final class FlowableRangeLong extends Flowable<Long> {
    final long start;
    final long end;

    public FlowableRangeLong(long start, long count) {
        this.start = start;
        this.end = start + count;
    }

    @Override
    public void subscribeActual(Subscriber<? super Long> s) {
        if (s instanceof ConditionalSubscriber) {
            s.onSubscribe(new RangeConditionalSubscription(
                    (ConditionalSubscriber<? super Long>)s, start, end));
        } else {
            s.onSubscribe(new RangeSubscription(s, start, end));
        }
    }

    abstract static class BaseRangeSubscription extends BasicQueueSubscription<Long> {

        private static final long serialVersionUID = -2252972430506210021L;

        final long end;

        long index;

        volatile boolean cancelled;

        BaseRangeSubscription(long index, long end) {
            this.index = index;
            this.end = end;
        }

        @Override
        public final int requestFusion(int mode) {
            return mode & SYNC;
        }

        @Nullable
        @Override
        public final Long poll() {
            long i = index;
            if (i == end) {
                return null;
            }
            index = i + 1;
            return i;
        }

        @Override
        public final boolean isEmpty() {
            return index == end;
        }

        @Override
        public final void clear() {
            index = end;
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

    static final class RangeSubscription extends BaseRangeSubscription {

        private static final long serialVersionUID = 2587302975077663557L;

        final Subscriber<? super Long> actual;

        RangeSubscription(Subscriber<? super Long> actual, long index, long end) {
            super(index, end);
            this.actual = actual;
        }

        @Override
        void fastPath() {
            long f = end;
            Subscriber<? super Long> a = actual;

            for (long i = index; i != f; i++) {
                if (cancelled) {
                    return;
                }
                a.onNext(i);
            }
            if (cancelled) {
                return;
            }
            a.onComplete();
        }

        @Override
        void slowPath(long r) {
            long e = 0;
            long f = end;
            long i = index;
            Subscriber<? super Long> a = actual;

            for (;;) {

                while (e != r && i != f) {
                    if (cancelled) {
                        return;
                    }

                    a.onNext(i);

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

    static final class RangeConditionalSubscription extends BaseRangeSubscription {


        private static final long serialVersionUID = 2587302975077663557L;

        final ConditionalSubscriber<? super Long> actual;

        RangeConditionalSubscription(ConditionalSubscriber<? super Long> actual, long index, long end) {
            super(index, end);
            this.actual = actual;
        }

        @Override
        void fastPath() {
            long f = end;
            ConditionalSubscriber<? super Long> a = actual;

            for (long i = index; i != f; i++) {
                if (cancelled) {
                    return;
                }
                a.tryOnNext(i);
            }
            if (cancelled) {
                return;
            }
            a.onComplete();
        }

        @Override
        void slowPath(long r) {
            long e = 0;
            long f = end;
            long i = index;
            ConditionalSubscriber<? super Long> a = actual;

            for (;;) {

                while (e != r && i != f) {
                    if (cancelled) {
                        return;
                    }

                    if (a.tryOnNext(i)) {
                        e++;
                    }

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
                    if (r == 0) {
                        return;
                    }
                    e = 0;
                }
            }
        }
    }
}
