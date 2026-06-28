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

import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.internal.functions.ObjectHelper;

/**
 * Configuration record for Observable.observeOn() operators.
 * @param delayError should the error propagation be delayed?
 * @param bufferSize the expected number of items to cache from the upstream
 * @since 4.0.0
 */
public record ObservableObserveOnConfig(boolean delayError, int bufferSize) {

    /**
     * The default configuration with no error delays and bufferSize of Observable.bufferSize().
     */
    public static final ObservableObserveOnConfig DEFAULT = new ObservableObserveOnConfig(false);

    /**
     * The default configuration with error delays and bufferSize of Observable.bufferSize().
     */
    public static final ObservableObserveOnConfig DELAY_ERROR = new ObservableObserveOnConfig(true);

    /**
     * Constructs a configuration record.
     * @param delayError should the error propagation be delayed?
     */
    public ObservableObserveOnConfig(boolean delayError) {
        this(delayError, Observable.bufferSize());
    }

    /**
     * Constructs a configuration record.
     * @param bufferSize the expected number of items to cache from the upstream
     */
    public ObservableObserveOnConfig(int bufferSize) {
        this(false, bufferSize);
    }

    /**
     * Constructs a configuration record.
     * @param delayError should the error propagation be delayed?
     * @param bufferSize the expected number of items to cache from the upstream
     */
    public ObservableObserveOnConfig {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
    }
}
