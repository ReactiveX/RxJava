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
 * Configuration record for Maybe.concatEager() operators.
 * @param delayError should the error propagation be delayed?
 * @param maxConcurrency the maximum number of concurrent sources running
 * @param prefetch the number of items to request from each source
 * @since 4.0.0
 */
public record MaybeConcatEagerConfig(boolean delayError, int maxConcurrency, int prefetch) {

    /**
     * The default configuration with no error delays, maxConcurrency and prefetch of Flowable#bufferSize().
     */
    public static final MaybeConcatEagerConfig DEFAULT = new MaybeConcatEagerConfig(false, Flowable.bufferSize());

    /**
     * The default configuration with error delays, maxConcurrency and prefetch of Flowable#bufferSize().
     */
    public static final MaybeConcatEagerConfig DELAY_ERROR = new MaybeConcatEagerConfig(true, Flowable.bufferSize());

    /**
     * Constructs a configuration record.
     * @param delayError should the error propagation be delayed?
     */
    public MaybeConcatEagerConfig(boolean delayError) {
        this(delayError, 2, 2);
    }

    /**
     * Constructs a configuration record.
     * @param maxConcurrency the maximum number of concurrent sources running
     */
    public MaybeConcatEagerConfig(int maxConcurrency) {
        this(false, maxConcurrency, maxConcurrency);
    }

    /**
     * Constructs a configuration record.
     * @param delayError should the error propagation be delayed?
     * @param maxConcurrency the maximum number of concurrent sources running
     */
    public MaybeConcatEagerConfig(boolean delayError, int maxConcurrency) {
        this(delayError, maxConcurrency, maxConcurrency);
    }

    /**
     * Constructs a configuration record.
     * @param delayError should the error propagation be delayed?
     * @param maxConcurrency the maximum number of concurrent sources running
     * @param prefetch the number of items to request from each source
     */
    public MaybeConcatEagerConfig(boolean delayError, int maxConcurrency, int prefetch) {
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        this.delayError = delayError;
        this.maxConcurrency = maxConcurrency;
        this.prefetch = prefetch;
    }
}
