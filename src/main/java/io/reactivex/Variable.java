/**
 * Copyright 2016 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex;

import io.reactivex.internal.functions.Objects;
import io.reactivex.subjects.BehaviorSubject;

/**
 * An observable mutable value.
 * <p>
 * Thread safety: this implementation is thread-safe, all modification and notification is synchronized.c
 *
 * @param <T> the value type
 */
public class Variable<T> implements Value<T> {

    protected final BehaviorSubject<T> behaviorSubject;

    /**
     * Create uninitialized variable
     */
    public Variable() {
        behaviorSubject = BehaviorSubject.create();
    }

    /**
     * Create initialized variable
     *
     * @param value variable value
     */
    public Variable(final T value) {
        behaviorSubject = BehaviorSubject.createDefault(value);
    }

    @Override
    public T getValue() {
        if (!behaviorSubject.hasValue()) {
            synchronized (behaviorSubject) {
                while (!hasValue()) {
                    try {
                        behaviorSubject.wait();
                    } catch (InterruptedException e) {
                        throw new IllegalStateException("Value not initialized yet", e);
                    }
                }
            }
        }
        return behaviorSubject.getValue();
    }

    /**
     * Set current value
     *
     * @param value current non null value
     */
    public void setValue(final T value) {
        Objects.requireNonNull(value, "Value cannot be null");
        synchronized (behaviorSubject) {
            behaviorSubject.onNext(value);
            behaviorSubject.notifyAll();
        }
    }

    @Override
    public boolean hasValue() {
        return behaviorSubject.hasValue();
    }

    @Override
    public Observable<T> asObservable() {
        return behaviorSubject;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        behaviorSubject.onComplete();
    }
}
