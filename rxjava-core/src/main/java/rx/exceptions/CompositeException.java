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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Exception that is a composite of 1 or more other exceptions.
 * A CompositeException does not modify the structure of any exception it wraps, but at print-time
 * iterates through the list of contained Throwables to print them all.
 *
 * Its invariant is to contains an immutable, ordered (by insertion order), unique list of non-composite exceptions.
 * This list may be queried by {@code #getExceptions()}
 */
public final class CompositeException extends RuntimeException {

    private static final long serialVersionUID = 3026362227162912146L;

    private final List<Throwable> exceptions;
    private final String message;

    public CompositeException(String messagePrefix, Collection<? extends Throwable> errors) {
        Set<Throwable> deDupedExceptions = new LinkedHashSet<Throwable>();
        List<Throwable> _exceptions = new ArrayList<Throwable>();
        for (Throwable ex: errors) {
            if (ex instanceof CompositeException) {
                deDupedExceptions.addAll(((CompositeException) ex).getExceptions());
            } else {
                deDupedExceptions.add(ex);
            }
        }

        _exceptions.addAll(deDupedExceptions);
        this.exceptions = Collections.unmodifiableList(_exceptions);
        this.message = exceptions.size() + " exceptions occurred. See them in causal chain below.";
    }

    public CompositeException(Collection<? extends Throwable> errors) {
        this(null, errors);
    }

    /**
     * Retrieves the list of exceptions that make up the {@code CompositeException}
     *
     * @return the exceptions that make up the {@code CompositeException}, as a {@link List} of
     *         {@link Throwable}s
     */
    public List<Throwable> getExceptions() {
        return exceptions;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public synchronized Throwable getCause() {
        return null;
    }

    /**
     * All of the following printStackTrace functionality is derived from JDK Throwable printStackTrace.
     * In particular, the PrintStreamOrWriter abstraction is copied wholesale.
     *
     * Changes from the official JDK implementation:
     * * No infinite loop detection
     * * Smaller critical section holding printStream lock
     * * Explicit knowledge about exceptions List that this loops through
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
     * Special handling for printing out a CompositeException
     * Loop through all inner exceptions and print them out
     * @param s stream to print to
     */
    private void printStackTrace(PrintStreamOrWriter s) {
        StringBuilder bldr = new StringBuilder();
        bldr.append(this).append("\n");
        for (StackTraceElement myStackElement: getStackTrace()) {
            bldr.append("\tat ").append(myStackElement).append("\n");
        }
        int i = 1;
        for (Throwable ex: exceptions) {
            bldr.append("  ComposedException ").append(i).append(" :").append("\n");
            appendStackTrace(bldr, ex, "\t");
            i++;
        }
        synchronized (s.lock()) {
            s.println(bldr.toString());
        }
    }

    private void appendStackTrace(StringBuilder bldr, Throwable ex, String prefix) {
        bldr.append(prefix).append(ex).append("\n");
        for (StackTraceElement stackElement: ex.getStackTrace()) {
            bldr.append("\t\tat ").append(stackElement).append("\n");
        }
        if (ex.getCause() != null) {
            bldr.append("\tCaused by: ");
            appendStackTrace(bldr, ex.getCause(), "");
        }
    }

    private abstract static class PrintStreamOrWriter {
        /** Returns the object to be locked when using this StreamOrWriter */
        abstract Object lock();

        /** Prints the specified string as a line on this StreamOrWriter */
        abstract void println(Object o);
    }

    /**
     * Same abstraction and implementation as in JDK to allow PrintStream and PrintWriter to share implementation
     */
    private static class WrappedPrintStream extends PrintStreamOrWriter {
        private final PrintStream printStream;

        WrappedPrintStream(PrintStream printStream) {
            this.printStream = printStream;
        }

        Object lock() {
            return printStream;
        }

        void println(Object o) {
            printStream.println(o);
        }
    }

    private static class WrappedPrintWriter extends PrintStreamOrWriter {
        private final PrintWriter printWriter;

        WrappedPrintWriter(PrintWriter printWriter) {
            this.printWriter = printWriter;
        }

        Object lock() {
            return printWriter;
        }

        void println(Object o) {
            printWriter.println(o);
        }
    }
}
