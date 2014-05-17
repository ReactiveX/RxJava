/**
 * Copyright 2014 Netflix, Inc.
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
package rx.archive.performance;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import rx.functions.Func1;

public class TestChainPerformance {

    public static void main(String[] args) {
        TestChainPerformance test = new TestChainPerformance();
        Integer[] values = new Integer[100001];
        for (int i = 0; i < values.length; i++) {
            values[i] = i;
        }

        try {
            for (int i = 0; i < 100; i++) {
                System.out.println("-------------------------------");
                test.runChained(values);
                test.runComposed(values);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void runComposed(final Integer[] values) throws Exception {
        long start = System.nanoTime();

        Callable<Integer> c = null;
        for (int i = 0; i < 250; i++) {
            final Callable<Integer> previousC = c;
            c = new Callable<Integer>() {

                @Override
                public Integer call() throws Exception {
                    MathFunction f = new MathFunction();
                    int sum = 0;
                    for (int v : values) {
                        sum += f.call(v);
                    }
                    if (previousC != null) {
                        sum += previousC.call();
                    }
                    return sum;
                }

            };
        }

        int sum = c.call();

        long end = System.nanoTime();
        System.out.println("Composed => Sum: " + sum + " Time: " + ((double) (end - start)) / 1000 / 1000 + "ms");
    }

    public void runChained(Integer[] values) {
        long start = System.nanoTime();
        int sum = 0;

        ArrayList<Func1<Integer, Integer>> functions = new ArrayList<Func1<Integer, Integer>>();
        for (int i = 0; i < 250; i++) {
            functions.add(new MathFunction());
        }

        for (int v : values) {
            for (Func1<Integer, Integer> f : functions) {
                sum += f.call(v);
            }
        }

        long end = System.nanoTime();
        System.out.println("Iterative => Sum: " + sum + " Time: " + ((double) (end - start)) / 1000 / 1000 + "ms");
    }

    private static class MathFunction implements Func1<Integer, Integer> {

        @Override
        public Integer call(Integer t1) {
            return t1 + 1;
        }

    }
}
