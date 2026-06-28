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
 * Standard configuration block with option to delay errors and change max concurrency
 * amounts.
 * <p>
 * The configuration record combines the conventional binary error handling mode and the trinary
 * error handling modes. Use the {@link #StandardConcurrentConfig(boolean)} constructors to create
 * those binary cases with this record.
 * TODO once value classes are available, make this a record class.
 * @param errorMode how to handle when errors appear from the inner or outer sources
 * @param maxConcurrency the maximum number of concurrent flows?
 * @since 4.0.0
 */
public record StandardConcurrentConfig(@NonNull ErrorMode errorMode, int maxConcurrency) {

    /**
     * The default configuration with no error delay and Flowable#bufferSize() as the maximum concurrency and buffer size setting.
     */
    public static final StandardConcurrentConfig DEFAULT = new StandardConcurrentConfig(false);

    /**
     * The default configuration with error delay and Flowable#bufferSize() as the maximum concurrency and buffer size setting.
     */
    public static final StandardConcurrentConfig DELAY_ERRORS = new StandardConcurrentConfig(true);

    /**
     * The default configuration with error delay at the boundary and Flowable#bufferSize() as the maximum concurrency and buffer size setting.
     */
    public static final StandardConcurrentConfig DELAY_ERRORS_BOUNDARY = new StandardConcurrentConfig(true);

    /**
     * The default configuration with no error delay, MAX_VALUE for concurrency and Flowable#bufferSize() as the maximum concurrency setting.
     */
    public static final StandardConcurrentConfig MAX_DEFAULT = new StandardConcurrentConfig(false, Integer.MAX_VALUE);

    /**
     * The default configuration with error delay, MAX_VALUE for concurrency and Flowable#bufferSize() as the maximum concurrency setting.
     */
    public static final StandardConcurrentConfig MAX_DELAY_ERRORS = new StandardConcurrentConfig(true, Integer.MAX_VALUE);

    /**
     * The default configuration with error delay at the boundary, MAX_VALUE for concurrency and Flowable#bufferSize() as the maximum concurrency setting.
     */
    public static final StandardConcurrentConfig MAX_DELAY_ERRORS_BOUNDARY = new StandardConcurrentConfig(true, Integer.MAX_VALUE);

    /**
     * Optionally delay error, {@link Flowable#bufferSize()} sizes
     * @param delayErrors should the error be delayed?
     */
    public StandardConcurrentConfig(boolean delayErrors) {
        this(delayErrors, Flowable.bufferSize());
    }

    /**
     * Optionally delay error, {@link Flowable#bufferSize()} sizes
     * @param errorMode how to handle when errors appear from the inner or outer sources
     */
    public StandardConcurrentConfig(ErrorMode errorMode) {
        this(errorMode, Flowable.bufferSize());
    }

    /**
     * Optionally set the buffer size, no delay errors.
     * @param maxConcurrency the maximum number of concurrent flows
     */
    public StandardConcurrentConfig(int maxConcurrency) {
        this(false, maxConcurrency);
    }

    /**
     * Optionally delays errors and sets the buffer size too.
     * @param delayErrors should the errors be delayed?
     * @param maxConcurrency the maximum number of concurrent flows
     */
    public StandardConcurrentConfig(boolean delayErrors, int maxConcurrency) {
        this(delayErrors ? ErrorMode.END : ErrorMode.IMMEDIATE, maxConcurrency);
    }

    /**
     * Fully customize the configuration.
     * @param errorMode how to handle when errors appear from the inner or outer sources
     * @param maxConcurrency the maximum number of concurrent flows?
     */
    public StandardConcurrentConfig {
        Objects.requireNonNull(errorMode, "errorMode is null");
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
    }

    /**
     * Returns true if this config is set to a delayed error handling mode, such as BOUNDARY or END
     * @return true if this config is set to a delayed error handling mode,
     */
    public boolean delayErrors() {
        return errorMode != ErrorMode.IMMEDIATE;
    }

    /**
     * Converts this config into the buffered version with the default buffer size offered by it.
     * @return the new {@code StandardConcurrentBufferConfig} instance
     */
    public StandardConcurrentBufferedConfig toBuffered() {
        return new StandardConcurrentBufferedConfig(errorMode, maxConcurrency);
    }

    /**
     * Converts this config into the buffered version with the given buffer size offered by it.
     * @param bufferSize the expected number of items to buffer or prefetch from the various sources
     * @return the new {@code StandardConcurrentBufferConfig} instance
     */
    public StandardConcurrentBufferedConfig toBuffered(int bufferSize) {
        return new StandardConcurrentBufferedConfig(errorMode, maxConcurrency, bufferSize);
    }
}
