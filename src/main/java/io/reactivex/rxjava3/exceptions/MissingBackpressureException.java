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

package io.reactivex.rxjava3.exceptions;

/**
 * Indicates that an operator attempted to emit a value but the downstream wasn't ready for it.
 */
public final class MissingBackpressureException extends RuntimeException {

    private static final long serialVersionUID = 8517344746016032542L;

    /**
     * The default error message.
     * <p>
     * This can happen if the downstream doesn't call {@link org.reactivestreams.Subscription#request(long)}
     * in time or at all.
     * @since 3.1.6
     */
    public static final String DEFAULT_MESSAGE = "Could not emit value due to lack of requests";

    /**
     * Constructs a MissingBackpressureException without message or cause.
     */
    public MissingBackpressureException() {
        // no message
    }

    /**
     * Constructs a MissingBackpressureException with the given message but no cause.
     * @param message the error message
     */
    public MissingBackpressureException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code MissingBackpressureException} with the
     * default message {@value #DEFAULT_MESSAGE}.
     * @return the new {@code MissingBackpressureException} instance.
     * @since 3.1.6
     */
    public static MissingBackpressureException createDefault() {
        return new MissingBackpressureException(DEFAULT_MESSAGE);
    }
}
