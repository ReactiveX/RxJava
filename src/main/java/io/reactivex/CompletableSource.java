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
package io.reactivex;

import io.reactivex.annotations.*;

/**
 * Represents a basic {@link Completable} source base interface,
 * consumable via an {@link CompletableObserver}.
 *
 * @since 2.0
 */
public interface CompletableSource {

    /**
     * Subscribes the given CompletableObserver to this CompletableSource instance.
     * @param cs the CompletableObserver, not null
     * @throws NullPointerException if {@code cs} is null
     */
    void subscribe(@NonNull CompletableObserver cs);
}
