/**
 * Copyright 2014 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.quasar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import co.paralleluniverse.fibers.instrument.MethodDatabase;
import co.paralleluniverse.fibers.instrument.SimpleSuspendableClassifier;
import co.paralleluniverse.fibers.instrument.SuspendableClassifier;

public class RxSuspendableClassifier implements SuspendableClassifier {
    private static final Set<String> CORE_PACKAGES = new HashSet<String>(Arrays.asList(new String[]{
        "rx", "rx.joins", "rx.observables", "rx.observers", "rx.operators", "rx.plugins", "rx.schedulers",
        "rx.subjects", "rx.subscriptions", "rx.functions", "rx.util", "rx.util.functions"
    }));

    private static final Set<String> EXCEPTIONS = new HashSet<String>(Arrays.asList(new String[]{
        "rx/observers/SynchronizedObserver",
        "rx/schedulers/AbstractSchedulerTests$ConcurrentObserverValidator",
    }));

    private static final Set<String> OBSERVER_METHODS = new HashSet<String>(Arrays.asList(new String[]{
        "onNext(Ljava/lang/Object;)V", "onCompleted()V", "onError(Ljava/lang/Throwable;)V"
    }));

    private static final String FUNCTION_METHOD = "call";

    @Override
    public MethodDatabase.SuspendableType isSuspendable(MethodDatabase db, String className, String superClassName, String[] interfaces, String methodName, String methodDesc, String methodSignature, String[] methodExceptions) {
        MethodDatabase.SuspendableType s = null;
        if (isCoreRx(className) && !EXCEPTIONS.contains(className)) {
            if (isObserverImplementation(db, className, superClassName, interfaces, methodName, methodDesc))
                s = MethodDatabase.SuspendableType.SUSPENDABLE;
            else if (isUtilFunction(db, className, superClassName, interfaces, methodName, methodDesc))
                s = MethodDatabase.SuspendableType.SUSPENDABLE;
        }
        if (s == null 
                && methodName.equals("call")
                && (className.startsWith("rx/functions/Func") || className.startsWith("rx/functions/Action")
                || className.startsWith("rx/util/functions/Func") || className.startsWith("rx/util/functions/Action"))) {
            s = MethodDatabase.SuspendableType.SUSPENDABLE_SUPER;
        }
        // System.out.println("-- " + className + "." + methodName + ": " + s);
        return s;
    }

    private boolean isCoreRx(String className) {
        return CORE_PACKAGES.contains(packageOf(className));
    }

    private static boolean isObserverImplementation(MethodDatabase db, String className, String superClassName, String[] interfaces, String methodName, String methodDesc) {
        return !className.equals("rx/Observer")
                && OBSERVER_METHODS.contains(methodName + methodDesc)
                && SimpleSuspendableClassifier.extendsOrImplements("rx/Observer", db, className, superClassName, interfaces);
    }

    private static boolean isUtilFunction(MethodDatabase db, String className, String superClassName, String[] interfaces, String methodName, String methodDesc) {
        return (className.startsWith("rx/functions/Functions") || className.startsWith("rx/functions/Actions")
                || className.startsWith("rx/util/functions/Functions") || className.startsWith("rx/util/functions/Actions"))
                && methodName.equals(FUNCTION_METHOD)
                && (SimpleSuspendableClassifier.extendsOrImplements("rx/functions/Function", db, className, superClassName, interfaces)
                || SimpleSuspendableClassifier.extendsOrImplements("rx/functions/Action", db, className, superClassName, interfaces)
                || SimpleSuspendableClassifier.extendsOrImplements("rx/util/functions/Function", db, className, superClassName, interfaces)
                || SimpleSuspendableClassifier.extendsOrImplements("rx/util/functions/Action", db, className, superClassName, interfaces));
    }

    private static String packageOf(String className) {
        return className.substring(0, Math.max(0, className.lastIndexOf('/'))).replace('/', '.');
    }
}
