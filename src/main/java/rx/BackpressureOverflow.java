/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx;

import rx.annotations.Beta;
import rx.exceptions.MissingBackpressureException;

/**
 * Generic strategy and default implementations to deal with backpressure buffer overflows.
 * 
 * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
 */
@Beta
public final class BackpressureOverflow {

    private BackpressureOverflow() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Signal a MissingBackressureException due to lack of requests.
     */
    public static final BackpressureOverflow.Strategy ON_OVERFLOW_ERROR = Error.INSTANCE;

    /**
     * By default, signal a MissingBackressureException due to lack of requests.
     */
    public static final BackpressureOverflow.Strategy ON_OVERFLOW_DEFAULT = ON_OVERFLOW_ERROR;

    /**
     * Drop the oldest value in the buffer.
     */
    public static final BackpressureOverflow.Strategy ON_OVERFLOW_DROP_OLDEST = DropOldest.INSTANCE;

    /**
     * Drop the latest value.
     */
    public static final BackpressureOverflow.Strategy ON_OVERFLOW_DROP_LATEST = DropLatest.INSTANCE;

    /**
     * Represents a callback called when a value is about to be dropped
     * due to lack of downstream requests.
     */
    public interface Strategy {

        /**
         * Whether the Backpressure manager should attempt to drop the oldest item, or simply
         * drop the item currently causing backpressure.
         *
         * @return true to request drop of the oldest item, false to drop the newest.
         * @throws MissingBackpressureException if the strategy should signal MissingBackpressureException
         */
        boolean mayAttemptDrop() throws MissingBackpressureException;
    }

    /**
     * Drop oldest items from the buffer making room for newer ones.
     */
    static final class DropOldest implements BackpressureOverflow.Strategy {
        static final DropOldest INSTANCE = new DropOldest();

        private DropOldest() { }

        @Override
        public boolean mayAttemptDrop() {
            return true;
        }
    }

    /**
     * Drop most recent items, but not {@code onError} nor unsubscribe from source
     * (as {code OperatorOnBackpressureDrop}).
     */
    static final class DropLatest implements BackpressureOverflow.Strategy {
        static final DropLatest INSTANCE = new DropLatest();

        private DropLatest() { }

        @Override
        public boolean mayAttemptDrop() {
            return false;
        }
    }

    /**
     * {@code onError} a MissingBackpressureException and unsubscribe from source.
     */
    static final class Error implements BackpressureOverflow.Strategy {

        static final Error INSTANCE = new Error();

        private Error() { }

        @Override
        public boolean mayAttemptDrop() throws MissingBackpressureException {
            throw new MissingBackpressureException("Overflowed buffer");
        }
    }
}
