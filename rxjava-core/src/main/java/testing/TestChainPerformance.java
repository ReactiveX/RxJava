package testing;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import rx.util.functions.Func1;

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
