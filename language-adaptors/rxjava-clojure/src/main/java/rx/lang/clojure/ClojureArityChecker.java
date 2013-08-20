/**
 * Copyright 2013 Netflix, Inc.
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
package rx.lang.clojure;

import clojure.lang.IFn;

import java.lang.reflect.Method;

/**
 * Base class for Clojure adaptors that knows how to get the arity of an {@code IFn}.
 */
public abstract class ClojureArityChecker {
    protected IFn ifn;

    //Hoping this is correct: 
    //http://stackoverflow.com/questions/1696693/clojure-how-to-find-out-the-arity-of-function-at-runtime
    int getArity() {
        Class<?> ifnClass = ifn.getClass();
        for (Method m: ifnClass.getDeclaredMethods()) {
            return m.getParameterTypes().length;
        }
        return 0;
    }
}