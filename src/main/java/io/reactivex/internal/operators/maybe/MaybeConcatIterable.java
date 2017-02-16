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

package io.reactivex.internal.operators.maybe;

import java.util.Iterator;
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.disposables.SequentialDisposable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.*;

/**
 * Concatenate values of each MaybeSource provided by an Iterable.
 *
 * @param <T> the value type
 */
public final class MaybeConcatIterable<T> extends Flowable<T> {

    final Iterable<? extends MaybeSource<? extends T>> sources;

    public MaybeConcatIterable(Iterable<? extends MaybeSource<? extends T>> sources) {
        this.sources = sources;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {

        Iterator<? extends MaybeSource<? extends T>> it;

        try {
            it = ObjectHelper.requireNonNull(sources.iterator(), "The sources Iterable returned a null Iterator");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, s);
            return;
        }

        ConcatMaybeObserver<T> parent = new ConcatMaybeObserver<T>(s, it);
        s.onSubscribe(parent);
        parent.drain();
    }

    static final class ConcatMaybeObserver<T>
    extends AtomicInteger
    implements MaybeObserver<T>, Subscription {

        private static final long serialVersionUID = 3520831347801429610L;

        final Subscriber<? super T> actual;

        final AtomicLong requested;

        final AtomicReference<Object> current;

        final SequentialDisposable disposables;

        final Iterator<? extends MaybeSource<? extends T>> sources;

        long produced;

        ConcatMaybeObserver(Subscriber<? super T> actual, Iterator<? extends MaybeSource<? extends T>> sources) {
            this.actual = actual;
            this.sources = sources;
            this.requested = new AtomicLong();
            this.disposables = new SequentialDisposable();
            this.current = new AtomicReference<Object>(NotificationLite.COMPLETE); // as if a previous completed
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
            disposables.dispose();
        }

        @Override
        public void onSubscribe(Disposable d) {
            disposables.replace(d);
        }

        @Override
        public void onSuccess(T value) {
            current.lazySet(value);
            drain();
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }

        @Override
        public void onComplete() {
            current.lazySet(NotificationLite.COMPLETE);
            drain();
        }

        @SuppressWarnings("unchecked")
        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            AtomicReference<Object> c = current;
            Subscriber<? super T> a = actual;
            Disposable cancelled = disposables;

            for (;;) {
                if (cancelled.isDisposed()) {
                    c.lazySet(null);
                    return;
                }

                Object o = c.get();

                if (o != null) {
                    boolean goNextSource;
                    if (o != NotificationLite.COMPLETE) {
                        long p = produced;
                        if (p != requested.get()) {
                            produced = p + 1;
                            c.lazySet(null);
                            goNextSource = true;

                            a.onNext((T)o);
                        } else {
                            goNextSource = false;
                        }
                    } else {
                        goNextSource = true;
                        c.lazySet(null);
                    }

                    if (goNextSource && !cancelled.isDisposed()) {
                        boolean b;

                        try {
                            b = sources.hasNext();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            a.onError(ex);
                            return;
                        }

                        if (b) {
                            MaybeSource<? extends T> source;

                            try {
                                source = ObjectHelper.requireNonNull(sources.next(), "The source Iterator returned a null MaybeSource");
                            } catch (Throwable ex) {
                                Exceptions.throwIfFatal(ex);
                                a.onError(ex);
                                return;
                            }

                            source.subscribe(this);
                        } else {
                            a.onComplete();
                        }
                    }
                }

                if (decrementAndGet() == 0) {
                    break;
                }
            }
        }
    }
}
