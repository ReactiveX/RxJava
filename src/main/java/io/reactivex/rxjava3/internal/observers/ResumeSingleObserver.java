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

package io.reactivex.rxjava3.internal.observers;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

/**
 * A SingleObserver implementation used for subscribing to the actual SingleSource
 * and replace the current Disposable in a parent AtomicReference.
 *
 * @param <T> the value type
 */
public final class ResumeSingleObserver<T> implements SingleObserver<T> {

    final AtomicReference<Disposable> parent;

    final SingleObserver<? super T> downstream;

    public ResumeSingleObserver(AtomicReference<Disposable> parent, SingleObserver<? super T> downstream) {
        this.parent = parent;
        this.downstream = downstream;
    }

    @Override
    public void onSubscribe(Disposable d) {
        DisposableHelper.replace(parent, d);
    }

    @Override
    public void onSuccess(T value) {
        downstream.onSuccess(value);
    }

    @Override
    public void onError(Throwable e) {
        downstream.onError(e);
    }
}
