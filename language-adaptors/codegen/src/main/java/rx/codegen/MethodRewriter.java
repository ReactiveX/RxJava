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
import java.util.Map;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import rx.util.functions.Action;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Action2;
import rx.util.functions.Action3;
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
import rx.util.functions.Function;
import rx.util.functions.FunctionLanguageAdaptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * A strategy for how to rewrite methods. This uses the javassist library heavily.  For every method in an
 * {@code Observable} class, a {@code MethodRewriter} gets constructed and used to output a method or methods
 * that add dynamic language support to the {@code Observable} class.
 */
abstract class MethodRewriter {

    protected static ClassPool pool = ClassPool.getDefault();
    protected CtClass enclosingClass;
    protected CtMethod initialMethod;
    protected FunctionLanguageAdaptor adaptor;

    /**
     * Public static constructor
     * @param enclosingClass class in which method exists
     * @param method method to rewrite
     * @param adaptor Language adaptor which describes how to add dynamic language support
     * @return a strategy for rewriting the given {@code CtMethod}
     * @throws Exception on any error
     */
    public static MethodRewriter from(CtClass enclosingClass, CtMethod method, FunctionLanguageAdaptor adaptor, List<ConflictingMethodGroup> conflictingMethodGroups) throws Exception {
        ConflictingMethodGroup conflictingMethodGroup = getConflictingMethodGroup(conflictingMethodGroups, method);
        if (conflictingMethodGroup != null) {
            if (conflictingMethodGroup.hasHead(method)) {
                return new ConflictingMethodRewriter(enclosingClass, method, adaptor, conflictingMethodGroup);
            }
        } else {
            if (enclosingClass.equals(method.getDeclaringClass())) {
                if (isOneArgSubscribeOnMap(enclosingClass, method)) {
                    return new OneArgSubscribeOnMapMethodRewriter(enclosingClass, method, adaptor);
                } else if (argTypeIncludesRxFunction(enclosingClass, method)) {
                    return new AddSpecializedDynamicMethodRewriter(enclosingClass, method, adaptor);
                }
            }
        }
        return new NoOpMethodRewriter();
    }

    /**
     * Does the method require a rewrite at all?
     */
    public abstract boolean needsRewrite();

    /**
     * Should the method replace the original or add additional methods?
     */
    public abstract boolean isReplacement();

    /**
     * Given arguments on the initial method, what methods should get generated?
     * @param initialArgTypes arguments of the initial method
     * @return a list of method rewrites to attempt
     */
    protected abstract List<MethodRewriteRequest> getNewMethodsToRewrite(CtClass[] initialArgTypes);

    /**
     * Return the body of the rewritten body for the {@code MethodRewriteRequest} that gets made
     * @param method initial method
     * @param enclosingClass {@code Observable} class in which initial method exists
     * @param methodRewriteRequest request for rewrite (includes args of new method and adaptor for how to add dynamic
     *                             support for a given language)
     * @return {@code String} body of new method
     */
    protected abstract String getRewrittenMethodBody(MethodRewriteRequest methodRewriteRequest);

    /**
     * Return all the fully-formed {@code CtMethod}s that the rewriting process generates, given the initial method
     * @return new {@code CtMethod}s
     * @throws Exception on any error
     */
    public List<CtMethod> getNewMethods() throws Exception {
        List<CtMethod> methodList = new ArrayList<CtMethod>();
        
        CtClass[] initialArgTypes = initialMethod.getParameterTypes();

        List<MethodRewriteRequest> newMethodRewriteRequests = getNewMethodsToRewrite(initialArgTypes);
        for (MethodRewriteRequest newMethodRewriteRequest: newMethodRewriteRequests) {
            List<CtClass> newArgTypes = newMethodRewriteRequest.getNewArgTypes();
            CtClass[] newArgTypeArray = newArgTypes.toArray(new CtClass[newArgTypes.size()]);
            CtMethod newMethod = new CtMethod(initialMethod.getReturnType(), initialMethod.getName(), newArgTypeArray, enclosingClass);
            newMethod.setModifiers(initialMethod.getModifiers());
            String newBody = getRewrittenMethodBody(newMethodRewriteRequest);
            newMethod.setBody(newBody);
            methodList.add(newMethod);

            //Uncomment this to see the rewritten method body
            //debugMethodRewriting(initialMethod, newMethod, newBody);
        }
        return methodList;
    }

