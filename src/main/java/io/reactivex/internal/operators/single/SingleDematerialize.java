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

package io.reactivex.internal.operators.single;

import io.reactivex.*;
import io.reactivex.annotations.Experimental;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;

/**
 * Maps the success value of the source to a Notification, then
 * maps it back to the corresponding signal type.
 * @param <T> the element type of the source
 * @param <R> the element type of the Notification and result
 * @since 2.2.4 - experimental
 */
@Experimental
public final class SingleDematerialize<T, R> extends Maybe<R> {

    final Single<T> source;

    final Function<? super T, Notification<R>> selector;

    public SingleDematerialize(Single<T> source, Function<? super T, Notification<R>> selector) {
        this.source = source;
        this.selector = selector;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super R> observer) {
        source.subscribe(new DematerializeObserver<T, R>(observer, selector));
    }

    static final class DematerializeObserver<T, R> implements SingleObserver<T>, Disposable {

        final MaybeObserver<? super R> downstream;

        final Function<? super T, Notification<R>> selector;

        Disposable upstream;

        DematerializeObserver(MaybeObserver<? super R> downstream,
                Function<? super T, Notification<R>> selector) {
            this.downstream = downstream;
            this.selector = selector;
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
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(upstream, d)) {
                upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T t) {
            Notification<R> notification;

            try {
                notification = ObjectHelper.requireNonNull(selector.apply(t), "The selector returned a null Notification");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }
            if (notification.isOnNext()) {
                downstream.onSuccess(notification.getValue());
            } else if (notification.isOnComplete()) {
                downstream.onComplete();
            } else {
                downstream.onError(notification.getError());
            }
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }
    }
}
