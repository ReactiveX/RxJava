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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.SequentialDisposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Concatenate values of each MaybeSource provided in an array and delays
 * any errors till the very end.
 *
 * @param <T> the value type
 */
public final class MaybeConcatArrayDelayError<T> extends Flowable<T> {

    final MaybeSource<? extends T>[] sources;

    public MaybeConcatArrayDelayError(MaybeSource<? extends T>[] sources) {
        this.sources = sources;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        ConcatMaybeObserver<T> parent = new ConcatMaybeObserver<T>(s, sources);
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

        final MaybeSource<? extends T>[] sources;

        final AtomicThrowable errors;

        int index;

        long produced;

        ConcatMaybeObserver(Subscriber<? super T> actual, MaybeSource<? extends T>[] sources) {
            this.actual = actual;
            this.sources = sources;
            this.requested = new AtomicLong();
            this.disposables = new SequentialDisposable();
            this.current = new AtomicReference<Object>(NotificationLite.COMPLETE); // as if a previous completed
            this.errors = new AtomicThrowable();
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
            current.lazySet(NotificationLite.COMPLETE);
            if (errors.addThrowable(e)) {
                drain();
            } else {
                RxJavaPlugins.onError(e);
            }
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
                        int i = index;
                        if (i == sources.length) {
                            Throwable ex = errors.get();
                            if (ex != null) {
                                a.onError(errors.terminate());
                            } else {
                                a.onComplete();
                            }
                            return;
                        }
                        index = i + 1;

                        sources[i].subscribe(this);
                    }
                }

                if (decrementAndGet() == 0) {
                    break;
                }
            }
        }
    }
}
