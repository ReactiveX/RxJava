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

import java.io.*;
import java.util.*;

import rx.annotations.Experimental;

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

    /** 
     * Constructs a CompositeException with the given prefix and error collection.
     * @param messagePrefix the prefix to use (actually unused)
     * @param errors the collection of errors
     * @deprecated please use {@link #CompositeException(Collection)} */
    @Deprecated
    public CompositeException(String messagePrefix, Collection<? extends Throwable> errors) {
        Set<Throwable> deDupedExceptions = new LinkedHashSet<Throwable>();
        List<Throwable> _exceptions = new ArrayList<Throwable>();
        if (errors != null) {
            for (Throwable ex : errors) {
                if (ex instanceof CompositeException) {
                    deDupedExceptions.addAll(((CompositeException) ex).getExceptions());
                } else 
                if (ex != null) {
                    deDupedExceptions.add(ex);
                } else {
                    deDupedExceptions.add(new NullPointerException());
                }
            }
        } else {
            deDupedExceptions.add(new NullPointerException());
        }

        _exceptions.addAll(deDupedExceptions);
        this.exceptions = Collections.unmodifiableList(_exceptions);
        this.message = exceptions.size() + " exceptions occurred. ";
    }

    public CompositeException(Collection<? extends Throwable> errors) {
        this(null, errors);
    }

    /**
     * Constructs a CompositeException instance with the supplied initial Throwables.
     * @param errors the array of Throwables
     */
    @Experimental
    public CompositeException(Throwable... errors) {
        Set<Throwable> deDupedExceptions = new LinkedHashSet<Throwable>();
        List<Throwable> _exceptions = new ArrayList<Throwable>();
        if (errors != null) {
            for (Throwable ex : errors) {
                if (ex instanceof CompositeException) {
                    deDupedExceptions.addAll(((CompositeException) ex).getExceptions());
                } else 
                if (ex != null) {
                    deDupedExceptions.add(ex);
                } else {
                    deDupedExceptions.add(new NullPointerException());
                }
            }
        } else {
            deDupedExceptions.add(new NullPointerException());
        }

        _exceptions.addAll(deDupedExceptions);
        this.exceptions = Collections.unmodifiableList(_exceptions);
        this.message = exceptions.size() + " exceptions occurred. ";
    }

    /**
     * Retrieves the list of exceptions that make up the {@code CompositeException}
     *
     * @return the exceptions that make up the {@code CompositeException}, as a {@link List} of {@link Throwable}s
     */
    public List<Throwable> getExceptions() {
        return exceptions;
    }

    @Override
    public String getMessage() {
        return message;
    }

    private Throwable cause = null;

    @Override
    public synchronized Throwable getCause() {
        if (cause == null) {
            // we lazily generate this causal chain if this is called
            CompositeExceptionCausalChain _cause = new CompositeExceptionCausalChain();
            Set<Throwable> seenCauses = new HashSet<Throwable>();

            Throwable chain = _cause;
            for (Throwable e : exceptions) {
                if (seenCauses.contains(e)) {
                    // already seen this outer Throwable so skip
                    continue;
                }
                seenCauses.add(e);
                
                List<Throwable> listOfCauses = getListOfCauses(e);
                // check if any of them have been seen before
                for(Throwable child : listOfCauses) {
                    if (seenCauses.contains(child)) {
                        // already seen this outer Throwable so skip
                        e = new RuntimeException("Duplicate found in causal chain so cropping to prevent loop ...");
                        continue;
                    }
                    seenCauses.add(child);
                }

                // we now have 'e' as the last in the chain
                try {
                    chain.initCause(e);
                } catch (Throwable t) {
                    // ignore
                    // the javadocs say that some Throwables (depending on how they're made) will never
                    // let me call initCause without blowing up even if it returns null
                }
                chain = getRootCause(chain);
            }
            cause = _cause;
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
        StringBuilder bldr = new StringBuilder();
        bldr.append(this).append("\n");
        for (StackTraceElement myStackElement : getStackTrace()) {
            bldr.append("\tat ").append(myStackElement).append("\n");
        }
        int i = 1;
        for (Throwable ex : exceptions) {
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
        for (StackTraceElement stackElement : ex.getStackTrace()) {
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

        @Override
        Object lock() {
            return printStream;
        }

        @Override
        void println(Object o) {
            printStream.println(o);
        }
    }

    private static class WrappedPrintWriter extends PrintStreamOrWriter {
        private final PrintWriter printWriter;

        WrappedPrintWriter(PrintWriter printWriter) {
            this.printWriter = printWriter;
        }

        @Override
        Object lock() {
            return printWriter;
        }

        @Override
        void println(Object o) {
            printWriter.println(o);
        }
    }

    /* package-private */final static class CompositeExceptionCausalChain extends RuntimeException {
        private static final long serialVersionUID = 3875212506787802066L;
        /* package-private */static String MESSAGE = "Chain of Causes for CompositeException In Order Received =>";

        @Override
        public String getMessage() {
            return MESSAGE;
        }
    }

    private List<Throwable> getListOfCauses(Throwable ex) {
        List<Throwable> list = new ArrayList<Throwable>();
        Throwable root = ex.getCause();
        if (root == null || root == ex) {
            return list;
        } else {
            while(true) {
                list.add(root);
                Throwable cause = root.getCause();
                if (cause == null || cause == root) {
                    return list;
                } else {
                    root = root.getCause();
                }
            }
        }
    }

    /**
     * Returns the root cause of {@code e}. If {@code e.getCause()} returns {@null} or {@code e}, just return {@code e} itself.
     *
     * @param e the {@link Throwable} {@code e}.
     * @return The root cause of {@code e}. If {@code e.getCause()} returns {@null} or {@code e}, just return {@code e} itself.
     */
    private Throwable getRootCause(Throwable e) {
        Throwable root = e.getCause();
        if (root == null || root == e) {
            return e;
        } else {
            while(true) {
                Throwable cause = root.getCause();
                if (cause == null || cause == root) {
                    return root;
                }
                root = root.getCause();
            }
        }
    }
}
