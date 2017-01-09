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

package io.reactivex.internal.operators.maybe;

import io.reactivex.*;
import io.reactivex.internal.fuseable.HasUpstreamMaybeSource;

/**
 * Abstract base class for intermediate Maybe operators that take an upstream MaybeSource.
 *
 * @param <T> the source value type
 * @param <R> the output value type
 */
abstract class AbstractMaybeWithUpstream<T, R> extends Maybe<R> implements HasUpstreamMaybeSource<T> {

    protected final MaybeSource<T> source;

    AbstractMaybeWithUpstream(MaybeSource<T> source) {
        this.source = source;
    }

    @Override
    public final MaybeSource<T> source() {
        return source;
    }
}
