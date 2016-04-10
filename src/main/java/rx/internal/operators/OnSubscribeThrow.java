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

package rx.internal.operators;

import rx.*;
import rx.Observable.OnSubscribe;

/**
 * An Observable that invokes {@link Observer#onError onError} when the {@link Observer} subscribes to it.
 * 
 * @param <T>
 *            the type of item (ostensibly) emitted by the Observable
 */
public final class OnSubscribeThrow<T> implements OnSubscribe<T> {

    private final Throwable exception;

    public OnSubscribeThrow(Throwable exception) {
        this.exception = exception;
    }

    /**
     * Accepts an {@link Observer} and calls its {@link Observer#onError onError} method.
     * 
     * @param observer
     *            an {@link Observer} of this Observable
     */
    @Override
    public void call(Subscriber<? super T> observer) {
        observer.onError(exception);
    }
}
