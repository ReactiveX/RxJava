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

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.internal.functions.ObjectHelper;
import io.reactivex.rxjava4.core.ErrorMode;

/**
 * Configuration record for Observable.concatMap() operators.
 * @param errorMode how to handle when errors appear from the inner or outer sources
 * @param bufferSize the expected number of outer sources to buffer while processing an inner source
 * @since 4.0.0
 */
public record ObservableConcatMapConfig(@NonNull ErrorMode errorMode, int bufferSize) {

    /**
     * The default configuration with no error delays and bufferSize of 2.
     */
    public static final ObservableConcatMapConfig DEFAULT = new ObservableConcatMapConfig(ErrorMode.IMMEDIATE, 2);

    /**
     * The default configuration with error delays till the end and bufferSize of 2.
     */
    public static final ObservableConcatMapConfig DELAY_ERROR = new ObservableConcatMapConfig(ErrorMode.END, 2);

    /**
     * The default configuration with error delays till the boundary and bufferSize of 2.
     */
    public static final ObservableConcatMapConfig DELAY_ERROR_BOUNDARY = new ObservableConcatMapConfig(ErrorMode.BOUNDARY, 2);

    /**
     * Constructs a configuration record.
     * @param errorMode how to handle when errors appear from the inner or outer sources
     */
    public ObservableConcatMapConfig(@NonNull ErrorMode errorMode) {
        this(errorMode, 2);
    }

    /**
     * Constructs a configuration record.
     * @param bufferSize the expected number of outer sources to buffer while processing an inner source
     */
    public ObservableConcatMapConfig(int bufferSize) {
        this(ErrorMode.IMMEDIATE, bufferSize);
    }

    /**
     * Constructs a configuration record.
     * @param errorMode how to handle when errors appear from the inner or outer sources
     * @param bufferSize the expected number of outer sources to buffer while processing an inner source
     */
    public ObservableConcatMapConfig {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
    }
}
