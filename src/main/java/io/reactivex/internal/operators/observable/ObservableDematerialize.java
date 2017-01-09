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
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableDematerialize<T> extends AbstractObservableWithUpstream<Notification<T>, T> {

    public ObservableDematerialize(ObservableSource<Notification<T>> source) {
        super(source);
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        source.subscribe(new DematerializeObserver<T>(t));
    }

    static final class DematerializeObserver<T> implements Observer<Notification<T>>, Disposable {
        final Observer<? super T> actual;

        boolean done;

        Disposable s;

        DematerializeObserver(Observer<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;

                actual.onSubscribe(this);
            }
        }


        @Override
        public void dispose() {
            s.dispose();
        }

        @Override
        public boolean isDisposed() {
            return s.isDisposed();
        }


        @Override
        public void onNext(Notification<T> t) {
            if (done) {
                if (t.isOnError()) {
                    RxJavaPlugins.onError(t.getError());
                }
                return;
            }
            if (t.isOnError()) {
                s.dispose();
                onError(t.getError());
            }
            else if (t.isOnComplete()) {
                s.dispose();
                onComplete();
            } else {
                actual.onNext(t.getValue());
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;

            actual.onError(t);
        }
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;

            actual.onComplete();
        }
    }
}
