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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class FlowableGenerate<T, S> extends Flowable<T> {
    final Callable<S> stateSupplier;
    final BiFunction<S, Emitter<T>, S> generator;
    final Consumer<? super S> disposeState;

    public FlowableGenerate(Callable<S> stateSupplier, BiFunction<S, Emitter<T>, S> generator,
            Consumer<? super S> disposeState) {
        this.stateSupplier = stateSupplier;
        this.generator = generator;
        this.disposeState = disposeState;
    }

    @Override
    public void subscribeActual(Subscriber<? super T> s) {
        S state;

        try {
            state = stateSupplier.call();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptySubscription.error(e, s);
            return;
        }

        s.onSubscribe(new GeneratorSubscription<T, S>(s, generator, disposeState, state));
    }

    static final class GeneratorSubscription<T, S>
    extends AtomicLong
    implements Emitter<T>, Subscription {

        private static final long serialVersionUID = 7565982551505011832L;

        final Subscriber<? super T> actual;
        final BiFunction<S, ? super Emitter<T>, S> generator;
        final Consumer<? super S> disposeState;

        S state;

        volatile boolean cancelled;

        boolean terminate;

        boolean hasNext;

        GeneratorSubscription(Subscriber<? super T> actual,
                BiFunction<S, ? super Emitter<T>, S> generator,
                Consumer<? super S> disposeState, S initialState) {
            this.actual = actual;
            this.generator = generator;
            this.disposeState = disposeState;
            this.state = initialState;
        }

        @Override
        public void request(long n) {
            if (!SubscriptionHelper.validate(n)) {
                return;
            }
            if (BackpressureHelper.add(this, n) != 0L) {
                return;
            }

            long e = 0L;

            S s = state;

            final BiFunction<S, ? super Emitter<T>, S> f = generator;

            for (;;) {
                while (e != n) {

                    if (cancelled) {
                        state = null;
                        dispose(s);
                        return;
                    }

                    hasNext = false;

                    try {
                        s = f.apply(s, this);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        cancelled = true;
                        state = null;
                        onError(ex);
                        dispose(s);
                        return;
                    }

                    if (terminate) {
                        cancelled = true;
                        state = null;
                        dispose(s);
                        return;
                    }

                    e++;
                }

                n = get();
                if (e == n) {
                    state = s;
                    n = addAndGet(-e);
                    if (n == 0L) {
                        break;
                    }
                    e = 0L;
                }
            }
        }

        private void dispose(S s) {
            try {
                disposeState.accept(s);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;

                // if there are no running requests, just dispose the state
                if (BackpressureHelper.add(this, 1) == 0) {
                    S s = state;
                    state = null;
                    dispose(s);
                }
            }
        }

        @Override
        public void onNext(T t) {
            if (!terminate) {
                if (hasNext) {
                    onError(new IllegalStateException("onNext already called in this generate turn"));
                } else {
                    if (t == null) {
                        onError(new NullPointerException("onNext called with null. Null values are generally not allowed in 2.x operators and sources."));
                    } else {
                        hasNext = true;
                        actual.onNext(t);
                    }
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (terminate) {
                RxJavaPlugins.onError(t);
            } else {
                if (t == null) {
                    t = new NullPointerException("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
                }
                terminate = true;
                actual.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!terminate) {
                terminate = true;
                actual.onComplete();
            }
        }
    }
}
