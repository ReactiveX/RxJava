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
 * Used for re-throwing errors thrown from {@link Subscriber#onError(Throwable)}.
 * 
 * https://github.com/Netflix/RxJava/issues/969
 */
public class OnErrorFailedException extends RuntimeException {
    private static final long serialVersionUID = -419289748403337611L;

    public OnErrorFailedException(String message, Throwable e) {
        super(message, e);
    }

    public OnErrorFailedException(Throwable e) {
        super(e.getMessage(), e);
    }
}