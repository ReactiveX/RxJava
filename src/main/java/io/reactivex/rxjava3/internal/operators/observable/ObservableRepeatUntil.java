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
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.BooleanSupplier;
import io.reactivex.rxjava3.internal.disposables.SequentialDisposable;

public final class ObservableRepeatUntil<T> extends AbstractObservableWithUpstream<T, T> {
    final BooleanSupplier until;
    public ObservableRepeatUntil(Observable<T> source, BooleanSupplier until) {
        super(source);
        this.until = until;
    }

    @Override
    public void subscribeActual(Observer<? super T> observer) {
        SequentialDisposable sd = new SequentialDisposable();
        observer.onSubscribe(sd);

        RepeatUntilObserver<T> rs = new RepeatUntilObserver<T>(observer, until, sd, source);
        rs.subscribeNext();
    }

    static final class RepeatUntilObserver<T> extends AtomicInteger implements Observer<T> {

        private static final long serialVersionUID = -7098360935104053232L;

        final Observer<? super T> downstream;
        final SequentialDisposable upstream;
        final ObservableSource<? extends T> source;
        final BooleanSupplier stop;
        RepeatUntilObserver(Observer<? super T> actual, BooleanSupplier until, SequentialDisposable sd, ObservableSource<? extends T> source) {
            this.downstream = actual;
            this.upstream = sd;
            this.source = source;
            this.stop = until;
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
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            boolean b;
            try {
                b = stop.getAsBoolean();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                downstream.onError(e);
                return;
            }
            if (b) {
                downstream.onComplete();
            } else {
                subscribeNext();
            }
        }

        /**
         * Subscribes to the source again via trampolining.
         */
        void subscribeNext() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                for (;;) {
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
