/**
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.internal.operators;

import java.util.Queue;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.exceptions.Exceptions;
import rx.functions.Func1;
import rx.internal.util.ExceptionsUtils;
import rx.internal.util.atomic.MpscLinkedAtomicQueue;
import rx.internal.util.unsafe.*;
import rx.plugins.RxJavaHooks;
import rx.subscriptions.CompositeSubscription;

/**
 * Maps upstream values to Singles and merges them, up to a given
 * number of them concurrently, optionally delaying errors.
 * @param <T> the upstream value type
 * @param <R> the inner Singles and result value type
 * @since 1.2.7 - experimental
 */
public final class OnSubscribeFlatMapSingle<T, R> implements Observable.OnSubscribe<R> {

    final Observable<T> source;

    final Func1<? super T, ? extends Single<? extends R>> mapper;

    final boolean delayErrors;

    final int maxConcurrency;

    public OnSubscribeFlatMapSingle(Observable<T> source, Func1<? super T, ? extends Single<? extends R>> mapper,
            boolean delayErrors, int maxConcurrency) {
        if (mapper == null) {
            throw new NullPointerException("mapper is null");
        }
        if (maxConcurrency <= 0) {
            throw new IllegalArgumentException("maxConcurrency > 0 required but it was " + maxConcurrency);
        }
        this.source = source;
        this.mapper = mapper;
        this.delayErrors = delayErrors;
        this.maxConcurrency = maxConcurrency;
    }

    @Override
    public void call(Subscriber<? super R> child) {
        FlatMapSingleSubscriber<T, R> parent = new FlatMapSingleSubscriber<T, R>(child, mapper, delayErrors, maxConcurrency);
        child.add(parent.set);
        child.add(parent.requested);
        child.setProducer(parent.requested);
        source.unsafeSubscribe(parent);
    }

    static final class FlatMapSingleSubscriber<T, R> extends Subscriber<T> {

        final Subscriber<? super R> actual;

        final Func1<? super T, ? extends Single<? extends R>> mapper;

        final boolean delayErrors;

        final int maxConcurrency;

        final AtomicInteger wip;

        final AtomicInteger active;

        final CompositeSubscription set;

        final AtomicReference<Throwable> errors;

        final Queue<Object> queue;

        final Requested requested;

        volatile boolean done;

        volatile boolean cancelled;

        FlatMapSingleSubscriber(Subscriber<? super R> actual,
                Func1<? super T, ? extends Single<? extends R>> mapper,
                boolean delayErrors, int maxConcurrency) {
            this.actual = actual;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.maxConcurrency = maxConcurrency;
            this.wip = new AtomicInteger();
            this.errors = new AtomicReference<Throwable>();
            this.requested = new Requested();
            this.set = new CompositeSubscription();
            this.active = new AtomicInteger();
            if (UnsafeAccess.isUnsafeAvailable()) {
                queue = new MpscLinkedQueue<Object>();
            } else {
                queue = new MpscLinkedAtomicQueue<Object>();
            }
            this.request(maxConcurrency != Integer.MAX_VALUE ? maxConcurrency : Long.MAX_VALUE);
        }

        @Override
        public void onNext(T t) {
            Single<? extends R> c;

            try {
                c = mapper.call(t);
                if (c == null) {
                    throw new NullPointerException("The mapper returned a null Single");
                }
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                unsubscribe();
                onError(ex);
                return;
            }

            InnerSubscriber inner = new InnerSubscriber();
            set.add(inner);
            active.incrementAndGet();

            c.subscribe(inner);
        }

        @Override
        public void onError(Throwable e) {
            if (delayErrors) {
                ExceptionsUtils.addThrowable(errors, e);
            } else {
                set.unsubscribe();
                if (!errors.compareAndSet(null, e)) {
                    RxJavaHooks.onError(e);
                    return;
                }
            }
            done = true;
            drain();
        }

        @Override
        public void onCompleted() {
            done = true;
            drain();
        }

        void innerSuccess(InnerSubscriber inner, R value) {
            queue.offer(NotificationLite.next(value));
            set.remove(inner);
            active.decrementAndGet();
            drain();
        }

        void innerError(InnerSubscriber inner, Throwable e) {
            if (delayErrors) {
                ExceptionsUtils.addThrowable(errors, e);
                set.remove(inner);
                if (!done && maxConcurrency != Integer.MAX_VALUE) {
                    request(1);
                }
            } else {
                set.unsubscribe();
                unsubscribe();
                if (!errors.compareAndSet(null, e)) {
                    RxJavaHooks.onError(e);
                    return;
                }
                done = true;
            }
            active.decrementAndGet();
            drain();
        }

        void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            Subscriber<? super R> a = actual;
            Queue<Object> q = queue;
            boolean delayError = this.delayErrors;
            AtomicInteger act = active;

            for (;;) {
                long r = requested.get();
                long e = 0L;

                while (e != r) {
                    if (cancelled) {
                        q.clear();
                        return;
                    }

                    boolean d = done;

                    if (!delayError && d) {
                        Throwable ex = errors.get();
                        if (ex != null) {
                            q.clear();
                            a.onError(ExceptionsUtils.terminate(errors));
                            return;
                        }
                    }

                    Object o = q.poll();

                    boolean empty = o == null;

                    if (d && act.get() == 0 && empty) {
                        Throwable ex = errors.get();
                        if (ex != null) {
                            a.onError(ExceptionsUtils.terminate(errors));
                        } else {
                            a.onCompleted();
                        }
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    a.onNext(NotificationLite.<R>getValue(o));

                    e++;
                }

                if (e == r) {
                    if (cancelled) {
                        q.clear();
                        return;
                    }

                    if (done) {
                        if (delayError) {
                            if (act.get() == 0 && q.isEmpty()) {
                                Throwable ex = errors.get();
                                if (ex != null) {
                                    a.onError(ExceptionsUtils.terminate(errors));
                                } else {
                                    a.onCompleted();
                                }
                                return;
                            }
                        } else {
                            Throwable ex = errors.get();
                            if (ex != null) {
                                q.clear();
                                a.onError(ExceptionsUtils.terminate(errors));
                                return;
                            }
                            else if (act.get() == 0 && q.isEmpty()) {
                                a.onCompleted();
                                return;
                            }
                        }
                    }
                }

                if (e != 0L) {
                    requested.produced(e);
                    if (!done && maxConcurrency != Integer.MAX_VALUE) {
                        request(e);
                    }
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        final class Requested extends AtomicLong implements Producer, Subscription {

            private static final long serialVersionUID = -887187595446742742L;

            @Override
            public void request(long n) {
                if (n > 0L) {
                    BackpressureUtils.getAndAddRequest(this, n);
                    drain();
                }
            }

            void produced(long e) {
                BackpressureUtils.produced(this, e);
            }

            @Override
            public void unsubscribe() {
                cancelled = true;
                FlatMapSingleSubscriber.this.unsubscribe();
                if (wip.getAndIncrement() == 0) {
                    queue.clear();
                }
            }

            @Override
            public boolean isUnsubscribed() {
                return cancelled;
            }
        }

        final class InnerSubscriber extends SingleSubscriber<R> {

            @Override
            public void onSuccess(R t) {
                innerSuccess(this, t);
            }

            @Override
            public void onError(Throwable error) {
                innerError(this, error);
            }
        }
    }
}
