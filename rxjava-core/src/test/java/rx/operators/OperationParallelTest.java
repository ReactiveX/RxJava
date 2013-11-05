package rx.operators;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class OperationParallelTest {

    @Test
    public void testParallel() {
        int NUM = 1000;
        final AtomicInteger count = new AtomicInteger();
        Observable.range(1, NUM).parallel(
                new Func1<Observable<Integer>, Observable<Integer[]>>() {

                    @Override
                    public Observable<Integer[]> call(Observable<Integer> o) {
                        return o.map(new Func1<Integer, Integer[]>() {

                            @Override
                            public Integer[] call(Integer t) {
                                return new Integer[] { t, t * 99 };
                            }

                        });
                    }
                }).toBlockingObservable().forEach(new Action1<Integer[]>() {

            @Override
            public void call(Integer[] v) {
                count.incrementAndGet();
                System.out.println("V: " + v[0] + " R: " + v[1] + " Thread: " + Thread.currentThread());
            }

        });

        // just making sure we finish and get the number we expect
        assertEquals(NUM, count.get());
    }
}
