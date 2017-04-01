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

package io.reactivex.exceptions;

import io.reactivex.annotations.*;
import io.reactivex.internal.util.ExceptionHelper;

/**
 * Utility class to help propagate checked exceptions and rethrow exceptions
 * designated as fatal.
 */
public final class Exceptions {

    /** Utility class. */
    private Exceptions() {
        throw new IllegalStateException("No instances!");
    }
    /**
     * Convenience method to throw a {@code RuntimeException} and {@code Error} directly
     * or wrap any other exception type into a {@code RuntimeException}.
     * @param t the exception to throw directly or wrapped
     * @return because {@code propagate} itself throws an exception or error, this is a sort of phantom return
     *         value; {@code propagate} does not actually return anything
     */
    @NonNull
    public static RuntimeException propagate(@NonNull Throwable t) {
        /*
         * The return type of RuntimeException is a trick for code to be like this:
         *
         * throw Exceptions.propagate(e);
         *
         * Even though nothing will return and throw via that 'throw', it allows the code to look like it
         * so it's easy to read and understand that it will always result in a throw.
         */
        throw ExceptionHelper.wrapOrThrow(t);
    }

    /**
     * Throws a particular {@code Throwable} only if it belongs to a set of "fatal" error varieties. These
     * varieties are as follows:
     * <ul>
     * <li>{@code VirtualMachineError}</li>
     * <li>{@code ThreadDeath}</li>
     * <li>{@code LinkageError}</li>
     * </ul>
     * This can be useful if you are writing an operator that calls user-supplied code, and you want to
     * notify subscribers of errors encountered in that code by calling their {@code onError} methods, but only
     * if the errors are not so catastrophic that such a call would be futile, in which case you simply want to
     * rethrow the error.
     *
     * @param t
     *         the {@code Throwable} to test and perhaps throw
     * @see <a href="https://github.com/ReactiveX/RxJava/issues/748#issuecomment-32471495">RxJava: StackOverflowError is swallowed (Issue #748)</a>
     */
    public static void throwIfFatal(@NonNull Throwable t) {
        // values here derived from https://github.com/ReactiveX/RxJava/issues/748#issuecomment-32471495
        if (t instanceof VirtualMachineError) {
            throw (VirtualMachineError) t;
        } else if (t instanceof ThreadDeath) {
            throw (ThreadDeath) t;
        } else if (t instanceof LinkageError) {
            throw (LinkageError) t;
        }
    }
}
