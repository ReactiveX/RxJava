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

/**
 * Synchronize execution to be single-threaded.
 * <p>
 * This ONLY does synchronization. It does not involve itself in safety or subscriptions. See SafeSubscriber for that.
 * 
 * @param <T>
 * @deprecated Use SerializedObserver instead as it doesn't block threads during event notification.
 */
@Deprecated
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

    private final Observer<? super T> observer;
    private final Object lock;
    private boolean isTerminated = false;

    public SynchronizedObserver(Observer<? super T> subscriber) {
        this.observer = subscriber;
        this.lock = this;
    }

    public SynchronizedObserver(Observer<? super T> subscriber, Object lock) {
        this.observer = subscriber;
        this.lock = lock;
    }

    public void onNext(T arg) {
        synchronized (lock) {
            if (!isTerminated) {
                observer.onNext(arg);
            }
        }
    }

    public void onError(Throwable e) {
        synchronized (lock) {
            if (!isTerminated) {
                isTerminated = true;
                observer.onError(e);
            }
        }
    }

    public void onCompleted() {
        synchronized (lock) {
            if (!isTerminated) {
                isTerminated = true;
                observer.onCompleted();
            }
        }
    }
}