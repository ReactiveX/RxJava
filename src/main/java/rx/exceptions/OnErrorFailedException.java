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
 * Represents an exception used to re-throw errors thrown from {@link Subscriber#onError(Throwable)}.
 * <p>
 * @see <a href="https://github.com/ReactiveX/RxJava/issues/969">RxJava issue #969</a>
 */
public class OnErrorFailedException extends RuntimeException {
    private static final long serialVersionUID = -419289748403337611L;

    /**
     * Customizes the {@code Throwable} with a custom message and wraps it before it is to be re-thrown as an
     * {@code OnErrorFailedException}.
     *
     * @param message
     *          the message to assign to the {@code Throwable} to re-throw
     * @param e
     *          the {@code Throwable} to re-throw; if null, a NullPointerException is constructed
     */
    public OnErrorFailedException(String message, Throwable e) {
        super(message, e != null ? e : new NullPointerException());
    }

    /**
     * Wraps the {@code Throwable} before it is to be re-thrown as an {@code OnErrorFailedException}.
     *
     * @param e
     *          the {@code Throwable} to re-throw; if null, a NullPointerException is constructed
     */
    public OnErrorFailedException(Throwable e) {
        super(e != null ? e.getMessage() : null, e != null ? e : new NullPointerException());
    }
}
