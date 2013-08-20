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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import rx.util.functions.Func1;
import rx.util.functions.FunctionLanguageAdaptor;

/**
 * An implementation of method rewriting that handles subscribe(Map) specially.  The core method has the type signature
 * subscribe(Map<String, Function> m).  Since {@code Map} is generic, we can't add a new method
 * subscribe(Map<String, groovy.lang.Closure> m) (or some other dynamic variant), since the JVM believes those 2 methods
 * are the same because of type erasure.
 *
 * Instead, we rewrite the body of the method to handle any necessary run-time checks and casting, while leaving the
 * signature along.
 *
 * For Groovy (as an example), the new class would contain:
 * subscribe(Map callbacks) {
 *     Func1 dynamicLanguageSupport = new DynamicConversion();
 *     return subscribeWithConversion(callbacks, dynamicLanguageSupport);
 * }
 * {@see Func1Generator} for details on how the Func1 is generated to have the appropriate runtime checks.
 */
public class OneArgSubscribeOnMapMethodRewriter extends MethodRewriter {

    private static final String DYNAMIC_FUNC1_NAME = "Func1DynamicConverter";

    public OneArgSubscribeOnMapMethodRewriter(CtClass enclosingClass, CtMethod method, FunctionLanguageAdaptor adaptor) {
        this.enclosingClass = enclosingClass;
        this.initialMethod = method;
        this.adaptor = adaptor;
    }

    @Override
    public boolean needsRewrite() {
        return true;
    }

    @Override
    public boolean isReplacement() {
        return true;
    }

    /**
     * We want to return a subscribe(Map m) method.  Note that this is true no matter how many
     * dynamic languages we intend to support, since Map is generic.
     */
    @Override
    protected List<MethodRewriteRequest> getNewMethodsToRewrite(CtClass[] initialArgTypes) {
        List<MethodRewriteRequest> reqs = new ArrayList<MethodRewriteRequest>();
        for (Class<?> nativeFunctionClass: adaptor.getAllClassesToRewrite()) {
            Class<?> functionAdaptorClass = adaptor.getFunctionClassRewritingMap().get(nativeFunctionClass);
            Class<?> actionAdaptorClass = adaptor.getActionClassRewritingMap().get(nativeFunctionClass);
            reqs.add(new MethodRewriteRequest(functionAdaptorClass, actionAdaptorClass, Arrays.asList(initialArgTypes)));
            return reqs;
        }
        return reqs;
    }

    @Override
    protected String getRewrittenMethodBody(MethodRewriteRequest methodRewriteRequest) {
        Class<?> func1Class = Func1.class;
        StringBuffer methodBody = new StringBuffer();

        try {
            CtClass dynamicFunc1CtClass = pool.get(DYNAMIC_FUNC1_NAME);

            methodBody.append("{");
            methodBody.append(func1Class.getName() + " dynamicLanguageSupport = new " + dynamicFunc1CtClass.getName() + "();\n");
            methodBody.append("return subscribeWithConversion($1, dynamicLanguageSupport);");
            methodBody.append("}");
        } catch (NotFoundException ex) {
            System.out.println("Couldn't find class for : " + DYNAMIC_FUNC1_NAME);
            throw new RuntimeException("Couldn't find class for : " + DYNAMIC_FUNC1_NAME);
        }
        return methodBody.toString();
    }
}