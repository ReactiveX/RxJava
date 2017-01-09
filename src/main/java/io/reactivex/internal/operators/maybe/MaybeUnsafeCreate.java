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

/**
 * Wraps a MaybeSource without safeguard and calls its subscribe() method for each MaybeObserver.
 *
 * @param <T> the value type
 */
public final class MaybeUnsafeCreate<T> extends AbstractMaybeWithUpstream<T, T> {

    public MaybeUnsafeCreate(MaybeSource<T> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(observer);
    }

}
