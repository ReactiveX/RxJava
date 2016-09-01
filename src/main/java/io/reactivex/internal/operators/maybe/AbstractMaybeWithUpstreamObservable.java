/**
 * Copyright 2016 Netflix, Inc.
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
import io.reactivex.internal.fuseable.HasUpstreamObservableSource;

/**
 * Base class for operators with a source consumable.
 *
 * @param <T> the input source type
 * @param <U> the output type
 */
public abstract class AbstractMaybeWithUpstreamObservable<T, U> extends Maybe<U> implements HasUpstreamObservableSource<T> {

    /** The source consumable Observable. */
    protected final ObservableSource<T> source;
    
    /**
     * Constructs the ObservableSource with the given consumable.
     * @param source the consumable Observable
     */
    public AbstractMaybeWithUpstreamObservable(ObservableSource<T> source) {
        this.source = source;
    }
    
    @Override
    public final ObservableSource<T> source() {
        return source;
    }

}
