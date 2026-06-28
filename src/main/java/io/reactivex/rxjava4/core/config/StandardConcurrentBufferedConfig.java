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
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.internal.functions.ObjectHelper;

/**
 * Standard configuration block with option to delay errors, change max concurrency
 * amounts and buffer/prefetch sizes.
 * <p>
 * The configuration record combines the conventional binary error handling mode and the trinary
 * error handling modes. Use the {@link #StandardConcurrentBufferedConfig(boolean)} constructors to create
 * those binary cases with this record.
 * TODO once value classes are available, make this a record class.
 * @param errorMode how to handle when errors appear from the inner or outer sources
 * @param maxConcurrency the maximum number of concurrent flows?
 * @param bufferSize the expected number of items to buffer or prefetch from the various sources
 * @since 4.0.0
 */
public record StandardConcurrentBufferedConfig(@NonNull ErrorMode errorMode, int maxConcurrency, int bufferSize) {

    /**
     * The default configuration with no error delay and Flowable#bufferSize() as the maximum concurrency and buffer size setting.
     */
    public static final StandardConcurrentBufferedConfig DEFAULT = new StandardConcurrentBufferedConfig(false);

    /**
     * The default configuration with error delay and Flowable#bufferSize() as the maximum concurrency and buffer size setting.
     */
    public static final StandardConcurrentBufferedConfig DELAY_ERRORS = new StandardConcurrentBufferedConfig(true);

    /**
     * The default configuration with error delay at the boundary and Flowable#bufferSize() as the maximum concurrency and buffer size setting.
     */
    public static final StandardConcurrentBufferedConfig DELAY_ERRORS_BOUNDARY = new StandardConcurrentBufferedConfig(ErrorMode.BOUNDARY);

    /**
     * The default configuration with no error delay, MAX_VALUE for concurrency and Flowable#bufferSize() as the maximum concurrency setting.
     */
    public static final StandardConcurrentBufferedConfig MAX_DEFAULT = new StandardConcurrentBufferedConfig(false, Integer.MAX_VALUE, Flowable.bufferSize());

    /**
     * The default configuration with error delay, MAX_VALUE for concurrency and Flowable#bufferSize() as the maximum concurrency setting.
     */
    public static final StandardConcurrentBufferedConfig MAX_DELAY_ERRORS = new StandardConcurrentBufferedConfig(true, Integer.MAX_VALUE, Flowable.bufferSize());

    /**
     * The default configuration with error delay at the boundary, MAX_VALUE for concurrency and Flowable#bufferSize() as the maximum concurrency setting.
     */
    public static final StandardConcurrentBufferedConfig MAX_DELAY_ERRORS_BOUNDARY =
            new StandardConcurrentBufferedConfig(ErrorMode.BOUNDARY, Integer.MAX_VALUE, Flowable.bufferSize());

    /**
     * Optionally delay error, {@link Flowable#bufferSize()} sizes
     * @param delayErrors should the error be delayed?
     */
    public StandardConcurrentBufferedConfig(boolean delayErrors) {
        this(delayErrors, Flowable.bufferSize(), Flowable.bufferSize());
    }

    /**
     * Optionally delay error, {@link Flowable#bufferSize()} sizes
     * @param errorMode how to handle when errors appear from the inner or outer sources
     */
    public StandardConcurrentBufferedConfig(ErrorMode errorMode) {
        this(errorMode, Flowable.bufferSize(), Flowable.bufferSize());
    }

    /**
     * Optionally set the buffer size, no delay errors.
     * @param maxConcurrency the maximum number of concurrent flows
     */
    public StandardConcurrentBufferedConfig(int maxConcurrency) {
        this(false, maxConcurrency, Flowable.bufferSize());
    }

    /**
     * Optionally delays errors and sets the buffer size too.
     * @param delayErrors should the errors be delayed?
     * @param maxConcurrency the maximum number of concurrent flows
     */
    public StandardConcurrentBufferedConfig(boolean delayErrors, int maxConcurrency) {
        this(delayErrors, maxConcurrency, Flowable.bufferSize());
    }

    /**
     * Optionally delay error, {@link Flowable#bufferSize()} sizes
     * @param errorMode how to handle when errors appear from the inner or outer sources
     * @param maxConcurrency the maximum number of concurrent flows
     */
    public StandardConcurrentBufferedConfig(ErrorMode errorMode, int maxConcurrency) {
        this(errorMode, maxConcurrency, Flowable.bufferSize());
    }

    /**
     * Optionally delay error, {@link Flowable#bufferSize()} sizes
     * @param delayErrors should the error be delayed?
     * @param maxConcurrency the maximum number of concurrent flows
     * @param bufferSize what would be the buffer size
     */
    public StandardConcurrentBufferedConfig(boolean delayErrors, int maxConcurrency, int bufferSize) {
        this(delayErrors ? ErrorMode.END : ErrorMode.IMMEDIATE, maxConcurrency, bufferSize);
    }

    /**
     * Fully customize the configuration.
     * @param errorMode how to handle when errors appear from the inner or outer sources
     * @param maxConcurrency the maximum number of concurrent flows?
     * @param bufferSize what would be the buffer size
     */
    public StandardConcurrentBufferedConfig {
        Objects.requireNonNull(errorMode, "errorMode is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
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
