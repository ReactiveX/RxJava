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
import io.reactivex.rxjava4.internal.functions.ObjectHelper;
import io.reactivex.rxjava4.core.ErrorMode;

/**
 * Configuration record for operators which have three error handling modes and a buffer size or prefetch like parameter.
 * @param errorMode how to handle when errors appear from the inner or outer sources
 * @param bufferSize the expected number of items to buffer or prefetch from the various sources
 * @since 4.0.0
 */
public record StandardBufferedConfig(@NonNull ErrorMode errorMode, int bufferSize) {

    /**
     * The default configuration with no error delays and bufferSize of {@link Observable#bufferSize()}.
     */
    public static final StandardBufferedConfig DEFAULT = new StandardBufferedConfig(ErrorMode.IMMEDIATE);

    /**
     * The default configuration with error delays till the end and bufferSize of {@link Observable#bufferSize()}.
     */
    public static final StandardBufferedConfig DELAY_ERRORS = new StandardBufferedConfig(ErrorMode.END);

    /**
     * The default configuration with error delays till the boundary and bufferSize of {@link Observable#bufferSize()}.
     */
    public static final StandardBufferedConfig DELAY_ERRORS_BOUNDARY = new StandardBufferedConfig(ErrorMode.BOUNDARY);

    /**
     * The default config with no error delay and 2 as the maximum buffer size / prefetch amount setting.
     */
    public static final StandardBufferedConfig MIN_DEFAULT = new StandardBufferedConfig(ErrorMode.IMMEDIATE, 2);

    /**
     * The default config with error delay and 2 as the maximum buffer size / prefetch amount setting.
     */
    public static final StandardBufferedConfig MIN_DELAY_ERRORS = new StandardBufferedConfig(ErrorMode.END, 2);

    /**
     * The default config with error delay till the boundary and 2 as the maximum buffer size / prefetch amount setting.
     */
    public static final StandardBufferedConfig MIN_DELAY_ERRORS_BOUNDARY = new StandardBufferedConfig(ErrorMode.BOUNDARY, 2);

    /**
     * Constructs a configuration record.
     * @param errorMode how to handle when errors appear from the inner or outer sources
     */
    public StandardBufferedConfig(@NonNull ErrorMode errorMode) {
        this(errorMode, Observable.bufferSize());
    }

    /**
     * Constructs a configuration record with convenience for the basic no-delay/delay error management.
     * @param delayErrors if true, ErrorMode.END is used, ErrorMode.IMMEDIATE otherwise
     */
    public StandardBufferedConfig(boolean delayErrors) {
        this(delayErrors ? ErrorMode.END : ErrorMode.IMMEDIATE);
    }

    /**
     * Constructs a configuration record.
     * @param bufferSize the expected number of outer sources to buffer while processing an inner source
     */
    public StandardBufferedConfig(int bufferSize) {
        this(ErrorMode.IMMEDIATE, bufferSize);
    }

    /**
     * Constructs a configuration record.
     * @param delayErrors if true, ErrorMode.END is used, ErrorMode.IMMEDIATE otherwise
     * @param bufferSize the expected number of outer sources to buffer while processing an inner source
     */
    public StandardBufferedConfig(boolean delayErrors, int bufferSize) {
        this(delayErrors ? ErrorMode.END : ErrorMode.IMMEDIATE, bufferSize);
    }

    /**
     * Constructs a configuration record.
     * @param errorMode how to handle when errors appear from the inner or outer sources
     * @param bufferSize the expected number of outer sources to buffer while processing an inner source
     */
    public StandardBufferedConfig {
        Objects.requireNonNull(errorMode, "errorMode is null");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
    }

    /**
     * Returns true if this config is set to a delayed error handling mode, such as BOUNDARY or END
     * @return true if this config is set to a delayed error handling mode,
     */
    public boolean delayErrors() {
        return errorMode != ErrorMode.IMMEDIATE;
    }
}
