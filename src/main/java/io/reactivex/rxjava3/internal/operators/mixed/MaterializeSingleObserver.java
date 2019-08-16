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

package io.reactivex.rxjava3.internal.operators.mixed;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

/**
 * A consumer that implements the consumer types of Maybe, Single and Completable
 * and turns their signals into Notifications for a SingleObserver.
 * <p>History: 2.2.4 - experimental
 * @param <T> the element type of the source
 * @since 3.0.0
 */
public final class MaterializeSingleObserver<T>
implements SingleObserver<T>, MaybeObserver<T>, CompletableObserver, Disposable {

    final SingleObserver<? super Notification<T>> downstream;

    Disposable upstream;

    public MaterializeSingleObserver(SingleObserver<? super Notification<T>> downstream) {
        this.downstream = downstream;
    }

    @Override
    public void onSubscribe(Disposable d) {
        if (DisposableHelper.validate(upstream, d)) {
            this.upstream = d;
            downstream.onSubscribe(this);
        }
    }

    @Override
    public void onComplete() {
        downstream.onSuccess(Notification.<T>createOnComplete());
    }

    @Override
    public void onSuccess(T t) {
        downstream.onSuccess(Notification.<T>createOnNext(t));
    }

    @Override
    public void onError(Throwable e) {
        downstream.onSuccess(Notification.<T>createOnError(e));
    }

    @Override
    public boolean isDisposed() {
        return upstream.isDisposed();
    }

    @Override
    public void dispose() {
        upstream.dispose();
    }
}
