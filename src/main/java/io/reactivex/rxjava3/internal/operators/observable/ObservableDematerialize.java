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

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

import java.util.Objects;

public final class ObservableDematerialize<T, R> extends AbstractObservableWithUpstream<T, R> {

    final Function<? super T, ? extends Notification<R>> selector;

    public ObservableDematerialize(ObservableSource<T> source, Function<? super T, ? extends Notification<R>> selector) {
        super(source);
        this.selector = selector;
    }

    @Override
    public void subscribeActual(Observer<? super R> observer) {
        source.subscribe(new DematerializeObserver<T, R>(observer, selector));
    }

    static final class DematerializeObserver<T, R> implements Observer<T>, Disposable {
        final Observer<? super R> downstream;

        final Function<? super T, ? extends Notification<R>> selector;

        boolean done;

        Disposable upstream;

        DematerializeObserver(Observer<? super R> downstream, Function<? super T, ? extends Notification<R>> selector) {
            this.downstream = downstream;
            this.selector = selector;
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
        public void onNext(T item) {
            if (done) {
                if (item instanceof Notification) {
                    Notification<?> notification = (Notification<?>)item;
                    if (notification.isOnError()) {
                        RxJavaPlugins.onError(notification.getError());
                    }
                }
                return;
            }

            Notification<R> notification;

            try {
                notification = Objects.requireNonNull(selector.apply(item), "The selector returned a null Notification");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.dispose();
                onError(ex);
                return;
            }
            if (notification.isOnError()) {
                upstream.dispose();
                onError(notification.getError());
            }
            else if (notification.isOnComplete()) {
                upstream.dispose();
                onComplete();
            } else {
                downstream.onNext(notification.getValue());
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
