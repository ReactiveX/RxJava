/**
 * Copyright (c) 2016-present, RxJava Contributors.
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
package io.reactivex.rxjava3.exceptions;

import java.io.*;
import java.util.*;

import io.reactivex.rxjava3.annotations.NonNull;

/**
 * Represents an exception that is a composite of one or more other exceptions. A {@code CompositeException}
 * does not modify the structure of any exception it wraps, but at print-time it iterates through the list of
 * Throwables contained in the composite in order to print them all.
 *
 * Its invariant is to contain an immutable, ordered (by insertion order), unique list of non-composite
 * exceptions. You can retrieve individual exceptions in this list with {@link #getExceptions()}.
 *
 * The {@link #printStackTrace()} implementation handles the StackTrace in a customized way instead of using
 * {@code getCause()} so that it can avoid circular references.
 *
 * If you invoke {@link #getCause()}, it will lazily create the causal chain but will stop if it finds any
 * Throwable in the chain that it has already seen.
 */
public final class CompositeException extends RuntimeException {

    private static final long serialVersionUID = 3026362227162912146L;

    private final List<Throwable> exceptions;
    private final String message;
    private Throwable cause;

    /**
     * Constructs a CompositeException with the given array of Throwables as the
     * list of suppressed exceptions.
     * @param exceptions the Throwables to have as initially suppressed exceptions
     *
     * @throws IllegalArgumentException if <code>exceptions</code> is empty.
     */
    public CompositeException(@NonNull Throwable... exceptions) {
        this(exceptions == null ?
                Collections.singletonList(new NullPointerException("exceptions was null")) : Arrays.asList(exceptions));
    }

    /**
     * Constructs a CompositeException with the given array of Throwables as the
     * list of suppressed exceptions.
     * @param errors the Throwables to have as initially suppressed exceptions
     *
     * @throws IllegalArgumentException if <code>errors</code> is empty.
     */
    public CompositeException(@NonNull Iterable<? extends Throwable> errors) {
        Set<Throwable> deDupedExceptions = new LinkedHashSet<Throwable>();
        List<Throwable> localExceptions = new ArrayList<Throwable>();
        if (errors != null) {
            for (Throwable ex : errors) {
                if (ex instanceof CompositeException) {
                    deDupedExceptions.addAll(((CompositeException) ex).getExceptions());
                } else
                if (ex != null) {
                    deDupedExceptions.add(ex);
                } else {
                    deDupedExceptions.add(new NullPointerException("Throwable was null!"));
                }
            }
        } else {
            deDupedExceptions.add(new NullPointerException("errors was null"));
        }
        if (deDupedExceptions.isEmpty()) {
            throw new IllegalArgumentException("errors is empty");
        }
        localExceptions.addAll(deDupedExceptions);
        this.exceptions = Collections.unmodifiableList(localExceptions);
        this.message = exceptions.size() + " exceptions occurred. ";
    }

    /**
     * Retrieves the list of exceptions that make up the {@code CompositeException}.
     *
     * @return the exceptions that make up the {@code CompositeException}, as a {@link List} of {@link Throwable}s
     */
    @NonNull
    public List<Throwable> getExceptions() {
        return exceptions;
    }

    @Override
    @NonNull
    public String getMessage() {
        return message;
    }

