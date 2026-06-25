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
import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.internal.functions.ObjectHelper;
import io.reactivex.rxjava4.core.ErrorMode;

/**
 * Configuration record for Observable.concat() operators.
 * @param errorMode how to handle when errors appear from the inner or outer sources
 * @param bufferSize the expected number of outer sources to buffer while processing an inner source
 * @since 4.0.0
 */
public record ObservableConcatConfig(@NonNull ErrorMode errorMode, int bufferSize) {

    /**
     * The default configuration with no error delays and bufferSize of {@link Observable#bufferSize()}.
     */
    public static final ObservableConcatConfig DEFAULT = new ObservableConcatConfig(ErrorMode.IMMEDIATE, Observable.bufferSize());

    /**
     * The default configuration with error delays and bufferSize of {@link Observable#bufferSize()}.
     */
    public static final ObservableConcatConfig DELAY_ERROR = new ObservableConcatConfig(ErrorMode.END, Observable.bufferSize());

    /**
     * The default configuration with error delays and bufferSize of {@link Observable#bufferSize()}.
     */
    public static final ObservableConcatConfig DELAY_ERROR_BOUNDARY = new ObservableConcatConfig(ErrorMode.BOUNDARY, Observable.bufferSize());

    /**
     * Constructs a configuration record.
     * @param errorMode how to handle when errors appear from the inner or outer sources
     */
    public ObservableConcatConfig(@NonNull ErrorMode errorMode) {
        this(errorMode, Observable.bufferSize());
    }

    /**
     * Constructs a configuration record.
     * @param bufferSize the expected number of outer sources to buffer while processing an inner source
     */
    public ObservableConcatConfig(int bufferSize) {
        this(ErrorMode.IMMEDIATE, bufferSize);
    }

    /**
     * Constructs a configuration record.
     * @param errorMode how to handle when errors appear from the inner or outer sources
     * @param bufferSize the expected number of outer sources to buffer while processing an inner source
     */
    public ObservableConcatConfig {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
    }
}
