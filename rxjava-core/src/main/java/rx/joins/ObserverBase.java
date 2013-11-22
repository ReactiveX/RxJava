/**
 * Copyright 2013 Netflix, Inc.
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
package rx.joins;

import java.util.concurrent.atomic.AtomicBoolean;
import rx.Observer;

/**
 * Implements an observer that ensures proper event delivery
 * semantics to its abstract onXxxxCore methods.
 */
public abstract class ObserverBase<T> implements Observer<T> {
    private final AtomicBoolean completed = new AtomicBoolean();

    @Override
    public void onNext(T args) {
        if (!completed.get()) {
            onNextCore(args);
        }
    }

    @Override
    public void onError(Throwable e) {
        if (completed.compareAndSet(false, true)) {
            onErrorCore(e);
        }
    }

    @Override
    public void onCompleted() {
        if (completed.compareAndSet(false, true)) {
            onCompletedCore();
        }
    }
    /**
     * Implement this method to react to the receival of a new element in the sequence.
     */
    protected abstract void onNextCore(T args);
    /**
     * Implement this method to react to the occurrence of an exception.
     */
    protected abstract void onErrorCore(Throwable e);
    /**
     * Implement this method to react to the end of the sequence.
     */
    protected abstract void onCompletedCore();
    /**
     * Try to trigger the error state.
     * @param t 
     * @return false if already completed
     */
    protected boolean fail(Throwable t) {
        if (completed.compareAndSet(false, true)) {
            onErrorCore(t);
            return true;
        }
        return false;
    }
}
