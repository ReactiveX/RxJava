/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.observables.operations;

import javax.annotation.concurrent.ThreadSafe;

import rx.observables.Observer;

/**
 * A thread-safe Observer for transitioning states in operators.
 * <p>
 * Allows both single-threaded and multi-threaded execution controlled by the following FastProperty:
 * <li>reactive.Observer.multithreaded.enabled [Default: false]</li>
 * <p>
 * Single-threaded Execution rules are:
 * <ul>
 * <li>Allow only single-threaded, synchronous, ordered execution of onNext, onCompleted, onError</li>
 * <li>Once an onComplete or onError are performed, no further calls can be executed</li>
 * <li>If unsubscribe is called, this means we call completed() and don't allow any further onNext calls.</li>
 * </ul>
 * <p>
 * Multi-threaded Execution rules are:
 * <ul>
 * <li>Allows multiple threads to perform onNext concurrently</li>
 * <li>When an onComplete, onError or unsubscribe request is received, block until all current onNext calls are completed</li>
 * <li>When an unsubscribe is received, block until all current onNext are completed</li>
 * <li>Once an onComplete or onError are performed, no further calls can be executed</li>
 * <li>If unsubscribe is called, this means we call completed() and don't allow any further onNext calls.</li>
 * </ul>
 * 
 * @param <T>
 */
@ThreadSafe
/* package */final class AtomicObserver<T> implements Observer<T> {

    /** Allow changing between forcing single or allowing multi-threaded execution of onNext */
    private static boolean allowMultiThreaded = true;
    static {
        String v = System.getProperty("rx.onNext.multithreaded.enabled");
        if (v != null) {
            // if we have a property set then we'll use it
            allowMultiThreaded = Boolean.parseBoolean(v);
        }
    }

    private final Observer<T> Observer;

    public AtomicObserver(Observer<T> Observer, AtomicObservableSubscription subscription) {
        if (allowMultiThreaded) {
            this.Observer = new AtomicObserverMultiThreaded<T>(Observer, subscription);
        } else {
            this.Observer = new AtomicObserverSingleThreaded<T>(Observer, subscription);
        }
    }

    @Override
    public void onCompleted() {
        Observer.onCompleted();
    }

    @Override
    public void onError(Exception e) {
        Observer.onError(e);
    }

    @Override
    public void onNext(T args) {
        Observer.onNext(args);
    }

}
