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

package io.reactivex.internal.util;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Utility class that lists tests related to Observable that is not present in Flowable tests.
 */
public final class ObservableToFlowabeTestSync {
    private ObservableToFlowabeTestSync() {
        throw new IllegalStateException("No instances!");
    }

    static List<String> readAllLines(File f) {
        List<String> result = new ArrayList<String>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(f));
            try {
                String line;

                while ((line = in.readLine()) != null) {
                    result.add(line);
                }
            } finally {
                in.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    static void list(String basepath, String basepackage) throws Exception {
        File[] observables = new File(basepath + "observable/").listFiles();

        int count = 0;

        for (File f : observables) {
            if (!f.getName().endsWith(".java")) {
                continue;
            }
            Class<?> clazz = Class.forName(basepackage + "observable." + f.getName().replace(".java", ""));

            String cn = f.getName().replace(".java", "").replace("Observable", "Flowable");

            File f2 = new File(basepath + "/flowable/" + cn + ".java");

            if (!f2.exists()) {
                continue;
            }

            Class<?> clazz2 = Class.forName(basepackage + "flowable." + cn);

            Set<String> methods2 = new HashSet<String>();

            for (Method m : clazz2.getMethods()) {
                methods2.add(m.getName());
            }

            for (Method m : clazz.getMethods()) {
                if (!methods2.contains(m.getName()) && !methods2.contains(m.getName().replace("Observable", "Flowable"))) {
                    count++;
                    System.out.println();
                    System.out.print("java.lang.RuntimeException: missing > ");
                    System.out.println(m.getName());
                    System.out.print(" at ");
                    System.out.print(clazz.getName());
                    System.out.print(" (");
                    System.out.print(clazz.getSimpleName());
                    System.out.print(".java:");

                    List<String> lines = readAllLines(f);

                    int j = 1;
                    for (int i = 1; i <= lines.size(); i++) {
                        if (lines.get(i - 1).contains("public void " + m.getName() + "(")) {
                            j = i;
                        }
                    }
                    System.out.print(j);
                    System.out.println(")");

                    System.out.print(" at ");
                    System.out.print(clazz2.getName());
                    System.out.print(" (");
                    System.out.print(clazz2.getSimpleName());

                    lines = readAllLines(f2);

                    System.out.print(".java:");
                    System.out.print(lines.size() - 1);
                    System.out.println(")");
                }
            }
        }

        System.out.println();
        System.out.println(count);
    }

    public static void main(String[] args) throws Exception {
        list("src/test/java/io/reactivex/internal/operators/", "io.reactivex.internal.operators.");
//        list("src/test/java/io/reactivex/", "io.reactivex.");
    }
}
