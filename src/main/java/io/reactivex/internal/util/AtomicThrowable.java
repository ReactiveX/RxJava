/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.util;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic container for Throwables including combining and having a
 * terminal state via ExceptionHelper.
 * <p>
 * Watch out for the leaked AtomicReference methods!
 */
public final class AtomicThrowable extends AtomicReference<Throwable> {


    private static final long serialVersionUID = 3949248817947090603L;

    /**
     * Atomically adds a Throwable to this container (combining with a previous Throwable is necessary).
     * @param t the throwable to add
     * @return true if successful, false if the container has been terminated
     */
    public boolean addThrowable(Throwable t) {
        return ExceptionHelper.addThrowable(this, t);
    }

    /**
     * Atomically terminate the container and return the contents of the last
     * non-terminal Throwable of it.
     * @return the last Throwable
     */
    public Throwable terminate() {
        return ExceptionHelper.terminate(this);
    }

    public boolean isTerminated() {
        return get() == ExceptionHelper.TERMINATED;
    }
}
