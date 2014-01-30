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
package rx.observers;

import rx.Observer;
import rx.Subscriber;
import rx.operators.SafeObservableSubscription;

/**
 * A thread-safe Observer for transitioning states in operators.
 * <p>
 * Execution rules are:
 * <ul>
 * <li>Allow only single-threaded, synchronous, ordered execution of onNext, onCompleted, onError</li>
 * <li>Once an onComplete or onError are performed, no further calls can be executed</li>
 * <li>If unsubscribe is called, this means we call completed() and don't allow any further onNext calls.</li>
 * </ul>
 * 
 * @param <T>
 */
public final class SynchronizedObserver<T> implements Observer<T> {

    /**
     * Intrinsic synchronized locking with double-check short-circuiting was chosen after testing several other implementations.
     * 
     * The code and results can be found here:
     * - https://github.com/benjchristensen/JavaLockPerformanceTests/tree/master/results/Observer
     * - https://github.com/benjchristensen/JavaLockPerformanceTests/tree/master/src/com/benjchristensen/performance/locks/Observer
     * 
     * The major characteristic that made me choose synchronized instead of Reentrant or a customer AbstractQueueSynchronizer implementation
     * is that intrinsic locking performed better when nested, and AtomicObserver will end up nested most of the time since Rx is
     * compositional by its very nature.
     * 
     * // TODO composing of this class should rarely happen now with updated design so this decision should be revisited
     */

    private final Subscriber<? super T> observer;
    private final SafeObservableSubscription subscription;
    private volatile boolean finishRequested = false;
    private volatile boolean finished = false;
    private volatile Object lock;

    public SynchronizedObserver(Subscriber<? super T> subscriber, SafeObservableSubscription subscription) {
        this.observer = subscriber;
        this.subscription = subscription;
        this.lock = this;
    }

    public SynchronizedObserver(Subscriber<? super T> subscriber, SafeObservableSubscription subscription, Object lock) {
        this.observer = subscriber;
        this.subscription = subscription;
        this.lock = lock;
    }
    
    public SynchronizedObserver(Subscriber<? super T> subscriber, Object lock) {
        this.observer = subscriber;
        this.subscription = new SafeObservableSubscription(subscriber);
        this.lock = lock;
    }

    /**
     * Used when synchronizing an Observer without access to the subscription.
     * 
     * @param Observer
     */
    public SynchronizedObserver(Subscriber<? super T> subscriber) {
        this(subscriber, new SafeObservableSubscription());
    }

    public void onNext(T arg) {
        if (finished || finishRequested || subscription.isUnsubscribed()) {
            // if we're already stopped, or a finish request has been received, we won't allow further onNext requests
            return;
        }
        synchronized (lock) {
            // check again since this could have changed while waiting
            if (finished || finishRequested || subscription.isUnsubscribed()) {
                // if we're already stopped, or a finish request has been received, we won't allow further onNext requests
                return;
            }
            observer.onNext(arg);
        }
    }

    public void onError(Throwable e) {
        if (finished || subscription.isUnsubscribed()) {
            // another thread has already finished us, so we won't proceed
            return;
        }
        finishRequested = true;
        synchronized (lock) {
            // check again since this could have changed while waiting
            if (finished || subscription.isUnsubscribed()) {
                return;
            }
            observer.onError(e);
            finished = true;
        }
    }

    public void onCompleted() {
        if (finished || subscription.isUnsubscribed()) {
            // another thread has already finished us, so we won't proceed
            return;
        }
        finishRequested = true;
        synchronized (lock) {
            // check again since this could have changed while waiting
            if (finished || subscription.isUnsubscribed()) {
                return;
            }
            observer.onCompleted();
            finished = true;
        }
    }
}