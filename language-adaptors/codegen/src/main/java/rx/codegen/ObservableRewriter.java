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
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import rx.util.functions.FunctionLanguageAdaptor;

/**
 * Given an initial {@code Observable} class, rewrite it with all necessary method to support a particular
 * dynamic language (as specified by the {@code FunctionLanguageAdaptor}).
 */
public class ObservableRewriter {

    private final ClassPool pool;
    private final FunctionLanguageAdaptor adaptor;

    public ObservableRewriter(ClassPool pool, FunctionLanguageAdaptor adaptor) {
        this.pool = pool;
        this.adaptor = adaptor;
    }

    /**
     * Entry point - given the passed-in class, add dynamic language support to the class and write it out 
     * to the filesystem
     * 
     * @param initialClass class to add dynamic support to
     * @return class with all rewritten methods
     */
    public CtClass addMethods(Class<?> initialClass) {
        if (!initialClass.getName().startsWith("rx.")) {
            throw new IllegalStateException("Refusing to rewrite a class that is not a core Rx Observable!");
        }
        
        Set<Class<?>> nativeFunctionClasses = adaptor.getAllClassesToRewrite();
        System.out.println("Adding dynamic language support to : " + initialClass.getSimpleName());
        for (Class<?> nativeFunctionClass: nativeFunctionClasses) {
            System.out.println(" * Adding : " + nativeFunctionClass.getName());
        }

        try {
            CtClass clazz = pool.get(initialClass.getName());
            CtClass rewrittenClass = rewriteMethodsWithRxArgs(clazz);
            return rewrittenClass;
        } catch (NotFoundException e) {
            throw new RuntimeException("Failed to add language adaptor methods as could not find observable Class named " + initialClass.getName(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to add language adaptor methods.", e);
        }
    }

    /**
     * Loop over all methods and rewrite each (if necessary), returning the end result
     * @param enclosingClass
     * @return
     * @throws Exception
     */
    private CtClass rewriteMethodsWithRxArgs(CtClass enclosingClass) throws Exception {
        for (CtMethod method : enclosingClass.getMethods()) {
            MethodRewriter methodRewriter = MethodRewriter.from(enclosingClass, method, adaptor);
            if (methodRewriter.needsRewrite()) {
                if (methodRewriter.isReplacement()) {
                    enclosingClass.removeMethod(method);
                }
                List<CtMethod> newMethods = methodRewriter.getNewMethods();
                for (CtMethod cm: newMethods) {
                    enclosingClass.addMethod(cm);
                }
            }
        }
        return enclosingClass;
    }
}

