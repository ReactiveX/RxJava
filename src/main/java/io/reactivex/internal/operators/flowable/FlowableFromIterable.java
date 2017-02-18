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

import java.util.Iterator;

import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.annotations.Nullable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.BackpressureHelper;

public final class FlowableFromIterable<T> extends Flowable<T> {

    final Iterable<? extends T> source;

    public FlowableFromIterable(Iterable<? extends T> source) {
        this.source = source;
    }

    @Override
    public void subscribeActual(Subscriber<? super T> s) {
        Iterator<? extends T> it;
        try {
            it = source.iterator();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptySubscription.error(e, s);
            return;
        }

        subscribe(s, it);
    }

    public static <T> void subscribe(Subscriber<? super T> s, Iterator<? extends T> it) {
        boolean hasNext;
        try {
            hasNext = it.hasNext();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptySubscription.error(e, s);
            return;
        }

        if (!hasNext) {
            EmptySubscription.complete(s);
            return;
        }

        if (s instanceof ConditionalSubscriber) {
            s.onSubscribe(new IteratorConditionalSubscription<T>(
                    (ConditionalSubscriber<? super T>)s, it));
        } else {
            s.onSubscribe(new IteratorSubscription<T>(s, it));
        }
    }

    abstract static class BaseRangeSubscription<T> extends BasicQueueSubscription<T> {
        private static final long serialVersionUID = -2252972430506210021L;

        Iterator<? extends T> it;

        volatile boolean cancelled;

        boolean once;

        BaseRangeSubscription(Iterator<? extends T> it) {
            this.it = it;
        }

        @Override
        public final int requestFusion(int mode) {
            return mode & SYNC;
        }

        @Nullable
        @Override
        public final T poll() {
            if (it == null) {
                return null;
            }
            if (!once) {
                once = true;
            } else {
                if (!it.hasNext()) {
                    return null;
                }
            }
            return ObjectHelper.requireNonNull(it.next(), "Iterator.next() returned a null value");
        }


        @Override
        public final boolean isEmpty() {
            return it == null || !it.hasNext();
        }

        @Override
        public final void clear() {
            it = null;
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

    static final class IteratorSubscription<T> extends BaseRangeSubscription<T> {


        private static final long serialVersionUID = -6022804456014692607L;

        final Subscriber<? super T> actual;

        IteratorSubscription(Subscriber<? super T> actual, Iterator<? extends T> it) {
            super(it);
            this.actual = actual;
        }

        @Override
        void fastPath() {
            Iterator<? extends T> it = this.it;
            Subscriber<? super T> a = actual;
            for (;;) {
                if (cancelled) {
                    return;
                }

                T t;

                try {
                    t = it.next();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    a.onError(ex);
                    return;
                }

                if (cancelled) {
                    return;
                }

                if (t == null) {
                    a.onError(new NullPointerException("Iterator.next() returned a null value"));
                    return;
                } else {
                    a.onNext(t);
                }

                if (cancelled) {
                    return;
                }

                boolean b;

                try {
                    b = it.hasNext();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    a.onError(ex);
                    return;
                }


                if (!b) {
                    if (!cancelled) {
                        a.onComplete();
                    }
                    return;
                }
            }
        }

        @Override
        void slowPath(long r) {
            long e = 0L;
            Iterator<? extends T> it = this.it;
            Subscriber<? super T> a = actual;

            for (;;) {

                while (e != r) {

                    if (cancelled) {
                        return;
                    }

                    T t;

                    try {
                        t = it.next();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        a.onError(ex);
                        return;
                    }

                    if (cancelled) {
                        return;
                    }

                    if (t == null) {
                        a.onError(new NullPointerException("Iterator.next() returned a null value"));
                        return;
                    } else {
                        a.onNext(t);
                    }

                    if (cancelled) {
                        return;
                    }

                    boolean b;

                    try {
                        b = it.hasNext();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        a.onError(ex);
                        return;
                    }

                    if (!b) {
                        if (!cancelled) {
                            a.onComplete();
                        }
                        return;
                    }

                    e++;
                }

                r = get();
                if (e == r) {
                    r = addAndGet(-e);
                    if (r == 0L) {
                        return;
                    }
                    e = 0L;
                }
            }
        }

    }

    static final class IteratorConditionalSubscription<T> extends BaseRangeSubscription<T> {


        private static final long serialVersionUID = -6022804456014692607L;

        final ConditionalSubscriber<? super T> actual;

        IteratorConditionalSubscription(ConditionalSubscriber<? super T> actual, Iterator<? extends T> it) {
            super(it);
            this.actual = actual;
        }

        @Override
        void fastPath() {
            Iterator<? extends T> it = this.it;
            ConditionalSubscriber<? super T> a = actual;
            for (;;) {
                if (cancelled) {
                    return;
                }

                T t;

                try {
                    t = it.next();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    a.onError(ex);
                    return;
                }

                if (cancelled) {
                    return;
                }

                if (t == null) {
                    a.onError(new NullPointerException("Iterator.next() returned a null value"));
                    return;
                } else {
                    a.tryOnNext(t);
                }

                if (cancelled) {
                    return;
                }

                boolean b;

                try {
                    b = it.hasNext();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    a.onError(ex);
                    return;
                }

                if (!b) {
                    if (!cancelled) {
                        a.onComplete();
                    }
                    return;
                }
            }
        }

        @Override
        void slowPath(long r) {
            long e = 0L;
            Iterator<? extends T> it = this.it;
            ConditionalSubscriber<? super T> a = actual;

            for (;;) {

                while (e != r) {

                    if (cancelled) {
                        return;
                    }

                    T t;

                    try {
                        t = it.next();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        a.onError(ex);
                        return;
                    }

                    if (cancelled) {
                        return;
                    }

                    boolean b;
                    if (t == null) {
                        a.onError(new NullPointerException("Iterator.next() returned a null value"));
                        return;
                    } else {
                        b = a.tryOnNext(t);
                    }

                    if (cancelled) {
                        return;
                    }

                    boolean hasNext;

                    try {
                        hasNext = it.hasNext();
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        a.onError(ex);
                        return;
                    }

                    if (!hasNext) {
                        if (!cancelled) {
                            a.onComplete();
                        }
                        return;
                    }

                    if (b) {
                        e++;
                    }
                }

                r = get();
                if (e == r) {
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
