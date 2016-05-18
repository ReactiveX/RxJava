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

import rx.annotations.Experimental;
import rx.exceptions.MissingBackpressureException;

/**
 * Generic strategy and default implementations to deal with backpressure buffer overflows.
 */
@Experimental
public final class BackpressureOverflow {

    public interface Strategy {

        /**
         * Whether the Backpressure manager should attempt to drop the oldest item, or simply
         * drop the item currently causing backpressure.
         *
         * @return true to request drop of the oldest item, false to drop the newest.
         * @throws MissingBackpressureException
         */
        boolean mayAttemptDrop() throws MissingBackpressureException;
    }

    public static final BackpressureOverflow.Strategy ON_OVERFLOW_DEFAULT = Error.INSTANCE;

    public static final BackpressureOverflow.Strategy ON_OVERFLOW_ERROR = Error.INSTANCE;

    public static final BackpressureOverflow.Strategy ON_OVERFLOW_DROP_OLDEST = DropOldest.INSTANCE;

    public static final BackpressureOverflow.Strategy ON_OVERFLOW_DROP_LATEST = DropLatest.INSTANCE;

    /**
     * Drop oldest items from the buffer making room for newer ones.
     */
    static class DropOldest implements BackpressureOverflow.Strategy {
        static final DropOldest INSTANCE = new DropOldest();

        private DropOldest() {}

        @Override
        public boolean mayAttemptDrop() {
            return true;
        }
    }

    /**
     * Drop most recent items, but not {@code onError} nor unsubscribe from source
     * (as {code OperatorOnBackpressureDrop}).
     */
    static class DropLatest implements BackpressureOverflow.Strategy {
        static final DropLatest INSTANCE = new DropLatest();

        private DropLatest() {}

        @Override
        public boolean mayAttemptDrop() {
            return false;
        }
    }

    /**
     * {@code onError} a MissingBackpressureException and unsubscribe from source.
     */
    static class Error implements BackpressureOverflow.Strategy {

        static final Error INSTANCE = new Error();

        private Error() {}

        @Override
        public boolean mayAttemptDrop() throws MissingBackpressureException {
            throw new MissingBackpressureException("Overflowed buffer");
        }
    }
}
