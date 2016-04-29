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

import java.util.*;

import rx.Observer;
import rx.SingleSubscriber;
import rx.annotations.Experimental;

/**
 * Utility class with methods to wrap checked exceptions and
 * manage fatal and regular exception delivery.
 */
public final class Exceptions {
    /** Utility class, no instances. */
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
     * <li>{@link OnCompletedFailedException}</li>
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
            throw (OnErrorFailedException) t;
        } else if (t instanceof OnCompletedFailedException) {
            throw (OnCompletedFailedException) t;
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
    /**
     * Throws a single or multiple exceptions contained in the collection, wrapping it into
     * {@code CompositeException} if necessary.
     * @param exceptions the collection of exceptions. If null or empty, no exception is thrown.
     * If the collection contains a single exception, that exception is either thrown as-is or wrapped into a
     * CompositeException. Multiple exceptions are wrapped into a CompositeException.
     * @since 1.1.0
     */
    public static void throwIfAny(List<? extends Throwable> exceptions) {
        if (exceptions != null && !exceptions.isEmpty()) {
            if (exceptions.size() == 1) {
                Throwable t = exceptions.get(0);
                // had to manually inline propagate because some tests attempt StackOverflowError 
                // and can't handle it with the stack space remaining
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else if (t instanceof Error) {
                    throw (Error) t;
                } else {
                    throw new RuntimeException(t);
                }
            }
            throw new CompositeException(exceptions);
        }
    }
    
    /**
     * Forwards a fatal exception or reports it along with the value
     * caused it to the given Observer.
     * @param t the exception
     * @param o the observer to report to
     * @param value the value that caused the exception
     * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
     */
    @Experimental
    public static void throwOrReport(Throwable t, Observer<?> o, Object value) {
        Exceptions.throwIfFatal(t);
        o.onError(OnErrorThrowable.addValueAsLastCause(t, value));
    }

    /**
     * Forwards a fatal exception or reports it to the given Observer.
     * @param t the exception
     * @param o the observer to report to
     * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
     */
    @Experimental
    public static void throwOrReport(Throwable t, Observer<?> o) {
        Exceptions.throwIfFatal(t);
        o.onError(t);
    }

    /**
     * Forwards a fatal exception or reports it to the given Observer.
     *
     * @param throwable the exception.
     * @param subscriber the subscriber to report to.
     * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number).
     */
    @Experimental
    public static void throwOrReport(Throwable throwable, SingleSubscriber<?> subscriber) {
        Exceptions.throwIfFatal(throwable);
        subscriber.onError(throwable);
    }
}
