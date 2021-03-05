/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.internal.util;

import java.lang.reflect.*;
import java.util.*;

import org.reactivestreams.*;

import com.google.common.base.Strings;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Observable;

/**
 * Compare method argument naming across base classes.
 * This is not a full test because some naming mismatch is legitimate, such as singular in Maybe/Single and
 * plural in Flowable/Observable
 */
public final class OperatorArgumentNaming {

    private OperatorArgumentNaming() {
        throw new IllegalStateException("No instances!");
    }

    /** Classes to compare with each other. */
    static final Class<?>[] CLASSES = { Flowable.class, Observable.class, Maybe.class, Single.class, Completable.class };

    /** Types that refer to a reactive type and is generally matching the parent class; for comparison, these have to be unified. */
    static final Set<Class<?>> BASE_TYPE_SET = new HashSet<>(Arrays.asList(
            Flowable.class, Publisher.class, Subscriber.class, FlowableSubscriber.class,
            Observable.class, ObservableSource.class, Observer.class,
            Maybe.class, MaybeSource.class, MaybeObserver.class,
            Single.class, SingleSource.class, SingleObserver.class,
            Completable.class, CompletableSource.class, CompletableObserver.class
    ));

    public static void main(String[] args) {
        // className -> methodName -> overloads -> arguments
        Map<String, Map<String, List<List<ArgumentNameAndType>>>> map = new HashMap<>();

        for (Class<?> clazz : CLASSES) {
            Map<String, List<List<ArgumentNameAndType>>> classMethods = map.computeIfAbsent(clazz.getSimpleName(), v -> new HashMap<>());
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getDeclaringClass() == clazz && method.getParameterCount() != 0) {
                    List<List<ArgumentNameAndType>> overloads = classMethods.computeIfAbsent(method.getName(), v -> new ArrayList<>());

                    List<ArgumentNameAndType> overload = new ArrayList<>();
                    overloads.add(overload);

                    for (Parameter param : method.getParameters()) {
                        String typeName;
                        Class<?> type = param.getType();
                        if (type.isArray()) {
                            Class<?> componentType = type.getComponentType();
                            if (BASE_TYPE_SET.contains(componentType)) {
                                typeName = "BaseType";
                            } else {
                                typeName = type.getComponentType().getSimpleName() + "[]";
                            }
                        } else
                        if (BASE_TYPE_SET.contains(type)) {
                            typeName = "BaseType";
                        } else {
                            typeName = type.getSimpleName();
                        }
                        String name = param.getName();
                        if (name.equals("bufferSize") || name.equals("prefetch") || name.equals("capacityHint")) {
                            name = "bufferSize|prefetch|capacityHint";
                        }
                        if (name.equals("subscriber") || name.equals("observer")) {
                            name = "subscriber|observer";
                        }
                        if (name.contains("onNext")) {
                            name = name.replace("onNext", "onNext|onSuccess");
                        } else
                        if (name.contains("onSuccess")) {
                            name = name.replace("onSuccess", "onNext|onSuccess");
                        }
                        overload.add(new ArgumentNameAndType(typeName, name));
                    }
                }
            }
        }

        int counter = 0;

        for (int i = 0; i < CLASSES.length - 1; i++) {
            String firstName = CLASSES[i].getSimpleName();
            Map<String, List<List<ArgumentNameAndType>>> firstClassMethods = map.get(firstName);
            for (int j = i + 1; j < CLASSES.length; j++) {
                String secondName = CLASSES[j].getSimpleName();
                Map<String, List<List<ArgumentNameAndType>>> secondClassMethods = map.get(secondName);

                for (Map.Entry<String, List<List<ArgumentNameAndType>>> methodOverloadsFirst : firstClassMethods.entrySet()) {

                    List<List<ArgumentNameAndType>> methodOverloadsSecond = secondClassMethods.get(methodOverloadsFirst.getKey());

                    if (methodOverloadsSecond != null) {
                        for (List<ArgumentNameAndType> overloadFirst : methodOverloadsFirst.getValue()) {
                            for (List<ArgumentNameAndType> overloadSecond : methodOverloadsSecond) {
                                if (overloadFirst.size() == overloadSecond.size()) {
                                    // Argument types match?
                                    boolean match = true;
                                    for (int k = 0; k < overloadFirst.size(); k++) {
                                        if (!overloadFirst.get(k).type.equals(overloadSecond.get(k).type)) {
                                            match = false;
                                            break;
                                        }
                                    }
                                    // Argument names match?
                                    if (match) {
                                        for (int k = 0; k < overloadFirst.size(); k++) {
                                            if (!overloadFirst.get(k).name.equals(overloadSecond.get(k).name)) {
                                                System.out.print("Argument naming mismatch #");
                                                System.out.println(++counter);

                                                System.out.print("  ");
                                                System.out.print(Strings.padEnd(firstName, Math.max(firstName.length(), secondName.length()) + 1, ' '));
                                                System.out.print(methodOverloadsFirst.getKey());
                                                System.out.print("  ");
                                                System.out.println(overloadFirst);

                                                System.out.print("  ");
                                                System.out.print(Strings.padEnd(secondName, Math.max(firstName.length(), secondName.length()) + 1, ' '));
                                                System.out.print(methodOverloadsFirst.getKey());
                                                System.out.print("  ");
                                                System.out.println(overloadSecond);
                                                System.out.println();
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static final class ArgumentNameAndType {
        final String type;
        final String name;

        ArgumentNameAndType(String type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public String toString() {
            return type + " " + name;
        }
    }
}
