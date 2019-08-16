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

package io.reactivex.rxjava3.internal.operators.observable;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.internal.disposables.SequentialDisposable;

public final class ObservableRetryPredicate<T> extends AbstractObservableWithUpstream<T, T> {
    final Predicate<? super Throwable> predicate;
    final long count;
    public ObservableRetryPredicate(Observable<T> source,
            long count,
            Predicate<? super Throwable> predicate) {
        super(source);
        this.predicate = predicate;
        this.count = count;
    }

    @Override
    public void subscribeActual(Observer<? super T> observer) {
        SequentialDisposable sa = new SequentialDisposable();
        observer.onSubscribe(sa);

        RepeatObserver<T> rs = new RepeatObserver<T>(observer, count, predicate, sa, source);
        rs.subscribeNext();
    }

    static final class RepeatObserver<T> extends AtomicInteger implements Observer<T> {

        private static final long serialVersionUID = -7098360935104053232L;

        final Observer<? super T> downstream;
        final SequentialDisposable upstream;
        final ObservableSource<? extends T> source;
        final Predicate<? super Throwable> predicate;
        long remaining;
        RepeatObserver(Observer<? super T> actual, long count,
                Predicate<? super Throwable> predicate, SequentialDisposable sa, ObservableSource<? extends T> source) {
            this.downstream = actual;
            this.upstream = sa;
            this.source = source;
            this.predicate = predicate;
            this.remaining = count;
        }

        @Override
        public void onSubscribe(Disposable d) {
            upstream.replace(d);
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            long r = remaining;
            if (r != Long.MAX_VALUE) {
                remaining = r - 1;
            }
            if (r == 0) {
                downstream.onError(t);
            } else {
                boolean b;
                try {
                    b = predicate.test(t);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    downstream.onError(new CompositeException(t, e));
                    return;
                }
                if (!b) {
                    downstream.onError(t);
                    return;
                }
                subscribeNext();
            }
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        /**
         * Subscribes to the source again via trampolining.
         */
        void subscribeNext() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                for (;;) {
                    if (upstream.isDisposed()) {
                        return;
                    }
                    source.subscribe(this);

                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                }
            }
        }
    }
}
