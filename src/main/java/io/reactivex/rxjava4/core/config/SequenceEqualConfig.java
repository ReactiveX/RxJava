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

package io.reactivex.rxjava4.core.config;

import java.util.Objects;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.functions.BiPredicate;
import io.reactivex.rxjava4.internal.functions.ObjectHelper;

/**
 * Configuration record for sequenceEqual() operators.
 * @param <T> the element type of the sequences being compared
 * @param bufferSize the expected number of items to cache from the inner {@code ObservableSource}s
 * @param isEqual the custom lambda to compare two elements
 * @since 4.0.0
 */
public record SequenceEqualConfig<T>(int bufferSize, @NonNull BiPredicate<? super T, ? super T> isEqual) {

    /**
     * The default configuration with bufferSize of Observable.bufferSize() and a default Objects.equals predicate.
     */
    public static final SequenceEqualConfig<Object> DEFAULT =
            new SequenceEqualConfig<>(Observable.bufferSize(), ObjectHelper.equalsPredicate());

    /**
     * Constructs a configuration record.
     * @param bufferSize the expected number of row combination items to be buffered internally
     */
    public SequenceEqualConfig(int bufferSize) {
        this(bufferSize, ObjectHelper.equalsPredicate());
    }

    /**
     * Constructs a configuration record.
 * @param isEqual the custom lambda to compare two elements
     */
    public SequenceEqualConfig(@NonNull BiPredicate<? super T, ? super T> isEqual) {
        this(Observable.bufferSize(), isEqual);
    }

    /**
     * Constructs a configuration record.
     * @param bufferSize the expected number of items to cache from the inner {@code ObservableSource}s
     * @param isEqual the custom lambda to compare two elements
     */
    public SequenceEqualConfig {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        Objects.requireNonNull(isEqual, "isEqual is null");
    }
}
