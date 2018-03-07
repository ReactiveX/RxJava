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
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.annotations.Experimental;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Generates items by invoking a callback, for each downstream request one by one, that sets up an
 * asynchronous call to some API that eventually responds with an item, an error or termination, while
 * making sure there is only one such outstanding API call in progress and honoring the
 * backpressure of the downstream.
 *
 * @param <T> the generated item type
 * @param <S> the state associated with an individual subscription.
 * @since 2.1.11 - experimental
 */
@Experimental
public final class FlowableGenerateAsync<T, S> extends Flowable<T> {

    final Callable<S> initialState;

    final BiFunction<? super S, ? super FlowableAsyncEmitter<T>, ? extends S> asyncGenerator;

    final Consumer<? super S> stateCleanup;

    public FlowableGenerateAsync(Callable<S> initialState,
            BiFunction<? super S, ? super FlowableAsyncEmitter<T>, ? extends S> asyncGenerator,
            Consumer<? super S> stateCleanup) {
        this.initialState = initialState;
        this.asyncGenerator = asyncGenerator;
        this.stateCleanup = stateCleanup;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        S state;

        try {
            state = initialState.call();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        GenerateAsyncSubscription<T, S> parent = new GenerateAsyncSubscription<T, S>(s, state, asyncGenerator, stateCleanup);
        s.onSubscribe(parent);
        parent.moveNext();
    }

    static final class GenerateAsyncSubscription<T, S>
    extends AtomicInteger
    implements Subscription, FlowableAsyncEmitter<T> {

        private static final long serialVersionUID = -2460374219999425947L;

        final Subscriber<? super T> downstream;

        final AtomicInteger wip;

        final AtomicLong requested;

        final AtomicCancellable resource;

        final BiFunction<? super S, ? super FlowableAsyncEmitter<T>, ? extends S> asyncGenerator;

        final Consumer<? super S> stateCleanup;

        final AtomicThrowable errors;

        volatile S state;

        T item;
        volatile int itemState;

        static final int ITEM_STATE_NOTHING_YET = 0;
        static final int ITEM_STATE_HAS_VALUE = 1;
        static final int ITEM_STATE_EMPTY = 2;
        static final int ITEM_STATE_DONE = 4;
        static final int ITEM_STATE_HAS_VALUE_DONE = ITEM_STATE_HAS_VALUE | ITEM_STATE_DONE;
        static final int ITEM_STATE_EMPTY_DONE = ITEM_STATE_HAS_VALUE | ITEM_STATE_DONE;

        volatile boolean done;

        volatile boolean cancelled;

        long emitted;

        GenerateAsyncSubscription(Subscriber<? super T> downstream,
                S state,
                BiFunction<? super S, ? super FlowableAsyncEmitter<T>, ? extends S> asyncGenerator,
                Consumer<? super S> stateCleanup) {
            this.downstream = downstream;
            this.state = state;
            this.asyncGenerator = asyncGenerator;
            this.stateCleanup = stateCleanup;
            this.wip = new AtomicInteger();
            this.requested = new AtomicLong();
            this.resource = new AtomicCancellable();
            this.errors = new AtomicThrowable();
        }

        @Override
        public void request(long n) {
            BackpressureHelper.add(requested, n);
            drain();
        }

        @Override
        public void cancel() {
            cancelled = true;
            resource.cancel();
            if (getAndIncrement() == 0) {
                cleanup();
            }
        }

        void cleanup() {
            try {
                stateCleanup.accept(state);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void onNext(T value) {
            if (value != null) {
                item = value;
                itemState = ITEM_STATE_HAS_VALUE;
                drain();
            } else {
                onError(new NullPointerException("value is null"));
            }
        }

        @Override
        public void onError(Throwable error) {
            if (error == null) {
                error = new NullPointerException("error is null");
            }
            if (errors.addThrowable(error)) {
                itemState |= ITEM_STATE_DONE;
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(error);
            }
        }

        @Override
        public void onComplete() {
            itemState |= ITEM_STATE_DONE;
            done = true;
            drain();
        }

        @Override
        public void onNothing() {
            item = null;
            itemState = ITEM_STATE_EMPTY;
            drain();
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean replaceCancellable(Cancellable c) {
            return resource.replaceCancellable(c);
        }

        @Override
        public boolean setCancellable(Cancellable c) {
            return resource.setCancellable(c);
        }

        void moveNext() {
            if (wip.getAndIncrement() == 0) {
                do {
                    if (cancelled) {
                        return;
                    }
                    try {
                        state = asyncGenerator.apply(state, this);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        onError(ex);
                        return;
                    }
                } while (wip.decrementAndGet() != 0);
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            Subscriber<? super T> downstream = this.downstream;
            long emitted = this.emitted;
            AtomicLong requested = this.requested;

            for (;;) {

                for (;;) {
                    if (cancelled) {
                        cleanup();
                        return;
                    }

                    boolean d = done;
                    int s = itemState;

                    if (d && s == ITEM_STATE_DONE) {
                        Throwable ex = errors.terminate();
                        if (ex != null) {
                            downstream.onError(ex);
                        } else {
                            downstream.onComplete();
                        }
                        resource.cancel();
                        cleanup();
                        return;
                    }

                    if ((s & ~ITEM_STATE_DONE) == ITEM_STATE_HAS_VALUE) {
                        if (emitted != requested.get()) {
                            T v = item;
                            item = null;

                            downstream.onNext(v);

                            emitted++;

                            if ((s & ITEM_STATE_DONE) != 0) {
                                itemState = ITEM_STATE_DONE;
                            } else {
                                itemState = ITEM_STATE_NOTHING_YET;
                                moveNext();
                            }
                        } else {
                            break;
                        }
                    } else if ((s & ~ITEM_STATE_DONE) == ITEM_STATE_EMPTY) {
                        if ((s & ITEM_STATE_DONE) != 0) {
                            itemState = ITEM_STATE_DONE;
                        } else {
                            itemState = ITEM_STATE_NOTHING_YET;
                            moveNext();
                        }
                    } else {
                        break;
                    }
                }

                this.emitted = emitted;
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }

    static final class AtomicCancellable extends AtomicReference<Cancellable> {

        private static final long serialVersionUID = -8193511349691432602L;

        public boolean replaceCancellable(Cancellable c) {
            for (;;) {
                Cancellable curr = get();
                if (curr == CANCELLED) {
                    cancel(c);
                    return false;
                }
                if (compareAndSet(curr, c)) {
                    return true;
                }
            }
        }

        public boolean setCancellable(Cancellable c) {
            for (;;) {
                Cancellable curr = get();
                if (curr == CANCELLED) {
                    cancel(c);
                    return false;
                }
                if (compareAndSet(curr, c)) {
                    cancel(curr);
                    return true;
                }
            }
        }

        void cancel() {
            Cancellable c = getAndSet(CANCELLED);
            cancel(c);
        }

        void cancel(Cancellable c) {
            if (c != null) {
                try {
                    c.cancel();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaPlugins.onError(ex);
                }
            }
        }

        static final Cancellable CANCELLED = new Cancellable() {
            @Override
            public void cancel() throws Exception {
            }
        };
    }
}
