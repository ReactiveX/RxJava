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

import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Publisher;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.BiPredicate;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.operators.flowable.FlowableSequenceEqual.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.AtomicThrowable;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableSequenceEqualSingle<T> extends Single<Boolean> implements FuseToFlowable<Boolean> {
    final Publisher<? extends T> first;
    final Publisher<? extends T> second;
    final BiPredicate<? super T, ? super T> comparer;
    final int prefetch;

    public FlowableSequenceEqualSingle(Publisher<? extends T> first, Publisher<? extends T> second,
            BiPredicate<? super T, ? super T> comparer, int prefetch) {
        this.first = first;
        this.second = second;
        this.comparer = comparer;
        this.prefetch = prefetch;
    }

    @Override
    public void subscribeActual(SingleObserver<? super Boolean> s) {
        EqualCoordinator<T> parent = new EqualCoordinator<T>(s, prefetch, comparer);
        s.onSubscribe(parent);
        parent.subscribe(first, second);
    }

    @Override
    public Flowable<Boolean> fuseToFlowable() {
        return RxJavaPlugins.onAssembly(new FlowableSequenceEqual<T>(first, second, comparer, prefetch));
    }

    static final class EqualCoordinator<T>
    extends AtomicInteger
    implements Disposable, EqualCoordinatorHelper {

        private static final long serialVersionUID = -6178010334400373240L;

        final SingleObserver<? super Boolean> actual;

        final BiPredicate<? super T, ? super T> comparer;

        final EqualSubscriber<T> first;

        final EqualSubscriber<T> second;

        final AtomicThrowable error;

        T v1;

        T v2;

        EqualCoordinator(SingleObserver<? super Boolean> actual, int prefetch, BiPredicate<? super T, ? super T> comparer) {
            this.actual = actual;
            this.comparer = comparer;
            this.first = new EqualSubscriber<T>(this, prefetch);
            this.second = new EqualSubscriber<T>(this, prefetch);
            this.error = new AtomicThrowable();
        }

        void subscribe(Publisher<? extends T> source1, Publisher<? extends T> source2) {
            source1.subscribe(first);
            source2.subscribe(second);
        }

        @Override
        public void dispose() {
            first.cancel();
            second.cancel();
            if (getAndIncrement() == 0) {
                first.clear();
                second.clear();
            }
        }

        @Override
        public boolean isDisposed() {
            return SubscriptionHelper.isCancelled(first.get());
        }

        void cancelAndClear() {
            first.cancel();
            first.clear();
            second.cancel();
            second.clear();
        }

        @Override
        public void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;

            for (;;) {
                SimpleQueue<T> q1 = first.queue;
                SimpleQueue<T> q2 = second.queue;

                if (q1 != null && q2 != null) {
                    for (;;) {
                        if (isDisposed()) {
                            first.clear();
                            second.clear();
                            return;
                        }

                        Throwable ex = error.get();
                        if (ex != null) {
                            cancelAndClear();

                            actual.onError(error.terminate());
                            return;
                        }

                        boolean d1 = first.done;

                        T a = v1;
                        if (a == null) {
                            try {
                                a = q1.poll();
                            } catch (Throwable exc) {
                                Exceptions.throwIfFatal(exc);
                                cancelAndClear();
                                error.addThrowable(exc);
                                actual.onError(error.terminate());
                                return;
                            }
                            v1 = a;
                        }
                        boolean e1 = a == null;

                        boolean d2 = second.done;
                        T b = v2;
                        if (b == null) {
                            try {
                                b = q2.poll();
                            } catch (Throwable exc) {
                                Exceptions.throwIfFatal(exc);
                                cancelAndClear();
                                error.addThrowable(exc);
                                actual.onError(error.terminate());
                                return;
                            }
                            v2 = b;
                        }

                        boolean e2 = b == null;

                        if (d1 && d2 && e1 && e2) {
                            actual.onSuccess(true);
                            return;
                        }
                        if ((d1 && d2) && (e1 != e2)) {
                            cancelAndClear();
                            actual.onSuccess(false);
                            return;
                        }

                        if (e1 || e2) {
                            break;
                        }

                        boolean c;

                        try {
                            c = comparer.test(a, b);
                        } catch (Throwable exc) {
                            Exceptions.throwIfFatal(exc);
                            cancelAndClear();
                            error.addThrowable(exc);
                            actual.onError(error.terminate());
                            return;
                        }

                        if (!c) {
                            cancelAndClear();
                            actual.onSuccess(false);
                            return;
                        }

                        v1 = null;
                        v2 = null;

                        first.request();
                        second.request();
                    }

                } else {
                    if (isDisposed()) {
                        first.clear();
                        second.clear();
                        return;
                    }

                    Throwable ex = error.get();
                    if (ex != null) {
                        cancelAndClear();

                        actual.onError(error.terminate());
                        return;
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public void innerError(Throwable t) {
            if (error.addThrowable(t)) {
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }
    }
}
