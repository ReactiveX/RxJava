/**
 * Copyright 2016 Netflix, Inc.
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

import rx.annotations.Experimental;
import rx.plugins.RxJavaHooks;

/**
 * A RuntimeException that is stackless but holds onto a textual
 * stacktrace from tracking the assembly location of operators.
 */
@Experimental
public final class AssemblyStackTraceException extends RuntimeException {

    /** */
    private static final long serialVersionUID = 2038859767182585852L;

    /**
     * Constructs an AssemblyStackTraceException with the given message.
     * @param message the message
     */
    public AssemblyStackTraceException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() { // NOPMD
        return this;
    }

    /**
     * Finds an empty cause slot and assigns itself to it.
     * @param exception the exception to start from
     */
    public void attachTo(Throwable exception) {
        Set<Throwable> memory = new HashSet<Throwable>();

        for (;;) {
            if (exception.getCause() == null) {
                try {
                    exception.initCause(this);
                } catch (IllegalStateException e) {
                    RxJavaHooks.onError(new RuntimeException(
                        "Received an exception with a cause set to null, instead of being unset."
                            + " To fix this, look down the chain of causes. The last exception had"
                            + " a cause explicitly set to null. It should be unset instead.",
                        exception));
                }
                return;
            }

            exception = exception.getCause();
            if (!memory.add(exception)) {
                // in case we run into a cycle, give up and report this to the hooks
                RxJavaHooks.onError(this);
                return;
            }
        }
    }

    /**
     * Locate the first AssemblyStackTraceException in the causal chain of the
     * given Throwable (or it if it's one).
     * @param e the input throwable
     * @return the AssemblyStackTraceException located or null if not found
     */
    public static AssemblyStackTraceException find(Throwable e) {
        Set<Throwable> memory = new HashSet<Throwable>();
        for (;;) {
            if (e instanceof AssemblyStackTraceException) {
                return (AssemblyStackTraceException)e;
            }
            if (e == null || e.getCause() == null) {
                return null;
            }
            e = e.getCause();
            if (!memory.add(e)) {
                return null;
            }
        }
    }
}
