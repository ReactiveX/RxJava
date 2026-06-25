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
import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.internal.functions.ObjectHelper;

/**
 * Configuration record for Observable.merge() operators.
 * @param delayErrors should the error be delayed?
 * @param maxConcurrency the maximum number of concurrent flows?
 * @param bufferSize what would be the buffer size?
 * @since 4.0.0
 */
public record ObservableMergeConfig(boolean delayErrors, int maxConcurrency, int bufferSize) {

    /**
     * The default configuration with no error delays and bufferSize of {@link Observable#bufferSize()}.
     */
    public static final ObservableMergeConfig DEFAULT = new ObservableMergeConfig(false, Observable.bufferSize());

    /**
     * The default configuration with error delays and bufferSize of {@link Observable#bufferSize()}.
     */
    public static final ObservableMergeConfig DELAY_ERROR = new ObservableMergeConfig(true, Observable.bufferSize());

    /**
     * Optionally delay error, {@link Flowable#bufferSize()} sizes
     * @param delayErrors should the error be delayed?
     */
    public ObservableMergeConfig(boolean delayErrors) {
        this(delayErrors, Flowable.bufferSize(), Flowable.bufferSize());
    }

    /**
     * Optionally set the buffer size, no delay errors.
     * @param maxConcurrency the maximum number of concurrent flows
     */
    public ObservableMergeConfig(int maxConcurrency) {
        this(false, maxConcurrency, Flowable.bufferSize());
    }

    /**
     * Optionally delays errors and sets the buffer size too.
     * @param delayErrors should the errors be delayed?
     * @param maxConcurrency the maximum number of concurrent flows
     */
    public ObservableMergeConfig(boolean delayErrors, int maxConcurrency) {
        this(delayErrors, maxConcurrency, Flowable.bufferSize());
    }

    /**
     * Fully customize the configuration.
     * @param delayErrors should the errors be delayed
     * @param maxConcurrency the maximum number of concurrent flows?
     * @param bufferSize what would be the buffer size
     */
    public ObservableMergeConfig {
        ObjectHelper.verifyPositive(maxConcurrency, "maxConcurrency");
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
    }
}
