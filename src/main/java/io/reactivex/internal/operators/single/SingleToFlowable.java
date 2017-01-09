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

import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.subscriptions.DeferredScalarSubscription;

/**
 * Wraps a Single and exposes it as a Flowable.
 *
 * @param <T> the value type
 */
public final class SingleToFlowable<T> extends Flowable<T> {

    final SingleSource<? extends T> source;

    public SingleToFlowable(SingleSource<? extends T> source) {
        this.source = source;
    }

    @Override
    public void subscribeActual(final Subscriber<? super T> s) {
        source.subscribe(new SingleToFlowableObserver<T>(s));
    }

    static final class SingleToFlowableObserver<T> extends DeferredScalarSubscription<T>
    implements SingleObserver<T> {


        private static final long serialVersionUID = 187782011903685568L;

        Disposable d;

        SingleToFlowableObserver(Subscriber<? super T> actual) {
            super(actual);
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.d, d)) {
                this.d = d;

                actual.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            complete(value);
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }

        @Override
        public void cancel() {
            super.cancel();
            d.dispose();
        }
    }
}
