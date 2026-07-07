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

package io.reactivex.rxjava4.disposables;
/**
 * Implementation of a never disposable container.
 * @since 4.0.0
 */
record NeverDisposableContainer() implements DisposableContainer {

    @Override
    public void dispose() {
        // Deliberately empty
    }

    @Override
    public boolean isDisposed() {
        // Who cares?
        return false;
    }

    @Override
    public boolean add(Disposable d) {
        // Who cares?
        return false;
    }

    @Override
    public boolean remove(Disposable d) {
        // Who cares?
        return false;
    }

    @Override
    public boolean delete(Disposable d) {
        // Who cares?
        return false;
    }

    @Override
    public void reset() {
        // Who cares?
    }

    @Override
    public void clear() {
        // Who cares?
    }

    @Override
    public DisposableContainer derive() {
        return NEVER;
    }
}
