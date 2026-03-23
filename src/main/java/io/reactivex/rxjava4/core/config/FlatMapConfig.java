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

import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.internal.functions.ObjectHelper;

/**
 * Generic configuration block with option to delay errors, change prefetch
 * amounts and buffer sizes.
 * TODO once value classes are available, make this a record class.
 * @param delayErrors should the error be delayed?
 * @param maxConcurrency the maximum number of concurrent flows?
 * @param bufferSize what would be the buffer size?
 * @since 4.0.0
 */
public record FlatMapConfig(boolean delayErrors, int maxConcurrency, int bufferSize) {

    /**
     * Default config: no error delay, {@link Flowable#bufferSize()} sizes.
     */
    public FlatMapConfig() {
        this(false, Flowable.bufferSize(), Flowable.bufferSize());
    }

    /**
     * Optionally delay error, {@link Flowable#bufferSize()} sizes
     * @param delayErrors should the error be delayed?
     */
    public FlatMapConfig(boolean delayErrors) {
        this(delayErrors, Flowable.bufferSize(), Flowable.bufferSize());
    }

    /**
     * Optionally set the buffer size, no delay errors.
     * @param maxConcurrency the maximum number of concurrent flows
     */
    public FlatMapConfig(int maxConcurrency) {
        this(false,
                ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency"),
                Flowable.bufferSize());
    }

    /**
     * Optionally delays errors and sets the buffer size too.
     * @param delayErrors should the errors be delayed?
     * @param maxConcurrency the maximum number of concurrent flows
     */
    public FlatMapConfig(boolean delayErrors, int maxConcurrency) {
        this(delayErrors,
                ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency"),
                Flowable.bufferSize());
    }

    /**
     * Fully customize the configuration.
     * @param delayErrors should the errors be delayed
     * @param maxConcurrency the maximum number of concurrent flows?
     * @param bufferSize what would be the buffer size
     */
    public FlatMapConfig(boolean delayErrors, int maxConcurrency, int bufferSize) {
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        this.delayErrors = delayErrors;
        this.maxConcurrency = maxConcurrency;
        this.bufferSize = bufferSize;
    }
}
