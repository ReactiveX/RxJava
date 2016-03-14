/**
 * Copyright 2016 Netflix, Inc.
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

package rx.internal.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import rx.exceptions.CompositeException;

/**
 * Utility methods for terminal atomics with Throwables.
 * 
 * @since 1.1.2
 */
public enum ExceptionsUtils {
    ;
    
    /** The single instance of a Throwable indicating a terminal state. */
    private static final Throwable TERMINATED = new Throwable("Terminated");
    
    /**
     * Atomically sets or combines the error with the contents of the field, wrapping multiple
     * errors into CompositeException if necessary.
     * 
     * @param field the target field
     * @param error the error to add
     * @return true if successful, false if the target field contains the terminal Throwable.
     */
    public static boolean addThrowable(AtomicReference<Throwable> field, Throwable error) {
        for (;;) {
            Throwable current = field.get();
            if (current == TERMINATED) {
                return false;
            }
            
            Throwable next;
            if (current == null) {
                next = error;
            } else
            if (current instanceof CompositeException) {
                List<Throwable> list = new ArrayList<Throwable>(((CompositeException)current).getExceptions());
                list.add(error);
                next = new CompositeException(list);
            } else {
                next = new CompositeException(current, error);
            }
            
            if (field.compareAndSet(current, next)) {
                return true;
            }
        }
    }
    
    /**
     * Atomically swaps in the terminal Throwable and returns the previous
     * contents of the field
     * 
     * @param field the target field
     * @return the previous contents of the field before the swap, may be null
     */
    public static Throwable terminate(AtomicReference<Throwable> field) {
        Throwable current = field.get();
        if (current != TERMINATED) {
            current = field.getAndSet(TERMINATED);
        }
        return current;
    }
    
    /**
     * Checks if the given field holds the terminated Throwable instance.
     * 
     * @param field the target field
     * @return true if the given field holds the terminated Throwable instance
     */
    public static boolean isTerminated(AtomicReference<Throwable> field) {
        return isTerminated(field.get());
    }

    /**
     * Returns true if the value is the terminated Throwable instance.
     * 
     * @param error the error to check
     * @return true if the value is the terminated Throwable instance
     */
    public static boolean isTerminated(Throwable error) {
        return error == TERMINATED;
    }
}
