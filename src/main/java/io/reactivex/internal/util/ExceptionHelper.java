/**
 * Copyright 2016 Netflix, Inc.
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

import io.reactivex.exceptions.CompositeException;

/**
 * Terminal atomics for Throwable containers.
 */
public enum ExceptionHelper {
    ;
    /**
     * A singleton instance of a Throwable indicating a terminal state for exceptions,
     * don't leak this!
     */
    public static final Throwable TERMINATED = new Throwable("No further exceptions");
    
    public static <T> boolean addThrowable(AtomicReference<Throwable> field, Throwable exception) {
        for (;;) {
            Throwable current = field.get();
            
            if (current == TERMINATED) {
                return false;
            }
            
            Throwable update;
            if (current == null) {
                update = exception;
            } else {
                update = new CompositeException(current, exception);
            }
            
            if (field.compareAndSet(current, update)) {
                return true;
            }
        }
    }
    
    public static <T> Throwable terminate(AtomicReference<Throwable> field) {
        Throwable current = field.get();
        if (current != TERMINATED) {
            current = field.getAndSet(TERMINATED);
        }
        return current;
    }
}
