/**
 * Copyright 2014 Netflix, Inc.
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
package rx.exceptions;

import rx.Subscriber;

/**
 * Represents an exception used to re-throw errors thrown from {@link Subscriber#onCompleted()}.
 */
public final class OnCompletedFailedException extends RuntimeException {

    private static final long serialVersionUID = 8622579378868820554L;

    /**
     * Wraps the {@code Throwable} before it is to be re-thrown as an {@code OnCompletedFailedException}.
     *
     * @param throwable
     *          the {@code Throwable} to re-throw; if null, a NullPointerException is constructed
     */
    public OnCompletedFailedException(Throwable throwable) {
        super(throwable != null ? throwable : new NullPointerException());
    }
    
    /**
     * Customizes the {@code Throwable} with a custom message and wraps it before it is to be re-thrown as an
     * {@code OnCompletedFailedException}.
     *
     * @param message
     *          the message to assign to the {@code Throwable} to re-throw
     * @param throwable
     *          the {@code Throwable} to re-throw; if null, a NullPointerException is constructed
     */
    public OnCompletedFailedException(String message, Throwable throwable) {
        super(message, throwable != null ? throwable : new NullPointerException());
    }
}
