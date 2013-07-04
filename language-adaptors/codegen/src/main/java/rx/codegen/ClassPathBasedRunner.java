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

import rx.util.functions.FunctionLanguageAdaptor;

public class ClassPathBasedRunner {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage : Expects 2 args: (Language, File to place classfiles)");
            System.out.println("Currently supported languages: Groovy/Clojure/JRuby");
            System.exit(1);
        }
        String lang = args[0];
        File dir = new File(args[1]);
        System.out.println("Looking for Adaptor for : " + lang);
        String className = "rx.lang." + lang.toLowerCase() + "." + lang + "Adaptor";
        try {
            Class<?> adaptorClass = Class.forName(className);
            System.out.println("Found Adaptor : " + adaptorClass);
            FunctionLanguageAdaptor adaptor = (FunctionLanguageAdaptor) adaptorClass.newInstance();

            CodeGenerator codeGen = new CodeGenerator(); 
            System.out.println("Using dir : " + dir + " for outputting classfiles");
            for (Class<?> observableClass: getObservableClasses()) {
                codeGen.addMethods(observableClass, adaptor, new File(args[1]));
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
        }
    }

    private static List<Class<?>> getObservableClasses() {
        List<Class<?>> observableClasses = new ArrayList<Class<?>>();
        observableClasses.add(rx.Observable.class);
        observableClasses.add(rx.observables.BlockingObservable.class);
        return observableClasses;
    }
}