    /**
     * Verbose output on how methods are getting rewritten
     * @param initialMethod initial method
     * @param newMethod new method
     * @param newBody new method body
     * @throws NotFoundException
     */
    private static void debugMethodRewriting(CtMethod initialMethod, CtMethod newMethod, String newBody) throws NotFoundException {
        List<String> initialArgStrings = mapClassNames(initialMethod.getParameterTypes());
        List<String> finalArgStrings = mapClassNames(newMethod.getParameterTypes());

        String initialArgString = makeArgList(initialArgStrings);
        String finalArgString = makeArgList(finalArgStrings);

        System.out.println(initialMethod.getReturnType().getName() + " " + initialMethod.getName() + "(" + initialArgString +") --> " + newMethod.getReturnType().getName() + " " + newMethod.getName() + "(" + finalArgString + ")");
        System.out.println("    " + newBody.toString());
    }

    private static ConflictingMethodGroup getConflictingMethodGroup(List<ConflictingMethodGroup> conflictingMethodGroups, CtMethod method) {
        for (ConflictingMethodGroup group: conflictingMethodGroups) {
            if (group.getMethods().contains(method)) {
                return group;
            }
        }
        return null;
    }

    /**
     * We need to special case the subscribe(Map m) method.  Check if the given method is subscribe(Map m)
     * @param enclosingClass {@code Observable} class where method lives
     * @param method method to check
     * @return true iff the passed-in method is subscribe(Map m)
     * @throws NotFoundException
     */
    private static boolean isOneArgSubscribeOnMap(CtClass enclosingClass, CtMethod method) throws Exception {
        String methodName = method.getName();
        CtClass[] args = method.getParameterTypes();
        if (args.length == 1 && methodName.equals("subscribe")) {
            if (isMap(args[0])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the passed-in method has any arguments which are an Rx core function type.
     * If so, we will need to add a variant of this method that can be called with a native function class instead.
     * @param method method to check
     * @return true iff the method has an Rx core function type in its args
     * @throws Exception
     */
    private static boolean argTypeIncludesRxFunction(CtClass enclosingClass, CtMethod method) throws Exception {
        CtClass[] args = method.getParameterTypes();
        for (CtClass methodArg: args) {
            if (isRxFunctionType(methodArg) || isRxActionType(methodArg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a class is an Rx core {@code Function} type
     * @param type class to check
     * @return true iff the passed-in class is a {@code Function}
     * @throws Exception
     */
    protected static boolean isRxFunctionType(CtClass type) throws Exception {
        return implementsInterface(type, Function.class);
    }

    /**
     * Check if a class is an Rx core {@code Action} type
     * @param type class to check
     * @return true iff the passed-in class is a {@code Action}
     * @throws Exception
     */
    protected static boolean isRxActionType(CtClass type) throws Exception {
        return implementsInterface(type, Action.class);
    }

    /**
     * Check if the class is any subclass of {@code Map}.
     * @param type class to check
     * @return true iff the passed-in class in a {@code Map}
     * @throws Exception
     */
    protected static boolean isMap(CtClass type) throws Exception {
        return implementsInterface(type, Map.class);
    }

    /**
     * Helper method to check if a given class implements a given interface (recursively)
     * @param type class to check
     * @param interfaceClass interface to check implementation of
     * @return true iff the passed-in type implements the passed-in interface
     * @throws Exception
     */
    protected static boolean implementsInterface(CtClass type, Class<?> interfaceClass) throws Exception {
        // Did I pass in the exact interface?
        if (type.getName().equals(interfaceClass.getName())) {
            return true;
        }
        CtClass[] implementedInterfaces = type.getInterfaces();
        if (implementedInterfaces.length == 0) {
            //no more superclasses to check
            return false;
        } else {
            for (CtClass implementedInterface: implementedInterfaces) {
                if (implementedInterface.getName().equals(interfaceClass.getName())) {
                    return true;
                }
                if (implementsInterface(implementedInterface, interfaceClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Given an initial method, return {@code MethodRewriteRequest}s that replace all Rx functions with
     * the native function equivalent, so that dynamic languages can target this method.
     * @return {@code MethodRewriteRequest}s that dynamic languages can natively target
     */
    protected List<MethodRewriteRequest> duplicatedMethodsWithWrappedFunctionTypes() {
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

    /**
     * Convert {@code CtClass} array to {@code List<String>}
     * @param classes classes to get names of
     * @return list of class names
     */
    protected static List<String> mapClassNames(CtClass[] classes) {
        List<String> strs = new ArrayList<String>();
        for (CtClass clazz: classes) {
            strs.add(clazz.getName());
        }
        return strs;
    }

    /**
     * Convert {@code List<String>} into comma-separated single {@code String}.
     * @param argTypes list of argument strings
     * @return comma-separated list of strings
     */
    protected static String makeArgList(List<String> argTypes) {
        if (argTypes.size() > 0) {
            StringBuffer buffer = new StringBuffer(argTypes.get(0));
            for (String argType: argTypes.subList(1, argTypes.size())) {
                buffer.append("," + argType);
            }
            return buffer.toString();
        }
        return "";
    }

    /**
     * Within a rewritten method body, wrap an arg in an adaptor class in a javassist-friendly way
     * Ex: If adaptorClass is FooAdaptor, then this method performs: "$2" -> "new FooAdaptor($2)"
     * @param adaptorClass adaptor intended to wrap the original arg
     * @param index position of arg (this is how javassist calls args)
     * @return String of wrapped arg
     */
    protected static String getAdaptedArg(Class<?> adaptorClass, int index) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("new ");
        buffer.append(adaptorClass.getName());
        buffer.append("(");
        buffer.append(getUntouchedArg(index));
        buffer.append(")");
        return buffer.toString();
    }

    /**
     * Rewrite an arg (based on position) in a javassist-friendly way.
     * Ex: 2 -> "$2"
     * @param index arg index
     * @return String of arg (in javassist-format)
     */
    protected static String getUntouchedArg(int index) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("$");
        buffer.append(index);
        return buffer.toString();
    }

    public static class UnitTest {
        private static ClassPool pool = ClassPool.getDefault();

        @Test
        public void testIsRxFunctionType() {
            try {
                assertTrue(isRxFunctionType(pool.get(Function.class.getName())));
                assertTrue(isRxFunctionType(pool.get(Func0.class.getName())));
                assertTrue(isRxFunctionType(pool.get(Func1.class.getName())));
                assertTrue(isRxFunctionType(pool.get(Func2.class.getName())));
                assertTrue(isRxFunctionType(pool.get(Func3.class.getName())));
                assertTrue(isRxFunctionType(pool.get(Func4.class.getName())));
                assertTrue(isRxFunctionType(pool.get(Func5.class.getName())));
                assertTrue(isRxFunctionType(pool.get(Func6.class.getName())));
                assertTrue(isRxFunctionType(pool.get(Func7.class.getName())));
                assertTrue(isRxFunctionType(pool.get(Func8.class.getName())));
                assertTrue(isRxFunctionType(pool.get(Func9.class.getName())));
                assertTrue(isRxFunctionType(pool.get(Action.class.getName())));
                assertTrue(isRxFunctionType(pool.get(Action0.class.getName())));
                assertTrue(isRxFunctionType(pool.get(Action1.class.getName())));
                assertTrue(isRxFunctionType(pool.get(Action2.class.getName())));
                assertTrue(isRxFunctionType(pool.get(Action3.class.getName())));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

       @Test
       public void testIsRxActionType() {
           try {
               assertFalse(isRxActionType(pool.get(Function.class.getName())));
               assertFalse(isRxActionType(pool.get(Func0.class.getName())));
               assertFalse(isRxActionType(pool.get(Func1.class.getName())));
               assertFalse(isRxActionType(pool.get(Func2.class.getName())));
               assertFalse(isRxActionType(pool.get(Func3.class.getName())));
               assertFalse(isRxActionType(pool.get(Func4.class.getName())));
               assertFalse(isRxActionType(pool.get(Func5.class.getName())));
               assertFalse(isRxActionType(pool.get(Func6.class.getName())));
               assertFalse(isRxActionType(pool.get(Func7.class.getName())));
               assertFalse(isRxActionType(pool.get(Func8.class.getName())));
               assertFalse(isRxActionType(pool.get(Func9.class.getName())));
               assertTrue(isRxActionType(pool.get(Action.class.getName())));
               assertTrue(isRxActionType(pool.get(Action0.class.getName())));
               assertTrue(isRxActionType(pool.get(Action1.class.getName())));
               assertTrue(isRxActionType(pool.get(Action2.class.getName())));
               assertTrue(isRxActionType(pool.get(Action3.class.getName())));
           } catch (Exception e) {
               fail(e.getMessage());
           }
       }
    }
}