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
package rx.internal.schedulers;

/**
 * Used only for providing context around where work was scheduled should an error occur in a different thread.
 */
public class SchedulerContextException extends Exception {
    /**
     * Constant to use when disabled
     */
    private static final Throwable CONTEXT_MISSING = new SchedulerContextException("Missing context. Enable by setting the system property \"rxjava.captureSchedulerContext=true\"");

    static {
        CONTEXT_MISSING.setStackTrace(new StackTraceElement[0]);
    }

    /**
     * @return a {@link Throwable} that captures the stack trace or a {@link Throwable} that documents how to enable the feature if needed.
     */
    public static Throwable create() {
        String def = "false";
        String setTo = System.getProperty("rxjava.captureSchedulerContext", def);
        return setTo != def && "true".equals(setTo) ? new SchedulerContextException("Asynchronous work scheduled at") : CONTEXT_MISSING;
    }

    private SchedulerContextException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 1L;
}
