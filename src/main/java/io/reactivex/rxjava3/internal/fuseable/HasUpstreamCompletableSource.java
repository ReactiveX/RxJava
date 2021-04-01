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

package io.reactivex.rxjava3.internal.fuseable;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.CompletableSource;

/**
 * Interface indicating the implementor has an upstream CompletableSource-like source available
 * via {@link #source()} method.
 */
public interface HasUpstreamCompletableSource {
    /**
     * Returns the upstream source of this Completable.
     * <p>Allows discovering the chain of observables.
     * @return the source CompletableSource
     */
    @NonNull
    CompletableSource source();
}