    @Override
    @NonNull
    public synchronized Throwable getCause() { // NOPMD
        if (cause == null) {
            String separator = System.getProperty("line.separator");
            if (exceptions.size() > 1) {
                Map<Throwable, Boolean> seenCauses = new IdentityHashMap<Throwable, Boolean>();

                StringBuilder aggregateMessage = new StringBuilder();
                aggregateMessage.append("Multiple exceptions (").append(exceptions.size()).append(")").append(separator);

                for (Throwable inner : exceptions) {
                    int depth = 0;
                    while (inner != null) {
                        for (int i = 0; i < depth; i++) {
                            aggregateMessage.append("  ");
                        }
                        aggregateMessage.append("|-- ");
                        aggregateMessage.append(inner.getClass().getCanonicalName()).append(": ");
                        String innerMessage = inner.getMessage();
                        if (innerMessage != null && innerMessage.contains(separator)) {
                            aggregateMessage.append(separator);
                            for (String line : innerMessage.split(separator)) {
                                for (int i = 0; i < depth + 2; i++) {
                                    aggregateMessage.append("  ");
                                }
                                aggregateMessage.append(line).append(separator);
                            }
                        } else {
                            aggregateMessage.append(innerMessage);
                            aggregateMessage.append(separator);
                        }

                        for (int i = 0; i < depth + 2; i++) {
                            aggregateMessage.append("  ");
                        }
                        StackTraceElement[] st = inner.getStackTrace();
                        if (st.length > 0) {
                            aggregateMessage.append("at ").append(st[0]).append(separator);
                        }

                        if (!seenCauses.containsKey(inner)) {
                            seenCauses.put(inner, true);

                            inner = inner.getCause();
                            depth++;
                        } else {
                            inner = inner.getCause();
                            if (inner != null) {
                                for (int i = 0; i < depth + 2; i++) {
                                    aggregateMessage.append("  ");
                                }
                                aggregateMessage.append("|-- ");
                                aggregateMessage.append("(cause not expanded again) ");
                                aggregateMessage.append(inner.getClass().getCanonicalName()).append(": ");
                                aggregateMessage.append(inner.getMessage());
                                aggregateMessage.append(separator);
                            }
                            break;
                        }
                    }
                }

                cause = new ExceptionOverview(aggregateMessage.toString().trim());
            } else {
                cause = exceptions.get(0);
            }
        }
        return cause;
    }

    /**
     * All of the following {@code printStackTrace} functionality is derived from JDK {@link Throwable}
     * {@code printStackTrace}. In particular, the {@code PrintStreamOrWriter} abstraction is copied wholesale.
     *
     * Changes from the official JDK implementation:<ul>
     * <li>no infinite loop detection</li>
     * <li>smaller critical section holding {@link PrintStream} lock</li>
     * <li>explicit knowledge about the exceptions {@link List} that this loops through</li>
     * </ul>
     */
    @Override
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    @Override
    public void printStackTrace(PrintStream s) {
        printStackTrace(new WrappedPrintStream(s));
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        printStackTrace(new WrappedPrintWriter(s));
    }

    /**
     * Special handling for printing out a {@code CompositeException}.
     * Loops through all inner exceptions and prints them out.
     *
     * @param s
     *            stream to print to
     */
    private void printStackTrace(PrintStreamOrWriter s) {
        StringBuilder b = new StringBuilder(128);
        b.append(this).append('\n');
        for (StackTraceElement myStackElement : getStackTrace()) {
            b.append("\tat ").append(myStackElement).append('\n');
        }
        int i = 1;
        for (Throwable ex : exceptions) {
            b.append("  ComposedException ").append(i).append(" :\n");
            appendStackTrace(b, ex, "\t");
            i++;
        }
        s.println(b.toString());
    }

    private void appendStackTrace(StringBuilder b, Throwable ex, String prefix) {
        b.append(prefix).append(ex).append('\n');
        for (StackTraceElement stackElement : ex.getStackTrace()) {
            b.append("\t\tat ").append(stackElement).append('\n');
        }
        if (ex.getCause() != null) {
            b.append("\tCaused by: ");
            appendStackTrace(b, ex.getCause(), "");
        }
    }

    abstract static class PrintStreamOrWriter {
        /** Prints the specified string as a line on this StreamOrWriter. */
        abstract void println(Object o);
    }

    /**
     * Same abstraction and implementation as in JDK to allow PrintStream and PrintWriter to share implementation.
     */
    static final class WrappedPrintStream extends PrintStreamOrWriter {
        private final PrintStream printStream;

        WrappedPrintStream(PrintStream printStream) {
            this.printStream = printStream;
        }

        @Override
        void println(Object o) {
            printStream.println(o);
        }
    }

    static final class WrappedPrintWriter extends PrintStreamOrWriter {
        private final PrintWriter printWriter;

        WrappedPrintWriter(PrintWriter printWriter) {
            this.printWriter = printWriter;
        }

        @Override
        void println(Object o) {
            printWriter.println(o);
        }
    }

    /**
     * Contains a formatted message with a simplified representation of the exception graph
     * contained within the CompositeException.
     */
    static final class ExceptionOverview extends RuntimeException {

        private static final long serialVersionUID = 3875212506787802066L;

        ExceptionOverview(String message) {
            super(message);
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    /**
     * Returns the number of suppressed exceptions.
     * @return the number of suppressed exceptions
     */
    public int size() {
        return exceptions.size();
    }
}
