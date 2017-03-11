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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.SequentialDisposable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Delays the subscription to the main source until the other
 * observable fires an event or completes.
 * @param <T> the main type
 * @param <U> the other value type, ignored
 */
public final class ObservableDelaySubscriptionOther<T, U> extends Observable<T> {
    final ObservableSource<? extends T> main;
    final ObservableSource<U> other;

    public ObservableDelaySubscriptionOther(ObservableSource<? extends T> main, ObservableSource<U> other) {
        this.main = main;
        this.other = other;
    }

    @Override
    public void subscribeActual(final Observer<? super T> child) {
        final SequentialDisposable serial = new SequentialDisposable();
        child.onSubscribe(serial);

        Observer<U> otherObserver = new DelayObserver(serial, child);

        other.subscribe(otherObserver);
    }

    final class DelayObserver implements Observer<U> {
        final SequentialDisposable serial;
        final Observer<? super T> child;
        boolean done;

        DelayObserver(SequentialDisposable serial, Observer<? super T> child) {
            this.serial = serial;
            this.child = child;
        }

        @Override
        public void onSubscribe(Disposable d) {
            serial.update(d);
        }

        @Override
        public void onNext(U t) {
            onComplete();
        }

        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaPlugins.onError(e);
                return;
            }
            done = true;
            child.onError(e);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;

            main.subscribe(new OnComplete());
        }

        final class OnComplete implements Observer<T> {
            @Override
            public void onSubscribe(Disposable d) {
                serial.update(d);
            }

            @Override
            public void onNext(T value) {
                child.onNext(value);
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onComplete() {
                child.onComplete();
            }
        }
    }
}
