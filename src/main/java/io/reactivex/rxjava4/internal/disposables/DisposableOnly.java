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

package io.reactivex.rxjava4.internal.disposables;

import io.reactivex.rxjava4.disposables.Disposable;

/**
 * An extension to {@link Disposable} that allows not
 * implementing the {@link Disposable#isDisposed()} as it
 * is practically never needed or cannot be observed anyways.
 * @since 4.0.0
 */
public interface DisposableOnly extends Disposable {

    @Override
    default boolean isDisposed() {
        throw new UnsupportedOperationException("The class " + this.getClass() + " does not support isDisposed");
    }
}
