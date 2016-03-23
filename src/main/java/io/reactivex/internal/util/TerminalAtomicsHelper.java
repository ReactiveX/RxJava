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

import io.reactivex.functions.*;

/**
 * Helper class to handle copy-on-write arrays plus terminal state for arbitrary references.
 */
public enum TerminalAtomicsHelper {
    ;
    
    /**
     * Atomically swaps in the terminalValue and calls the given callback if not already terminated.
     * @param <U> the reference type
     * @param instance
     * @param terminalValue
     * @param onTerminate
     */
    public static <U> void terminate(AtomicReference<U> instance, 
            U terminalValue, Consumer<? super U> onTerminate) {
        U a = instance.get();
        if (a != terminalValue) {
            a = instance.getAndSet(terminalValue);
            if (a != terminalValue && a != null) {
                onTerminate.accept(a);
            }
        }
    }
    
    /**
     * Atomically swaps in the terminal value and returns the previous value.
     * @param <U> the reference type
     * @param instance
     * @param terminalValue
     * @return the last value or the terminal value
     */
    public static <U> U terminate(AtomicReference<U> instance, 
            U terminalValue) {
        U a = instance.get();
        if (a != terminalValue) {
            a = instance.getAndSet(terminalValue);
        }
        return a;
    }
    
    /**
     * Atomically replaces the contents of the instance and calls the callback with the old value
     * or calls the callback with the new value if the instance holds the terminal value.
     * @param <T> the instance type
     * @param <U> the reference type
     * @param instance
     * @param newValue
     * @param terminalValue
     * @param onTerminate
     * @return false if the instance holds the terminal value
     * @see #update(AtomicReference, Object, Object, Consumer)
     */
    public static <T, U> boolean set(AtomicReference<U> instance, 
            U newValue, U terminalValue, Consumer<? super U> onTerminate) {
        for (;;) {
            U a = instance.get();
            if (a == terminalValue) {
                if (newValue != null) {
                    onTerminate.accept(newValue);
                }
                return false;
            }
            if (instance.compareAndSet(a, newValue)) {
                if (a != null) {
                    onTerminate.accept(a);
                }
                return true;
            }
        }
    }

    /**
     * Atomically replaces the content of the instance but calls the callback only if the instance holds
     * the terminal value.
     * @param <T> the instance type
     * @param <U> the reference type
     * @param instance
     * @param newValue
     * @param terminalValue
     * @param onTerminate
     * @return false if the instance holds the terminal value
     * @see #set(AtomicReference, Object, Object, Consumer)
     */
    public static <T, U> boolean update(AtomicReference<U> instance, 
            U newValue, U terminalValue, Consumer<? super U> onTerminate) {
        for (;;) {
            U a = instance.get();
            if (a == terminalValue) {
                if (newValue != null) {
                    onTerminate.accept(newValue);
                }
                return false;
            }
            if (instance.compareAndSet(a, newValue)) {
                return true;
            }
        }
    }

    /**
     * Atomically replaces the array in instance with a new array that contains the newValue as well
     * or returns false if the instance holds the terminalValue.
     * @param <U> the reference type
     * @param instance
     * @param newValue
     * @param terminalValue
     * @param arraySupplier
     * @return true if the add succeeded, false if the instance has terminated
     */
    public static <U> boolean add(AtomicReference<U[]> instance,
            U newValue, U[] terminalValue, IntFunction<U[]> arraySupplier) {
        for (;;) {
            U[] a = instance.get();
            if (a == terminalValue) {
                return false;
            }
            int n = a.length;
            U[] b = arraySupplier.apply(n + 1);
            System.arraycopy(a, 0, b, 0, n);
            b[n] = newValue;
            if (instance.compareAndSet(a, b)) {
                return true;
            }
        }
    }
    
    /**
     * Atomically replaces the array in instance with a new array that doesn't contain the
     * given value or returns false if the instance holds the terminal state or an empty array
     * or the value is not in the array.
     * @param <U> the reference type
     * @param reference
     * @param value
     * @param terminalValue
     * @param zeroArray
     * @param arraySupplier
     * @return true if the remove succeeded, false if the instance has terminated
     */
    public static <U> boolean remove(AtomicReference<U[]> reference,
            U value, U[] terminalValue, U[] zeroArray, IntFunction<U[]> arraySupplier) {
        for (;;) {
            U[] a = reference.get();
            if (a == terminalValue) {
                return false;
            }
            int n = a.length;
            if (n == 0) {
                return false;
            }
            int j = -1;
            for (int i = 0; i < n; i++) {
                if (a[i] == value) {
                    j = i;
                    break;
                }
            }
            if (j < 0) {
                return false;
            }
            U[] b;
            if (n == 1) {
                b = zeroArray;
            } else {
                b = arraySupplier.apply(n - 1);
                System.arraycopy(a, 0, b, 0, j);
                System.arraycopy(a, j + 1, b, j, n - j - 1);
            }
            if (reference.compareAndSet(a, b)) {
                return true;
            }
        }
    }
}
