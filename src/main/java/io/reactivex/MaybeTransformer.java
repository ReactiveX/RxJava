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
 * Interface to compose Maybes.
 *
 * @param <Upstream> the upstream value type
 * @param <Downstream> the downstream value type
 */
public interface MaybeTransformer<Upstream, Downstream> {
    /**
     * Applies a function to the upstream Maybe and returns a MaybeSource with
     * optionally different element type.
     * @param upstream the upstream Maybe instance
     * @return the transformed MaybeSource instance
     */
    @NonNull
    MaybeSource<Downstream> apply(@NonNull Maybe<Upstream> upstream);
}
