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
 * Interface to map/wrap a downstream observer to an upstream observer.
 *
 * @param <Downstream> the value type of the downstream
 * @param <Upstream> the value type of the upstream
 */
public interface ObservableOperator<Downstream, Upstream> {
    /**
     * Applies a function to the child Observer and returns a new parent Observer.
     * @param observer the child Observer instance
     * @return the parent Observer instance
     * @throws Exception on failure
     */
    @NonNull
    Observer<? super Upstream> apply(@NonNull Observer<? super Downstream> observer) throws Exception;
}
