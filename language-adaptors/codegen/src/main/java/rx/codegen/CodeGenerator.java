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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
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

public class CodeGenerator {

    static ClassPool pool = ClassPool.getDefault();

    public void addMethods(Class<?> initialClass, FunctionLanguageAdaptor adaptor, File file) {
        Set<Class<?>> nativeFunctionClasses = adaptor.getAllClassesToRewrite();
        System.out.println("Adding dynamic language support to : " + initialClass.getSimpleName());
        for (Class<?> nativeFunctionClass: nativeFunctionClasses) {
            System.out.println(" * Adding : " + nativeFunctionClass.getName());
        }
        addSupportFor(initialClass, adaptor, file);
    }

    private static void addSupportFor(Class<?> observableClass, FunctionLanguageAdaptor adaptor, File file) {
        CtClass clazz;

        if (!observableClass.getName().startsWith("rx.")) {
            throw new IllegalStateException("Refusing to rewrite a class that is not a core Rx Observable!");
        }

        Set<Class<?>> nativeFunctionClasses = adaptor.getAllClassesToRewrite();

        try {
            clazz = pool.get(observableClass.getName());
        } catch (NotFoundException e) {
            throw new RuntimeException("Failed to add language adaptor methods as could not find observable Class named " + observableClass.getName(), e);
        }
        try {
            rewriteMethodsWithRxArgs(clazz, adaptor);
            writeClassFile(clazz, file);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to add language adaptor methods.", e);
        }
    }

    private static void rewriteMethodsWithRxArgs(CtClass clazz, FunctionLanguageAdaptor adaptor) throws Exception {
        List<CtMethod> newMethods = new ArrayList<CtMethod>();

        for (CtMethod method : clazz.getMethods()) {
            CtClass[] argTypes = method.getParameterTypes();
            boolean needsRewrite = false;
            for (CtClass argType : argTypes) {
                if (isRxFunctionType(argType) || isRxActionType(argType)) {
                    needsRewrite = true;
                }
            }
            if (needsRewrite) {
                try {
                    newMethods.addAll(getRewrittenMethods(clazz, method, adaptor));
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to add language adaptor method: " + method.getName(), e);
                }
            }
        }

        for (CtMethod cm: newMethods) {
            clazz.addMethod(cm);
        }
    }

