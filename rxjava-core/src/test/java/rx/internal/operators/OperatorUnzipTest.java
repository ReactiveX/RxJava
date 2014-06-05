package rx.internal.operators;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.FuncN;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class OperatorUnzipTest {

    @Test
    public void test() {
        class Tuple<A, B> {
            A a;
            B b;

            public Tuple(A a, B b) {
                this.a = a;
                this.b = b;
            }
        }

        final AtomicInteger start = new AtomicInteger(1);
        Observable<Tuple<String, Integer>> source = Observable.defer(new Func0<Observable<Tuple<String, Integer>>>() {
            @Override
            public Observable<Tuple<String, Integer>> call() {
                return Observable.from(new Tuple<String, Integer>("a", start.getAndIncrement()),
                        new Tuple<String, Integer>("b", start.getAndIncrement()));
            }
        });

        Tuple<Observable<String>, Observable<Integer>> out = OperatorUnzip.unzip(source,
                new FuncN<Tuple<Observable<String>, Observable<Integer>>>() {
                    @Override
                    public Tuple<Observable<String>, Observable<Integer>> call(Object... args) {
                        return new Tuple<Observable<String>, Observable<Integer>>((Observable<String>) args[0],
                                (Observable<Integer>) args[1]);
                    }
                }, new Func1<Tuple<String, Integer>, String>() {
                    @Override
                    public String call(Tuple<String, Integer> t1) {
                        return t1.a;
                    }
                }, new Func1<Tuple<String, Integer>, Integer>() {
                    @Override
                    public Integer call(Tuple<String, Integer> t1) {
                        return t1.b;
                    }
                });

        TestSubscriber<String> sub0 = new TestSubscriber<String>();
        out.a.subscribe(sub0);
        TestSubscriber<Integer> sub1 = new TestSubscriber<Integer>();
        out.b.subscribe(sub1);

        assertEquals(Arrays.asList("a", "b"), sub0.getOnNextEvents());
        assertEquals(Arrays.asList(1, 2), sub1.getOnNextEvents());
    }
}
