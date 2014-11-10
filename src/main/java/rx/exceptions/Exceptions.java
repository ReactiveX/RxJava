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
package rx.exceptions;

import java.util.HashSet;
import java.util.Set;

/**
 * @warn javadoc class description missing
 */
public final class Exceptions {
    private Exceptions() {

    }

    /**
     * @warn javadoc missing
     * @return because {@code propagate} itself throws an exception or error, this is a sort of phantom return
     *         value; {@code propagate} does not actually return anything
     */
    public static RuntimeException propagate(Throwable t) {
        /*
         * The return type of RuntimeException is a trick for code to be like this:
         * 
         * throw Exceptions.propagate(e);
         * 
         * Even though nothing will return and throw via that 'throw', it allows the code to look like it
         * so it's easy to read and understand that it will always result in a throw.
         */
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } else {
            throw new RuntimeException(t);
        }
    }

    /**
     * Throws a particular {@code Throwable} only if it belongs to a set of "fatal" error varieties. These
     * varieties are as follows:
     * <ul>
     * <li>{@link OnErrorNotImplementedException}</li>
     * <li>{@link OnErrorFailedException}</li>
     * <li>{@code StackOverflowError}</li>
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
    public static void throwIfFatal(Throwable t) {
        if (t instanceof OnErrorNotImplementedException) {
            throw (OnErrorNotImplementedException) t;
        } else if (t instanceof OnErrorFailedException) {
            Throwable cause = ((OnErrorFailedException) t).getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw (OnErrorFailedException) t;
            }
        }
        // values here derived from https://github.com/ReactiveX/RxJava/issues/748#issuecomment-32471495
        else if (t instanceof StackOverflowError) {
            throw (StackOverflowError) t;
        } else if (t instanceof VirtualMachineError) {
            throw (VirtualMachineError) t;
        } else if (t instanceof ThreadDeath) {
            throw (ThreadDeath) t;
        } else if (t instanceof LinkageError) {
            throw (LinkageError) t;
        }
    }

    private static final int MAX_DEPTH = 25;

    /**
     * Adds a {@code Throwable} to a causality-chain of Throwables, as an additional cause (if it does not
     * already appear in the chain among the causes).
     *
     * @param e
     *         the {@code Throwable} at the head of the causality chain
     * @param cause
     *         the {@code Throwable} you want to add as a cause of the chain
     */
    public static void addCause(Throwable e, Throwable cause) {
        Set<Throwable> seenCauses = new HashSet<Throwable>();

        int i = 0;
        while (e.getCause() != null) {
            if (i++ >= MAX_DEPTH) {
                // stack too deep to associate cause
                return;
            }
            e = e.getCause();
            if (seenCauses.contains(e.getCause())) {
                break;
            } else {
                seenCauses.add(e.getCause());
            }
        }
        // we now have 'e' as the last in the chain
        try {
            e.initCause(cause);
        } catch (Throwable t) {
            // ignore
            // the javadocs say that some Throwables (depending on how they're made) will never
            // let me call initCause without blowing up even if it returns null
        }
    }

    /**
     * Get the {@code Throwable} at the end of the causality-chain for a particular {@code Throwable}
     *
     * @param e
     *         the {@code Throwable} whose final cause you are curious about
     * @return the last {@code Throwable} in the causality-chain of {@code e} (or a "Stack too deep to get
     *         final cause" {@code RuntimeException} if the chain is too long to traverse)
     */
    public static Throwable getFinalCause(Throwable e) {
        int i = 0;
        while (e.getCause() != null) {
            if (i++ >= MAX_DEPTH) {
                // stack too deep to get final cause
                return new RuntimeException("Stack too deep to get final cause");
            }
            e = e.getCause();
        }
        return e;
    }

}
