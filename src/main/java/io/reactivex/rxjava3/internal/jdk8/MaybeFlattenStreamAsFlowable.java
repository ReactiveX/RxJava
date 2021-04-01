/*
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

package io.reactivex.rxjava3.internal.jdk8;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.annotations.*;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.subscriptions.*;
import io.reactivex.rxjava3.internal.util.BackpressureHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * Map the success value into a Java {@link Stream} and emits its values.
 *
 * @param <T> the source value type
 * @param <R> the output value type
 * @since 3.0.0
 */
public final class MaybeFlattenStreamAsFlowable<T, R> extends Flowable<R> {

    final Maybe<T> source;

    final Function<? super T, ? extends Stream<? extends R>> mapper;

    public MaybeFlattenStreamAsFlowable(Maybe<T> source, Function<? super T, ? extends Stream<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(@NonNull Subscriber<? super R> s) {
        source.subscribe(new FlattenStreamMultiObserver<>(s, mapper));
    }

    static final class FlattenStreamMultiObserver<T, R>
    extends BasicIntQueueSubscription<R>
    implements MaybeObserver<T>, SingleObserver<T> {

        private static final long serialVersionUID = 7363336003027148283L;

        final Subscriber<? super R> downstream;

        final Function<? super T, ? extends Stream<? extends R>> mapper;

        final AtomicLong requested;

        Disposable upstream;

        volatile Iterator<? extends R> iterator;

        AutoCloseable close;

        boolean once;

        volatile boolean cancelled;

        boolean outputFused;

        long emitted;

        FlattenStreamMultiObserver(Subscriber<? super R> downstream, Function<? super T, ? extends Stream<? extends R>> mapper) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(@NonNull Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(@NonNull T t) {
            try {
                Stream<? extends R> stream = Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Stream");
                Iterator<? extends R> iterator = stream.iterator();
                AutoCloseable c = stream;

                if (!iterator.hasNext()) {
                    downstream.onComplete();
                    close(c);
                    return;
                }
                this.iterator = iterator;
                this.close = stream;
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }
            drain();
        }

        @Override
        public void onError(@NonNull Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
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
            cancelled = true;
            upstream.dispose();
            if (!outputFused) {
                drain();
            }
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & ASYNC) != 0) {
                outputFused = true;
                return ASYNC;
            }
            return NONE;
        }

        @Override
        public @Nullable R poll() throws Throwable {
            Iterator<? extends R> it = iterator;
            if (it != null) {
                if (once) {
                    if (!it.hasNext()) {
                        clear();
                        return null;
                    }
                } else {
                    once = true;
                }
                return it.next();
            }
            return null;
        }

        @Override
        public boolean isEmpty() {
            Iterator<? extends R> it = iterator;
            if (it != null) {
                if (!once) {
                    return false;
                }
                if (it.hasNext()) {
                    return false;
                }
                clear();
            }
            return true;
        }

        @Override
        public void clear() {
            iterator = null;
            AutoCloseable close = this.close;
            this.close = null;
            close(close);
        }

        void close(AutoCloseable c) {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPlugins.onError(ex);
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            Subscriber<? super R> downstream = this.downstream;
            long emitted = this.emitted;
            long requested = this.requested.get();
            Iterator<? extends R> it = iterator;

            for (;;) {

                if (cancelled) {
                    clear();
                } else {
                    if (outputFused) {
                        if (it != null) {
                            downstream.onNext(null);
                            downstream.onComplete();
                        }
                    } else {
                        if (it != null && emitted != requested) {
                            R item;
                            try {
                                item = it.next();
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);
                                downstream.onError(ex);
                                cancelled = true;
                                continue;
                            }

                            if (cancelled) {
                                continue;
                            }

                            downstream.onNext(item);
                            emitted++;

                            if (cancelled) {
                                continue;
                            }

                            boolean has;
                            try {
                                has = it.hasNext();
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);
                                downstream.onError(ex);
                                cancelled = true;
                                continue;
                            }

                            if (cancelled) {
                                continue;
                            }

                            if (!has) {
                                downstream.onComplete();
                                cancelled = true;
                            }
                            continue;
                        }
                    }
                }

                this.emitted = emitted;
                missed = addAndGet(-missed);
                if (missed == 0) {
                    return;
                }

                requested = this.requested.get();
                if (it == null) {
                    it = iterator;
                }
            }
        }
    }
}
