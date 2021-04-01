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

package io.reactivex.rxjava3.disposables;

import io.reactivex.rxjava3.annotations.NonNull;

/**
 * A disposable container that manages a {@link Runnable} instance.
 */
final class RunnableDisposable extends ReferenceDisposable<Runnable> {

    private static final long serialVersionUID = -8219729196779211169L;

    RunnableDisposable(Runnable value) {
        super(value);
    }

    @Override
    protected void onDisposed(@NonNull Runnable value) {
        value.run();
    }

    @Override
    public String toString() {
        return "RunnableDisposable(disposed=" + isDisposed() + ", " + get() + ")";
    }
}
