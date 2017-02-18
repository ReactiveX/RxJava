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
import io.reactivex.functions.Predicate;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.subscribers.*;

public final class FlowableFilter<T> extends AbstractFlowableWithUpstream<T, T> {
    final Predicate<? super T> predicate;
    public FlowableFilter(Flowable<T> source, Predicate<? super T> predicate) {
        super(source);
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            source.subscribe(new FilterConditionalSubscriber<T>(
                    (ConditionalSubscriber<? super T>)s, predicate));
        } else {
            source.subscribe(new FilterSubscriber<T>(s, predicate));
        }
    }

    static final class FilterSubscriber<T> extends BasicFuseableSubscriber<T, T>
    implements ConditionalSubscriber<T> {
        final Predicate<? super T> filter;

        FilterSubscriber(Subscriber<? super T> actual, Predicate<? super T> filter) {
            super(actual);
            this.filter = filter;
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                s.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }
            if (sourceMode != NONE) {
                actual.onNext(null);
                return true;
            }
            boolean b;
            try {
                b = filter.test(t);
            } catch (Throwable e) {
                fail(e);
                return true;
            }
            if (b) {
                actual.onNext(t);
            }
            return b;
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            QueueSubscription<T> qs = this.qs;
            Predicate<? super T> f = filter;

            for (;;) {
                T t = qs.poll();
                if (t == null) {
                    return null;
                }

                if (f.test(t)) {
                    return t;
                }

                if (sourceMode == ASYNC) {
                    qs.request(1);
                }
            }
        }


    }

    static final class FilterConditionalSubscriber<T> extends BasicFuseableConditionalSubscriber<T, T> {
        final Predicate<? super T> filter;

        FilterConditionalSubscriber(ConditionalSubscriber<? super T> actual, Predicate<? super T> filter) {
            super(actual);
            this.filter = filter;
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                s.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }

            if (sourceMode != NONE) {
                return actual.tryOnNext(null);
            }

            boolean b;
            try {
                b = filter.test(t);
            } catch (Throwable e) {
                fail(e);
                return true;
            }
            return b && actual.tryOnNext(t);
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            QueueSubscription<T> qs = this.qs;
            Predicate<? super T> f = filter;

            for (;;) {
                T t = qs.poll();
                if (t == null) {
                    return null;
                }

                if (f.test(t)) {
                    return t;
                }

                if (sourceMode == ASYNC) {
                    qs.request(1);
                }
            }
        }
    }
}
