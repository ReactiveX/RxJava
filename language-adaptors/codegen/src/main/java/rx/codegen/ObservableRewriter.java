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
        List<ConflictingMethodGroup> conflictingMethodGroups = getConflictingMethodGroups(enclosingClass);
        for (CtMethod method : enclosingClass.getMethods()) {
            MethodRewriter methodRewriter = MethodRewriter.from(enclosingClass, method, adaptor, conflictingMethodGroups);
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

    /**
     * Iterate through all the methods in the given class and find methods which would collide
     * if they were rewritten with dynamic wrappers.  
     * @param enclosingClass class to find conflicting methods in
     * @return list of {@code ConflictingMethodGroup}s found in given class
     */
    private List<ConflictingMethodGroup> getConflictingMethodGroups(CtClass enclosingClass) throws Exception {
        List<ConflictingMethodGroup> groups = new ArrayList<ConflictingMethodGroup>();

        Map<Integer, List<CtMethod>> methodHashes = new HashMap<Integer, List<CtMethod>>();
        for (CtMethod m: enclosingClass.getMethods()) {
            if (enclosingClass.equals(m.getDeclaringClass())) {
                Integer hash = getHash(m);
                if (methodHashes.containsKey(hash)) {
                    List<CtMethod> existingMethods = methodHashes.get(hash);
                    existingMethods.add(m);
                    methodHashes.put(hash, existingMethods);
                } else {
                    List<CtMethod> newMethodList = new ArrayList<CtMethod>();
                    newMethodList.add(m);
                    methodHashes.put(hash, newMethodList);
                }
            }
        }

        for (Integer key: methodHashes.keySet()) {
            List<CtMethod> methodsWithSameHash = methodHashes.get(key);
            if (methodsWithSameHash.size() > 1) {
                ConflictingMethodGroup group = new ConflictingMethodGroup(methodsWithSameHash);
                groups.add(group);
            }
        }

        return groups;
    }

    //we care about collisions post-rewrite.  So take a hash over method name and arguments
    //where every Rx {@code Action} or Rx {@code Function} is hashed to the same value
    // in this case {@code Function}
    private int getHash(CtMethod m) throws Exception {
        final String delimiter = ",";

        final StringBuffer buffer = new StringBuffer();
        final String name = m.getName();
        buffer.append(name);
        buffer.append(delimiter);
        for (CtClass argClass: m.getParameterTypes()) {
            if (MethodRewriter.isRxActionType(argClass) || MethodRewriter.isRxFunctionType(argClass)) {
                buffer.append("RxFunc");
            } else {
                buffer.append(argClass.getName());
            }
            buffer.append(delimiter);
        }
        return buffer.toString().hashCode();
    }
}

