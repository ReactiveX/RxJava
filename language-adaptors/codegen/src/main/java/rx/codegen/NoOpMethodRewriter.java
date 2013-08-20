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

import java.util.ArrayList;
import java.util.List;

import javassist.CtClass;
import javassist.CtMethod;

/**
 * Method that needs no rewriting to function with dynamic language.
 */
public class NoOpMethodRewriter extends MethodRewriter {
    
    @Override
    public boolean needsRewrite() {
        return false;
    }

    @Override
    public boolean isReplacement() {
        return false;
    }

    @Override
    protected List<MethodRewriteRequest> getNewMethodsToRewrite(CtClass[] initialArgTypes) {
        return new ArrayList<MethodRewriteRequest>();
    }

    @Override
    protected String getRewrittenMethodBody(MethodRewriteRequest methodRewriteRequest) {
        return "";
    }
}