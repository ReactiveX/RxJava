/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.util.functions;

import rx.Observer;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

/**
 * Utility class for the Action interfaces.
 */
public final class Actions {
    private Actions() { throw new IllegalStateException("No instances!"); }
    /**
     * Extracts a method reference to the observer's onNext method
     * in the form of an Action1.
     * <p>Java 8: observer::onNext</p>
     * @param observer the observer to use
     * @return an action which calls the observer's onNext method.
     */
    public static <T> Action1<T> onNextFrom(final Observer<T> observer) {
        return new Action1<T>() {
            @Override
            public void call(T t1) {
                observer.onNext(t1);
            }
        };
    }
    /**
     * Extracts a method reference to the observer's onError method
     * in the form of an Action1.
     * <p>Java 8: observer::onError</p>
     * @param observer the observer to use
     * @return an action which calls the observer's onError method.
     */
    public static <T> Action1<Throwable> onErrorFrom(final Observer<T> observer) {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable t1) {
                observer.onError(t1);
            }
        };
    }
    /**
     * Extracts a method reference to the observer's onCompleted method
     * in the form of an Action0.
     * <p>Java 8: observer::onCompleted</p>
     * @param observer the observer to use
     * @return an action which calls the observer's onCompleted method.
     */
    public static <T> Action0 onCompletedFrom(final Observer<T> observer) {
        return new Action0() {
            @Override
            public void call() {
                observer.onCompleted();
            }
        };
    }
}
