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
        final Observer<? super T> downstream;

        boolean done;

        Disposable upstream;

        DematerializeObserver(Observer<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
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
                upstream.dispose();
                onError(t.getError());
            }
            else if (t.isOnComplete()) {
                upstream.dispose();
                onComplete();
            } else {
                downstream.onNext(t.getValue());
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;

            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;

            downstream.onComplete();
        }
    }
}
