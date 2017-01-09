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

package io.reactivex.internal.operators.observable;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.SequentialDisposable;

public final class ObservableRepeat<T> extends AbstractObservableWithUpstream<T, T> {
    final long count;
    public ObservableRepeat(Observable<T> source, long count) {
        super(source);
        this.count = count;
    }

    @Override
    public void subscribeActual(Observer<? super T> s) {
        SequentialDisposable sd = new SequentialDisposable();
        s.onSubscribe(sd);

        RepeatObserver<T> rs = new RepeatObserver<T>(s, count != Long.MAX_VALUE ? count - 1 : Long.MAX_VALUE, sd, source);
        rs.subscribeNext();
    }

    static final class RepeatObserver<T> extends AtomicInteger implements Observer<T> {

        private static final long serialVersionUID = -7098360935104053232L;

        final Observer<? super T> actual;
        final SequentialDisposable sd;
        final ObservableSource<? extends T> source;
        long remaining;
        RepeatObserver(Observer<? super T> actual, long count, SequentialDisposable sd, ObservableSource<? extends T> source) {
            this.actual = actual;
            this.sd = sd;
            this.source = source;
            this.remaining = count;
        }

        @Override
        public void onSubscribe(Disposable s) {
            sd.replace(s);
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }
        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            long r = remaining;
            if (r != Long.MAX_VALUE) {
                remaining = r - 1;
            }
            if (r != 0L) {
                subscribeNext();
            } else {
                actual.onComplete();
            }
        }

        /**
         * Subscribes to the source again via trampolining.
         */
        void subscribeNext() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                for (;;) {
                    if (sd.isDisposed()) {
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
