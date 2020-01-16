/**
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

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;

/**
 * Generate a table of available operators across base classes in {@code Operator-Matrix.md}.
 * 
 * Should be run with the main project directory as working directory where the {@code docs}
 * folder is.
 */
public final class OperatorMatrixGenerator {

    private OperatorMatrixGenerator() {
        throw new IllegalStateException("No instances!");
    }

    static final String PRESENT = "![present](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_on.png)";
    static final String ABSENT = "![absent](https://raw.github.com/wiki/ReactiveX/RxJava/images/checkmark_off.png)";

    static final Class<?>[] CLASSES = {
            Flowable.class, Observable.class, Maybe.class, Single.class, Completable.class
    };

    public static void main(String[] args) throws IOException {
        Set<String> operatorSet = new HashSet<>();
        Map<Class<?>, Set<String>> operatorMap = new HashMap<>();

        for (Class<?> clazz : CLASSES) {
            Set<String> set = operatorMap.computeIfAbsent(clazz, c -> new HashSet<>());

            for (Method m : clazz.getMethods()) {
                String name = m.getName();
                if (!name.equals("bufferSize")
                        && m.getDeclaringClass() == clazz
                        && !m.isSynthetic()) {
                    operatorSet.add(m.getName());
                    set.add(m.getName());
                }
            }
        }

        List<String> sortedOperators = new ArrayList<>(operatorSet);
        sortedOperators.sort(Comparator.naturalOrder());

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get("docs", "Operator-Matrix.md"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
            out.print("Operator |");
            for (Class<?> clazz : CLASSES) {
                out.print(" `");
                out.print(clazz.getSimpleName());
                out.print("` |");
            }
            out.println();
            out.print("-----|");
            for (int i = 0; i < CLASSES.length; i++) {
                out.print(":---:|");
            }
            out.println();
            for (String operatorName : sortedOperators) {
                out.print("`");
                out.print(operatorName);
                out.print("`|");
                for (Class<?> clazz : CLASSES) {
                    if (operatorMap.get(clazz).contains(operatorName)) {
                        out.print(PRESENT);
                    } else {
                        out.print(ABSENT);
                    }
                    out.print("|");
                }
                out.println();
            }
        }
    }
}
