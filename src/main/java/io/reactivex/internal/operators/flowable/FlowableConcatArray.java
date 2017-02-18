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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.internal.subscriptions.SubscriptionArbiter;

public final class FlowableConcatArray<T> extends Flowable<T> {

    final Publisher<? extends T>[] sources;

    final boolean delayError;

    public FlowableConcatArray(Publisher<? extends T>[] sources, boolean delayError) {
        this.sources = sources;
        this.delayError = delayError;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        ConcatArraySubscriber<T> parent = new ConcatArraySubscriber<T>(sources, delayError, s);
        s.onSubscribe(parent);

        parent.onComplete();
    }

    static final class ConcatArraySubscriber<T> extends SubscriptionArbiter implements FlowableSubscriber<T> {

        private static final long serialVersionUID = -8158322871608889516L;

        final Subscriber<? super T> actual;

        final Publisher<? extends T>[] sources;

        final boolean delayError;

        final AtomicInteger wip;

        int index;

        List<Throwable> errors;

        long produced;

        ConcatArraySubscriber(Publisher<? extends T>[] sources, boolean delayError, Subscriber<? super T> actual) {
            this.actual = actual;
            this.sources = sources;
            this.delayError = delayError;
            this.wip = new AtomicInteger();
        }

        @Override
        public void onSubscribe(Subscription s) {
            setSubscription(s);
        }

        @Override
        public void onNext(T t) {
            produced++;
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (delayError) {
                List<Throwable> list = errors;
                if (list == null) {
                    list = new ArrayList<Throwable>(sources.length - index + 1);
                    errors = list;
                }
                list.add(t);
                onComplete();
            } else {
                actual.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (wip.getAndIncrement() == 0) {
                Publisher<? extends T>[] sources = this.sources;
                int n = sources.length;
                int i = index;
                for (;;) {

                    if (i == n) {
                        List<Throwable> list = errors;
                        if (list != null) {
                            if (list.size() == 1) {
                                actual.onError(list.get(0));
                            } else {
                                actual.onError(new CompositeException(list));
                            }
                        } else {
                            actual.onComplete();
                        }
                        return;
                    }

                    Publisher<? extends T> p = sources[i];

                    if (p == null) {
                        Throwable ex = new NullPointerException("A Publisher entry is null");
                        if (delayError) {
                            List<Throwable> list = errors;
                            if (list == null) {
                                list = new ArrayList<Throwable>(n - i + 1);
                                errors = list;
                            }
                            list.add(ex);
                            i++;
                            continue;
                        } else {
                            actual.onError(ex);
                            return;
                        }
                    } else {
                        long r = produced;
                        if (r != 0L) {
                            produced = 0L;
                            produced(r);
                        }
                        p.subscribe(this);
                    }

                    index = ++i;

                    if (wip.decrementAndGet() == 0) {
                        break;
                    }
                }
            }
        }
    }

}
