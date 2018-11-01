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
import io.reactivex.internal.disposables.DisposableHelper;

/**
 * Maps the Notification success value of the source back to normal
 * onXXX method call as a Maybe.
 * @param <T> the element type of the notification and result
 * @since 2.2.4 - experimental
 */
@Experimental
public final class SingleDematerialize<T> extends Maybe<T> {

    final Single<Object> source;

    public SingleDematerialize(Single<Object> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new DematerializeObserver<T>(observer));
    }

    static final class DematerializeObserver<T> implements SingleObserver<Object>, Disposable {

        final MaybeObserver<? super T> downstream;

        Disposable upstream;

        DematerializeObserver(MaybeObserver<? super T> downstream) {
            this.downstream = downstream;
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
        public void onSuccess(Object t) {
            if (t instanceof Notification) {
                @SuppressWarnings("unchecked")
                Notification<T> notification = (Notification<T>)t;
                if (notification.isOnNext()) {
                    downstream.onSuccess(notification.getValue());
                } else if (notification.isOnComplete()) {
                    downstream.onComplete();
                } else {
                    downstream.onError(notification.getError());
                }
            } else {
                downstream.onError(new ClassCastException("io.reactivex.Notification expected but got " + t.getClass()));
            }
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }
    }
}
