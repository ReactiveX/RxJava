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
package rx.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Exception that is a composite of 1 or more other exceptions.
 * <p>
 * The <code>getMessage()</code> will return a concatenation of the composite exceptions.
 */
public final class CompositeException extends RuntimeException {

    private static final long serialVersionUID = 3026362227162912146L;

    private final List<Throwable> exceptions;
    private final String message;
    private final Throwable cause;

    public CompositeException(String messagePrefix, Collection<Throwable> errors) {
        List<Throwable> _exceptions = new ArrayList<Throwable>();
        CompositeExceptionCausalChain _cause = new CompositeExceptionCausalChain();
        int count = 0;
        for (Throwable e : errors) {
            count++;
            attachCallingThreadStack(_cause, e);
            _exceptions.add(e);
        }
        this.exceptions = Collections.unmodifiableList(_exceptions);
        this.message = count + " exceptions occurred. See them in causal chain below.";
        this.cause = _cause;
    }

    public CompositeException(Collection<Throwable> errors) {
        this(null, errors);
    }

    public List<Throwable> getExceptions() {
        return exceptions;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public synchronized Throwable getCause() {
        return cause;
    }

    @SuppressWarnings("unused")
    // useful when debugging but don't want to make part of publicly supported API
    private static String getStackTraceAsString(StackTraceElement[] stack) {
        StringBuilder s = new StringBuilder();
        boolean firstLine = true;
        for (StackTraceElement e : stack) {
            if (e.toString().startsWith("java.lang.Thread.getStackTrace")) {
                // we'll ignore this one
                continue;
            }
            if (!firstLine) {
                s.append("\n\t");
            }
            s.append(e.toString());
            firstLine = false;
        }
        return s.toString();
    }

    private static void attachCallingThreadStack(Throwable e, Throwable cause) {
        while (e.getCause() != null) {
            e = e.getCause();
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

    private final static class CompositeExceptionCausalChain extends RuntimeException {
        private static final long serialVersionUID = 3875212506787802066L;

        @Override
        public String getMessage() {
            return "Chain of Causes for CompositeException In Order Received =>";
        }
    }

}