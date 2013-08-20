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
package rx.codegen;

import java.util.List;

import javassist.CtClass;

/**
 * Immutable class representing a request to rewrite a method, without specifying how
 */
public class MethodRewriteRequest {
    private Class<?> functionAdaptorClass;
    private Class<?> actionAdaptorClass;
    private List<CtClass> newArgTypes;

    /**
     * Public constructor
     * @param functionAdaptorClass adaptor for rewriting a native function to an Rx core {@code Function}.
     * @param actionAdaptorClass adaptor for rewriting a native action to an Rx core {@code Action}.
     * @param newArgTypes args of the new method that needs to be written
     */
    public MethodRewriteRequest(Class<?> functionAdaptorClass, Class<?> actionAdaptorClass, List<CtClass> newArgTypes) {
        this.functionAdaptorClass = functionAdaptorClass;
        this.actionAdaptorClass = actionAdaptorClass;
        this.newArgTypes = newArgTypes;
    }

    public Class<?> getFunctionAdaptorClass() {
        return this.functionAdaptorClass;
    }

    public Class<?> getActionAdaptorClass() {
        return this.actionAdaptorClass;
    }

    public List<CtClass> getNewArgTypes() {
        return this.newArgTypes;
    }

}