    private static boolean isNativeFunctionType(CtClass type, Set<Class<?>> nativeFunctionClasses) {
        for (Class<?> nativeFunctionClass: nativeFunctionClasses) {
            if (type.getName().equals(nativeFunctionClass.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRxFunctionType(CtClass type) throws Exception {
        return implementsInterface(type, Function.class);
    }

    private static boolean isRxActionType(CtClass type) throws Exception {
        return implementsInterface(type, Action.class);
    }

    private static boolean implementsInterface(CtClass type, Class<?> interfaceClass) throws Exception {
        // Did I pass in the exact interface?
        if (type.getName().equals(interfaceClass.getName())) {
            return true;
        }
        // Do I implement the interface?
        for (CtClass implementedInterface : type.getInterfaces()) {
            if (implementedInterface.getName().equals(interfaceClass.getName())) {
                return true;
            }
        }
        return false;
    }

    private static List<CtMethod> getRewrittenMethods(CtClass clazz, CtMethod method, FunctionLanguageAdaptor adaptor) throws Exception {
        List<CtMethod> newMethods = new ArrayList<CtMethod>();

        for (Class<?> nativeFunctionClass: adaptor.getAllClassesToRewrite()) {
            Class<?> functionAdaptorClass = adaptor.getFunctionClassRewritingMap().get(nativeFunctionClass);
            Class<?> actionAdaptorClass = adaptor.getActionClassRewritingMap().get(nativeFunctionClass);
            ArrayList<CtClass> parameters = new ArrayList<CtClass>();
            CtClass[] argTypes = method.getParameterTypes();

            ArrayList<String> initialArgTypes = new ArrayList<String>();
            ArrayList<String> finalArgTypes = new ArrayList<String>();

            for (CtClass argType : argTypes) {
                initialArgTypes.add(argType.getName());
                if (isRxFunctionType(argType) || isRxActionType(argType)) {
                    // needs conversion
                    finalArgTypes.add(nativeFunctionClass.getName());
                    parameters.add(pool.get(nativeFunctionClass.getName()));
                } else {
                    // no conversion, copy through
                    finalArgTypes.add(argType.getName());
                    parameters.add(argType);
                }
            }
            
            String initialArgString = makeArgList(initialArgTypes);
            String finalArgString = makeArgList(finalArgTypes);

            CtClass[] oldParameters = parameters.toArray(new CtClass[parameters.size()]);
            CtMethod newMethod = new CtMethod(method.getReturnType(), method.getName(), oldParameters, clazz);
            newMethod.setModifiers(method.getModifiers());
            List<String> argumentList = new ArrayList<String>();
            StringBuffer newBody = new StringBuffer();
            newBody.append("{ return ");
            if (Modifier.isStatic(method.getModifiers())) {
                newBody.append(clazz.getName() + ".");
            } else {
                newBody.append("this.");                
            }
            newBody.append(method.getName());
            newBody.append("(");
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                CtClass argType = method.getParameterTypes()[i];
                if (isRxActionType(argType) && actionAdaptorClass != null) {
                    argumentList.add(getAdaptedArg(actionAdaptorClass, i + 1));
                } else if (isRxFunctionType(argType) && functionAdaptorClass != null) {
                    argumentList.add(getAdaptedArg(functionAdaptorClass, i + 1));
                } else {
                    argumentList.add(getUntouchedArg(i + 1));
                }
            }
            newBody.append(makeArgList(argumentList));
            newBody.append(")");
            newBody.append(";}");
            //Uncomment these to see all of the rewritten methods
            //System.out.println(method.getReturnType().getName() + " " + method.getName() + "(" + initialArgString + ") --> " + newMethod.getReturnType().getName() + " " + newMethod.getName() + "(" + finalArgString + ")");
            //System.out.println("    " + newBody.toString());
            newMethod.setBody(newBody.toString());
            newMethods.add(newMethod);
        }
        return newMethods;
    }

    private static String getAdaptedArg(Class<?> adaptorClass, int index) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("new ");
        buffer.append(adaptorClass.getName());
        buffer.append("(");
        buffer.append(getUntouchedArg(index));
        buffer.append(")");
        return buffer.toString();
    }

    private static String getUntouchedArg(int index) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("$");
        buffer.append(index);
        return buffer.toString();
    }

    private static String makeArgList(List<String> args) {
        if (args.size() > 0) {
            StringBuffer buffer = new StringBuffer(args.get(0));
            for (String arg: args.subList(1, args.size())) {
                buffer.append("," + arg);
            }
            return buffer.toString();
        } 
        return "";
    }

    private static void writeClassFile(CtClass clazz, File dir) {
        try {
            System.out.println("Using " + dir.getCanonicalPath() + " for dynamic class file");
            clazz.writeFile(dir.getCanonicalPath());
        } catch (java.io.IOException ioe) {
            System.out.println("Could not write classfile to : " + dir.toString());
            System.exit(1);
        } catch (javassist.CannotCompileException cce) {
            System.out.println("Could not create a valid classfile");
            System.exit(2);
        }
    }

    public static class UnitTest {
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
                assertFalse(isRxFunctionType(pool.get(Action.class.getName())));
                assertFalse(isRxFunctionType(pool.get(Action0.class.getName())));
                assertFalse(isRxFunctionType(pool.get(Action1.class.getName())));
                assertFalse(isRxFunctionType(pool.get(Action2.class.getName())));
                assertFalse(isRxFunctionType(pool.get(Action3.class.getName())));
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

