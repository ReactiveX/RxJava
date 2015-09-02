/**
 * Copyright 2015 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.util;

public enum Exceptions {
    ;
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

}
