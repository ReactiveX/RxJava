/**
 * Copyright (c) 2016-present, RxJava Contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.reactivex.internal.operators.single;

import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import org.reactivestreams.Publisher;

/**
 * Helper function to merge/concat values of each SingleSource provided by a Publisher.
 */
public class SingleToPublisher implements Function<SingleSource<Object>, Publisher<Object>> {
    public static final Function<?, ?> INSTANCE = new SingleToPublisher();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Function<SingleSource<T>, Publisher<T>> instance() {
        return (Function)INSTANCE;
    }

    @Override
    public Publisher<Object> apply(SingleSource<Object> t) throws Exception {
        return new SingleToFlowable<>(t);
    }
}
