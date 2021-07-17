/*
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

/*
 * Copyright 2016-2019 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.rxjava3.internal.observers;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.DisposableContainer;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;

/**
 * Wraps lambda callbacks and when the upstream terminates or this observer gets disposed,
 * removes itself from a {@link io.reactivex.rxjava3.disposables.CompositeDisposable}.
 * <p>History: 0.18.0 @ RxJavaExtensions
 * @param <T> the element type consumed
 * @since 3.1.0
 */
public final class DisposableAutoReleaseObserver<T>
extends AbstractDisposableAutoRelease
implements Observer<T> {

    private static final long serialVersionUID = 8924480688481408726L;

    final Consumer<? super T> onNext;

    public DisposableAutoReleaseObserver(
            DisposableContainer composite,
            Consumer<? super T> onNext,
            Consumer<? super Throwable> onError,
            Action onComplete
    ) {
        super(composite, onError, onComplete);
        this.onNext = onNext;
    }

    @Override
    public void onNext(T t) {
        if (get() != DisposableHelper.DISPOSED) {
            try {
                onNext.accept(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                get().dispose();
                onError(e);
            }
        }
    }

}
