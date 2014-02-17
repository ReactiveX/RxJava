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
 * Used for re-throwing {@link Subscriber#onError(Throwable)} when an implementation doesn't exist.
 * 
 * https://github.com/Netflix/RxJava/issues/198
 * 
 * Rx Design Guidelines 5.2
 * 
 * "when calling the Subscribe method that only has an onNext argument, the OnError behavior will be
 * to rethrow the exception on the thread that the message comes out from the observable sequence.
 * The OnCompleted behavior in this case is to do nothing."
 */
public class OnErrorNotImplementedException extends RuntimeException {
    private static final long serialVersionUID = -6298857009889503852L;

    public OnErrorNotImplementedException(String message, Throwable e) {
        super(message, e);
    }

    public OnErrorNotImplementedException(Throwable e) {
        super(e.getMessage(), e);
    }
}