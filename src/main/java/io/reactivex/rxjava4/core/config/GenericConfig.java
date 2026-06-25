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
 * @param bufferSize what would be the buffer size?
 * @param prefetch how many upstream items to request upfront, then 75% of it later?
 * @since 4.0.0
 */
public record GenericConfig(boolean delayErrors, int bufferSize, int prefetch) {

    /**
     * The default config with no error delay and Flowable#bufferSize() as the maximum concurrency setting.
     */
    public static final GenericConfig DEFAULT = new GenericConfig(false, Flowable.bufferSize());

    /**
     * The default config with error delay and Flowable#bufferSize() as the maximum concurrency setting.
     */
    public static final GenericConfig DELAY_ERRORS = new GenericConfig(true, Flowable.bufferSize());

    /**
     * Optionally delay error, {@link Flowable#bufferSize()} sizes.
     * @param delayError should the error be delayed?
     */
    public GenericConfig(boolean delayError) {
        this(delayError, Flowable.bufferSize(), Flowable.bufferSize());
    }

    /**
     * Optionally set the buffer size, no delay errors.
     * @param bufferSize the prefetch and the buffer size
     */
    public GenericConfig(int bufferSize) {
        this(false, ObjectHelper.verifyPositive(bufferSize, "bufferSize"), Flowable.bufferSize());
    }

    /**
     * Optionally delays errors and sets the buffer size too.
     * @param delayError should the errors be delayed?
     * @param bufferSize the prefetch and the buffer size
     */
    public GenericConfig(boolean delayError, int bufferSize) {
        this(delayError, ObjectHelper.verifyPositive(bufferSize, "bufferSize"), Flowable.bufferSize());
    }

    /**
     * Fully customize the configuration.
     * @param delayErrors should the errors be delayed
     * @param bufferSize what would be the buffer size
     * @param prefetch what would be the prefetch amount
     */
    public GenericConfig {
        ObjectHelper.verifyPositive(bufferSize, "bufferSize");
        ObjectHelper.verifyPositive(prefetch, "prefetch");
    }
}
