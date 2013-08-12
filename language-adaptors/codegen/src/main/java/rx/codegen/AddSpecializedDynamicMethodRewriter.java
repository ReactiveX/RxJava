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
import javassist.Modifier;

import rx.util.functions.FunctionLanguageAdaptor;

/**
 * An implementation of method rewriting that leaves the rx.Function variant alone and adds dynamic language
 * support by adding a new method per native function class.  The new methods do nothing more than convert the
 * native function class into an rx.Function and forward to the core implementation.
 *
 * For example, if {@code Observable} has a method Integer foo(String s, Func1<A, B> f, Integer i), then the
 * rewritten class will add more 'foo's with dynamic language support.
 *
 * For Groovy (as an example), the new class would contain:
 * Integer foo(String s, Func1<A, B> f, Integer i);
 * Integer foo(String s, Closure f, Integer i) {
 *     return foo(s, new GroovyFunctionWrapper(f), i);
 * }
 */
public class AddSpecializedDynamicMethodRewriter extends MethodRewriter {

    public AddSpecializedDynamicMethodRewriter(CtClass enclosingClass, CtMethod method, FunctionLanguageAdaptor adaptor) {
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
        return false;
    }

    @Override
    protected List<MethodRewriteRequest> getNewMethodsToRewrite(CtClass[] initialArgTypes) {
        List<MethodRewriteRequest> reqs = new ArrayList<MethodRewriteRequest>();

        try {
            for (Class<?> nativeFunctionClass: adaptor.getAllClassesToRewrite()) {
                Class<?> functionAdaptorClass = adaptor.getFunctionClassRewritingMap().get(nativeFunctionClass);
                Class<?> actionAdaptorClass = adaptor.getActionClassRewritingMap().get(nativeFunctionClass);
                
                CtClass[] argTypes = initialMethod.getParameterTypes();
                List<CtClass> parameters = new ArrayList<CtClass>();
                
                for (CtClass argType : argTypes) {
                    if (isRxFunctionType(argType) || isRxActionType(argType)) {
                        // needs conversion
                        parameters.add(pool.get(nativeFunctionClass.getName()));
                    } else {
                        // no conversion, copy through
                        parameters.add(argType);
                    }
                }
                MethodRewriteRequest req = new MethodRewriteRequest(functionAdaptorClass, actionAdaptorClass, parameters);
                reqs.add(req);
            }
        } catch (Exception ex) {
            System.out.println("Exception while rewriting method : " + initialMethod.getName());
        }
        return reqs;
    }

    @Override
    protected String getRewrittenMethodBody(CtMethod method, CtClass enclosingClass, MethodRewriteRequest req) {
        StringBuffer newBody = new StringBuffer();
        List<String> argumentList = new ArrayList<String>();
        newBody.append("{ return ");
        if (Modifier.isStatic(method.getModifiers())) {
            newBody.append(enclosingClass.getName() + ".");
        } else {
            newBody.append("this.");
        }
        newBody.append(method.getName());
        newBody.append("(");
        try {
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                CtClass argType = method.getParameterTypes()[i];
                if (isRxActionType(argType) && req.getActionAdaptorClass() != null) {
                    argumentList.add(getAdaptedArg(req.getActionAdaptorClass(), i + 1));
                } else if (isRxFunctionType(argType) && req.getFunctionAdaptorClass() != null) {
                    argumentList.add(getAdaptedArg(req.getFunctionAdaptorClass(), i + 1));
                } else {
                    argumentList.add(getUntouchedArg(i + 1));
                }
            }
        } catch (Exception ex) {
            System.out.println("Exception while creating body for dynamic version of : " + initialMethod.getName());
        }
        newBody.append(makeArgList(argumentList));
        newBody.append(")");
        newBody.append(";}");
        return newBody.toString();
    }
}