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

/**
 * Custom exception for operator testing; makes sure an unwanted exception is
 * not mixed up with a wanted exception.
 */
public final class TestException extends RuntimeException {
    /** */
    private static final long serialVersionUID = -1138830497957801910L;
    /** Create the test exception without any message. */
    public TestException() {
        super();
    }
    /**
     * Create the test exception with the provided message.
     * @param message the mesage to use
     */
    public TestException(String message) {
        super(message);
    }
}
