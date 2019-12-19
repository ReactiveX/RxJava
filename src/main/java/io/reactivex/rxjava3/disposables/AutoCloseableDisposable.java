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

package io.reactivex.rxjava3.disposables;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;

/**
 * A disposable container that manages an {@link AutoCloseable} instance.
 * @since 3.0.0
 */
final class AutoCloseableDisposable extends ReferenceDisposable<AutoCloseable> {

    private static final long serialVersionUID = -6646144244598696847L;

    AutoCloseableDisposable(AutoCloseable value) {
        super(value);
    }

    @Override
    protected void onDisposed(@NonNull AutoCloseable value) {
        try {
            value.close();
        } catch (Throwable ex) {
            RxJavaPlugins.onError(ex);
        }
    }

    @Override
    public String toString() {
        return "AutoCloseableDisposable(disposed=" + isDisposed() + ", " + get() + ")";
    }

}
