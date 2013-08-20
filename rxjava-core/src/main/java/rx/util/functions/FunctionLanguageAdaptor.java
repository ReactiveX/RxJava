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
package rx.util.functions;

import java.util.Map;
import java.util.Set;

public interface FunctionLanguageAdaptor {

    /**
     * The Class of the Function that this adaptor serves.
     * <p>
     * Example: groovy.lang.Closure
     * <p>
     * This should not return classes of java.* packages.
     * 
     * @return Class[] of classes that this adaptor should be invoked for.
     */
    //public Class<?>[] getFunctionClass();

    /**
     * Map detailing how to rewrite a native function class into an Rx {@code Action}
     * Example: for Groovy, a 1-element Map : { groovy.lang.Closure -> GroovyActionWrapper }
     * @return map for rewriting native functions to Rx {@code Action}s.
     */
    public Map<Class<?>, Class<?>> getActionClassRewritingMap();

    /**
     * Map detailing how to rewrite a native function class into an Rx {@code Function}
     * Example: for Groovy, a 1-element Map : { groovy.lang.Closure -> GroovyFunctionWrapper }
     * @return map for rewriting native functions to Rx {@code Function}s.
     */
    public Map<Class<?>, Class<?>> getFunctionClassRewritingMap();

    /**
     * All native function classes that require translation into the Rx world
     * @return set of all native function classes
     */
    public Set<Class<?>> getAllClassesToRewrite();
}
