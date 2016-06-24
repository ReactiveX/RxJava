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

import rx.annotations.Experimental;

/**
 * A RuntimeException that is stackless but holds onto a textual
 * stacktrace from tracking the assembly location of operators.
 */
@Experimental
public final class AssemblyStackTraceException extends RuntimeException {

    /** */
    private static final long serialVersionUID = 2038859767182585852L;

    /**
     * Constructs an AssemblyStackTraceException with the given message and
     * a cause.
     * @param message the message
     * @param cause the cause
     */
    public AssemblyStackTraceException(String message, Throwable cause) {
        super(message, cause);
    }

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
}
