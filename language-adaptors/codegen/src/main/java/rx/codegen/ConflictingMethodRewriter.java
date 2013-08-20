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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;

import rx.util.functions.FunctionLanguageAdaptor;
import rx.util.functions.Func0;
import rx.util.functions.Func1;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Func4;
import rx.util.functions.Func5;
import rx.util.functions.Func6;
import rx.util.functions.Func7;
import rx.util.functions.Func8;
import rx.util.functions.Func9;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Action2;
import rx.util.functions.Action3;

/**
 * A strategy for rewriting a single method out of a {@code ConflictingMethodGroup}.  The goal is to have a single
 * method inserted that branches on the arity of the arguments and dynamically forwards to an Rx core method.
 *
 * The first example of needing conflict resolution (more are expected) came up on the following buffer method signatures:
 * public static <T> Observable<List<T>> buffer(Observable<T> source, Func0<Observable<BufferClosing>> bufferClosingSelector)
 * public Observable<List<T>> buffer(Observable<BufferOpening> bufferOpenings, Func1<BufferOpening, Observable<BufferClosing>> bufferClosingSelector)
 * These methods both would be rewritten to buffer(Observable, groovy.lang.Closure) unless we handled the conflict
 * explicitly.
 */
public class ConflictingMethodRewriter extends MethodRewriter {

    private ConflictingMethodGroup conflictingMethodGroup;
    
    public ConflictingMethodRewriter(CtClass enclosingClass, CtMethod initialMethod, FunctionLanguageAdaptor adaptor, ConflictingMethodGroup conflictingMethodGroup) {
        this.enclosingClass = enclosingClass;
        this.initialMethod = initialMethod;
        this.adaptor = adaptor;
        this.conflictingMethodGroup = conflictingMethodGroup;
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
        return duplicatedMethodsWithWrappedFunctionTypes();
    }

    //Sample body:
    //public T buffer(Observable $1, Closure $2) {
    //    GroovyFunctionAdaptor adaptor = new GroovyFunctionAdaptor($2);
    //    int arity = adaptor.getArity();
    //    if (arity == 0) {
    //        return buffer($1, ((Func0) adaptor));
    //    } else if (arity == 1) {
    //        return buffer($1, ((Func1) adaptor));
    //    } else {
    //        throw new RuntimeException("Couldn't match num args : " + arity);
    //    }
    //}

   /*
   * Known shortcoming - this only works for method groups that have 1 Function in the params
   * This simplified the generated code and at the moment, there is only 1 set of methods which
   * needs this treatment:
   * public static <T> Observable<List<T>> buffer(Observable<T> source, Func0<Observable<BufferClosing>> bufferClosingSelector)
   * public Observable<List<T>> buffer(Observable<BufferOpening> bufferOpenings, Func1<BufferOpening, Observable<BufferClosing>> bufferClosingSelector)
   */
    @Override
    protected String getRewrittenMethodBody(MethodRewriteRequest methodRewriteRequest) {
        final Map<Integer, CtMethod> arityMap = new HashMap<Integer, CtMethod>();
        try {
            for (CtMethod conflictingMethod: conflictingMethodGroup.getMethods()) {
                CtClass[] parameterTypes = conflictingMethod.getParameterTypes();
                for (CtClass paramType: parameterTypes) {
                    if (isRxFunctionType(paramType) || isRxActionType(paramType)) {
                        Integer arity = getArityFromName(paramType);
                        arityMap.put(arity, conflictingMethod);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error determining arity map : " + e);
        }

        final StringBuffer newBody = new StringBuffer();
        newBody.append("{");

        try {
            for (int i = 0; i < initialMethod.getParameterTypes().length; i++) {
                CtClass argType = initialMethod.getParameterTypes()[i];
                if (isRxActionType(argType)) {
                    newBody.append(methodRewriteRequest.getActionAdaptorClass().getName() + " adaptor = ");
                    newBody.append(getAdaptedArg(methodRewriteRequest.getActionAdaptorClass(), i + 1));
                    newBody.append(";");
                } else if (isRxFunctionType(argType)) {
                    newBody.append(methodRewriteRequest.getFunctionAdaptorClass().getName() + " adaptor = ");
                    newBody.append(getAdaptedArg(methodRewriteRequest.getFunctionAdaptorClass(), i + 1));
                    newBody.append(";");
                }
            }

            newBody.append("int arity = adaptor.getArity();");
            for (Integer arity: arityMap.keySet()) {
                CtMethod methodWithArity = arityMap.get(arity);
                newBody.append("if (arity == " + arity.toString() + ") {");
                newBody.append("return ");
                if (Modifier.isStatic(methodWithArity.getModifiers())) {
                    newBody.append(enclosingClass.getName() + ".");
                } else {
                    newBody.append("this.");
                }
                newBody.append(methodWithArity.getName());
                newBody.append("(");
                List<String> argumentList = new ArrayList<String>();
                for (int i = 0; i < initialMethod.getParameterTypes().length; i++) {
                    CtClass argType = initialMethod.getParameterTypes()[i];
                    if (isRxActionType(argType)) {
                        String actionArg = "((rx.util.functions.Action" + arity.toString() + ") adaptor)";
                        argumentList.add(actionArg);
                    } else if (isRxFunctionType(argType)) {
                        String functionArg = "((rx.util.functions.Func" + arity.toString() + ") adaptor)";
                        argumentList.add(functionArg);
                    } else {
                        argumentList.add(getUntouchedArg(i + 1));
                    }
                }
                newBody.append(makeArgList(argumentList));
                newBody.append(");");
                newBody.append(" } else ");
            }
            newBody.append(" {");
            newBody.append("throw new RuntimeException(\"Couldn't match arity : \" + arity);");
            newBody.append("}");
            newBody.append("}");
        } catch (Exception e) {
            System.out.println("Exception while create conflicted method body : " + e);
        }        

        return newBody.toString();
    }

    private int getArityFromName(CtClass clazz) {
        if (clazz.getName().equals(Func0.class.getName())) {
            return 0;
        } else if (clazz.getName().equals(Func1.class.getName())) {
            return 1;
        } else if (clazz.getName().equals(Func2.class.getName())) {
            return 2;
        } else if (clazz.getName().equals(Func3.class.getName())) {
            return 3;
        } else if (clazz.getName().equals(Func4.class.getName())) {
            return 4;
        } else if (clazz.getName().equals(Func5.class.getName())) {
            return 5;
        } else if (clazz.getName().equals(Func6.class.getName())) {
            return 6;
        } else if (clazz.getName().equals(Func7.class.getName())) {
            return 7;
        } else if (clazz.getName().equals(Func8.class.getName())) {
            return 8;
        } else if (clazz.getName().equals(Func9.class.getName())) {
            return 9;
        } else if (clazz.getName().equals(Action0.class.getName())) {
            return 0;
        } else if (clazz.getName().equals(Action1.class.getName())) {
            return 1;
        } else if (clazz.getName().equals(Action2.class.getName())) {
            return 2;
        } else if (clazz.getName().equals(Action3.class.getName())) {
            return 3;
        } else {
            return -1;
        }
    }
}