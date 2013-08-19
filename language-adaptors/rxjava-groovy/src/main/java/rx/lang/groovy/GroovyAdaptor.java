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
package rx.lang.groovy;

import java.util.HashMap;
import java.util.Map;

import rx.util.functions.FunctionLanguageAdaptor;

import groovy.lang.Closure;

import java.util.HashSet;
import java.util.Set;

/**
 *  Defines the single Groovy class {@code Closure} that should map to Rx functions
 */
public class GroovyAdaptor implements FunctionLanguageAdaptor {

    @Override
    public Map<Class<?>, Class<?>> getFunctionClassRewritingMap() {
        Map<Class<?>, Class<?>> m = new HashMap<Class<?>, Class<?>>();
        m.put(Closure.class, GroovyFunctionWrapper.class);
        return m;
    }

    @Override
    public Map<Class<?>, Class<?>> getActionClassRewritingMap() {
        Map<Class<?>, Class<?>> m = new HashMap<Class<?>, Class<?>>();
        m.put(Closure.class, GroovyActionWrapper.class);
        return m;
    }

    @Override
    public Set<Class<?>> getAllClassesToRewrite() {
        Set<Class<?>> groovyClasses = new HashSet<Class<?>>();
        groovyClasses.add(Closure.class);
        return groovyClasses;
    }
}

