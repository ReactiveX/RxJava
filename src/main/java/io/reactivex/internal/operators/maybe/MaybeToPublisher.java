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

import org.reactivestreams.Publisher;

import io.reactivex.MaybeSource;
import io.reactivex.functions.Function;

/**
 * Helper function to merge/concat values of each MaybeSource provided by a Publisher.
 */
public enum MaybeToPublisher implements Function<MaybeSource<Object>, Publisher<Object>> {
    INSTANCE;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Function<MaybeSource<T>, Publisher<T>> instance() {
        return (Function)INSTANCE;
    }

    @Override
    public Publisher<Object> apply(MaybeSource<Object> t) throws Exception {
        return new MaybeToFlowable<Object>(t);
    }
}
