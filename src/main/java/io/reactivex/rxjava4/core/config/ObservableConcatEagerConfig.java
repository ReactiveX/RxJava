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
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.internal.functions.ObjectHelper;

/**
 * Configuration record for Observable.concatEager() operators.
 * @param errorMode how to handle when errors appear from the inner or outer sources
 * @param maxConcurrency the maximum number of concurrent flows?
 * @param bufferSize what would be the buffer size?
 * @since 4.0.0
 */
public record ObservableConcatEagerConfig(@NonNull ErrorMode errorMode, int maxConcurrency, int bufferSize) {

    /**
     * The default configuration with no error delays, maxConcurrency and bufferSize of {@link Observable#bufferSize()}.
     */
    public static final ObservableConcatEagerConfig DEFAULT = new ObservableConcatEagerConfig(ErrorMode.IMMEDIATE);

    /**
     * The default configuration with error delays, maxConcurrency and bufferSize of {@link Observable#bufferSize()}.
     */
    public static final ObservableConcatEagerConfig DELAY_ERROR = new ObservableConcatEagerConfig(ErrorMode.END);

    /**
     * The default configuration with error delays, maxConcurrency and bufferSize of {@link Observable#bufferSize()}.
     */
    public static final ObservableConcatEagerConfig DELAY_ERROR_BOUNDARY = new ObservableConcatEagerConfig(ErrorMode.BOUNDARY);

    /**
     * The default configuration with no error delays, maxConcurrency of MAX_INT and bufferSize of {@link Observable#bufferSize()}.
     */
    public static final ObservableConcatEagerConfig MAX_DEFAULT = new ObservableConcatEagerConfig(ErrorMode.IMMEDIATE, Integer.MAX_VALUE);

    /**
     * Optionally delay error, {@link Observable#bufferSize()} sizes
     * @param errorMode how to handle when errors appear from the inner or outer sources
     */
    public ObservableConcatEagerConfig(ErrorMode errorMode) {
        this(errorMode, Observable.bufferSize(), Observable.bufferSize());
    }

    /**
     * Optionally set the buffer size, no delay errors.
     * @param maxConcurrency the maximum number of concurrent flows
     */
    public ObservableConcatEagerConfig(int maxConcurrency) {
        this(ErrorMode.IMMEDIATE, maxConcurrency, Observable.bufferSize());
    }

    /**
     * Optionally delays errors and sets the buffer size too.
     * @param errorMode how to handle when errors appear from the inner or outer sources
     * @param maxConcurrency the maximum number of concurrent flows
     */
    public ObservableConcatEagerConfig(@NonNull ErrorMode errorMode, int maxConcurrency) {
        this(errorMode, maxConcurrency, Flowable.bufferSize());
    }

    /**
     * Fully customize the configuration.
     * @param errorMode how to handle when errors appear from the inner or outer sources
     * @param maxConcurrency the maximum number of concurrent flows?
     * @param bufferSize what would be the buffer size
     */
    public ObservableConcatEagerConfig {
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
    }
}
