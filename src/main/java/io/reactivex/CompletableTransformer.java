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
 * Convenience interface and callback used by the compose operator to turn a Completable into another
 * Completable fluently.
 */
public interface CompletableTransformer {
    /**
     * Applies a function to the upstream Completable and returns a CompletableSource.
     * @param upstream the upstream Completable instance
     * @return the transformed CompletableSource instance
     */
    @NonNull
    CompletableSource apply(@NonNull Completable upstream);
}
