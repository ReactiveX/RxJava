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
 * Configuration record for Single.concat() operators.
 * @param delayError should the error propagation be delayed?
 * @param prefetch the number of source sequences to request from a backpressured source
 * @since 4.0.0
 */
public record SingleConcatConfig(boolean delayError, int prefetch) {

    /**
     * The default configuration with no error delays and prefetch of 2.
     */
    public static final SingleConcatConfig DEFAULT = new SingleConcatConfig(false, 2);

    /**
     * The default configuration with error delays and prefetch of 2.
     */
    public static final SingleConcatConfig DELAY_ERROR = new SingleConcatConfig(true, 2);

    /**
     * Constructs a configuration record.
     * @param delayError should the error propagation be delayed?
     */
    public SingleConcatConfig(boolean delayError) {
        this(delayError, 2);
    }

    /**
     * Constructs a configuration record.
     * @param prefetch the number of source sequences to request from a backpressured source
     */
    public SingleConcatConfig(int prefetch) {
        this(false, prefetch);
    }

    /**
     * Constructs a configuration record.
     * @param delayError should the error propagation be delayed?
     * @param prefetch the number of source sequences to request from a backpressured source
     */
    public SingleConcatConfig(boolean delayError, int prefetch) {
        ObjectHelper.verifyPositive(prefetch, "prefetch");
        this.delayError = delayError;
        this.prefetch = prefetch;
    }
}
