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

import javassist.ClassPool;
import javassist.CtClass;

import rx.util.functions.FunctionLanguageAdaptor;

/**
 * Java Executable that performs bytecode rewriting at compile-time.
 * It accepts 2 args: dynamic language and dir to store result output classfiles
 */
public class ClassPathBasedRunner {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage : Expects 2 args: (Language, File to place classfiles)");
            System.out.println("Currently supported languages: Groovy/Clojure/JRuby");
            System.exit(1);
        }
        String lang = args[0];
        File dir = new File(args[1]);
        System.out.println("Using dir : " + dir + " for outputting classfiles");
        System.out.println("Looking for Adaptor for : " + lang);
        String className = "rx.lang." + lang.toLowerCase() + "." + lang + "Adaptor";
        try {
            ClassPool pool = ClassPool.getDefault();

            Class<?> adaptorClass = Class.forName(className);
            System.out.println("Found Adaptor : " + adaptorClass);
            FunctionLanguageAdaptor adaptor = (FunctionLanguageAdaptor) adaptorClass.newInstance();

            Func1Generator func1Generator = new Func1Generator(pool, adaptor);
            CtClass dynamicFunc1Class = func1Generator.createDynamicFunc1Class();
            writeClassFile(dynamicFunc1Class, dir);
            pool.appendPathList(dir.getCanonicalPath());

            ObservableRewriter rewriter = new ObservableRewriter(pool, adaptor); 
            for (Class<?> observableClass: getObservableClasses()) {
                CtClass rewrittenClass = rewriter.addMethods(observableClass);
                writeClassFile(rewrittenClass, dir);
            }
        } catch (ClassNotFoundException ex) {
            System.out.println("Did not find adaptor class : " + className);
            System.exit(1);
        } catch (InstantiationException ex) {
            System.out.println("Reflective constuctor on : " + className + " failed");
            System.exit(1);
        } catch (IllegalAccessException ex) {
            System.out.println("Access to constructor on : " + className + " failed");
            System.exit(1);
        } catch (Exception ex) {
            System.out.println("Exception : " + ex.getMessage());
            System.exit(1);
        }
    }

    protected static void writeClassFile(CtClass clazz, File dir) {
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

    private static List<Class<?>> getObservableClasses() {
        List<Class<?>> observableClasses = new ArrayList<Class<?>>();
        observableClasses.add(rx.Observable.class);
        observableClasses.add(rx.observables.BlockingObservable.class);
        return observableClasses;
    }
}