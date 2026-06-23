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

import io.reactivex.rxjava4.internal.functions.ObjectHelper;

/**
 * Configuration record for Single.merge() operators.
 * @param delayErrors should the error propagation be delayed?
 * @param maxConcurrency the number of source sequences run concurrently
 * @since 4.0.0
 */
public record SingleMergeConfig(boolean delayErrors, int maxConcurrency) {

    /**
     * The default config with no error delay and Integer#MAX_VALUE as the maximum concurrency setting.
     */
    public static final SingleMergeConfig DEFAULT = new SingleMergeConfig(false, Integer.MAX_VALUE);

    /**
     * The default config with error delay and Integer#MAX_VALUE as the maximum concurrency setting.
     */
    public static final SingleMergeConfig DELAY_ERRORS = new SingleMergeConfig(true, Integer.MAX_VALUE);

    /**
     * Constructs a configuration record.
     * @param delayErrors should the error propagation be delayed?
     */
    public SingleMergeConfig(boolean delayErrors) {
        this(delayErrors, Integer.MAX_VALUE);
    }

    /**
     * Constructs a configuration record.
     * @param maxConcurrency the number of source sequences run concurrently
     */
    public SingleMergeConfig(int maxConcurrency) {
        this(false, maxConcurrency);
    }

    /**
     * Constructs a configuration record.
     * @param delayErrors should the error propagation be delayed?
     * @param maxConcurrency the number of source sequences run concurrently
     */
    public SingleMergeConfig(boolean delayErrors, int maxConcurrency) {
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        this.delayErrors = delayErrors;
        this.maxConcurrency = maxConcurrency;
    }
}
