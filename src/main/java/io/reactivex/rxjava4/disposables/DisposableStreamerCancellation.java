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

import io.reactivex.rxjava4.internal.disposables.NeverDisposableStreamerCancellation;

/**
 * Represents the full, disposable cancellation interface for {@code Streamer}
 * operations.
 * <p>
 * It was decided to have a separate set of interfaces instead of trying to
 * retrofit {@link DisposableContainer}.
 * @since 4.0.0
 */
public interface DisposableStreamerCancellation extends StreamerCancellation, Disposable {

    /**
     * Returns a constant instance which does nothing, cannot be disposed and
     * accepts any incoming Disposable without registering it or handling it in any form,
     * because this {@code never} instance cannot be disposed to begin with.
     * @return the shared constant no-op instance
     */
    static DisposableStreamerCancellation never() {
        return NeverDisposableStreamerCancellation.INSTANCE;
    }
}
