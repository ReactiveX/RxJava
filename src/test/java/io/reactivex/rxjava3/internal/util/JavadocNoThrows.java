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

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
import io.reactivex.rxjava3.testsupport.TestHelper;

/**
 * Scan the JavaDocs of the base classes and list those which do not have the {@code @throws} tag.
 * The lack is not an error by itself but worth looking at.
 */
public final class JavadocNoThrows {

    private JavadocNoThrows() {
        throw new IllegalArgumentException("No instances!");
    }

    public static void main(String[] args) throws Exception {
        for (Class<?> clazz : CLASSES) {
            String clazzName = clazz.getSimpleName();
            String packageName = clazz.getPackage().getName();
            File f = TestHelper.findSource(clazzName, packageName);

            List<String> lines = Files.readAllLines(f.toPath());

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();

                if (line.startsWith("/**")) {
                    boolean found = false;
                    for (int j = i + 1; j < lines.size(); j++) {

                        String line2 = lines.get(j).trim();
                        if (line2.startsWith("public")) {
                            if (line2.endsWith("() {")) {
                                found = true;
                            }
                            break;
                        }
                        if (line2.startsWith("* @throws")) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        System.out.printf(" at %s.%s.method(%s.java:%s)%n%n", packageName, clazzName, clazzName, i + 1);
                    }
                }
            }
        }
    }

    static final Class<?>[] CLASSES = {
            Flowable.class, Observable.class, Maybe.class, Single.class, Completable.class, ParallelFlowable.class
    };
}
