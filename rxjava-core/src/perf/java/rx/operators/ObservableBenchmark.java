package rx.operators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.openjdk.jmh.annotations.*;

import org.openjdk.jmh.logic.BlackHole;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;

public class ObservableBenchmark {

    @GenerateMicroBenchmark
    public void measureBaseline(BlackHole bh, Input input) {
        for (Integer value : input.values) {
            bh.consume(IDENTITY_FUNCTION.call(value));
        }
    }

    @GenerateMicroBenchmark
    public void measureMap(Input input) throws InterruptedException {
        input.observable.lift(MAP_OPERATOR).subscribe(input.observer);

        input.awaitCompletion();
    }

    private static final Func1<Integer, Integer> IDENTITY_FUNCTION = new Func1<Integer, Integer>() {
        @Override
        public Integer call(Integer value) {
            return value;
        }
    };

    private static final Operator<Integer, Integer> MAP_OPERATOR = new OperatorMap<Integer, Integer>(IDENTITY_FUNCTION);

    @State(Scope.Thread)
    public static class Input {

        @Param({"1", "1024", "1048576"})
        public int size;

        public Collection<Integer> values;
        public Observable<Integer> observable;
        public Observer<Integer> observer;

        private CountDownLatch latch;

        @Setup
        public void setup() {
            values = new ArrayList<Integer>();
            for(int i = 0; i < size; i ++) {
                values.add(i);
            }

            observable = Observable.create(new OnSubscribe<Integer>() {
                @Override
                public void call(Subscriber<? super Integer> o) {
                    for (Integer value : values) {
                        if (o.isUnsubscribed())
                            return;
                        o.onNext(value);
                    }
                    o.onCompleted();
                }
            });

            final BlackHole bh = new BlackHole();
            latch = new CountDownLatch(1);

            observer = new Observer<Integer>() {
                @Override
                public void onCompleted() {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable e) {
                    throw new RuntimeException(e);
                }

                @Override
                public void onNext(Integer value) {
                    bh.consume(value);
                }
            };

        }

        public void awaitCompletion() throws InterruptedException {
            latch.await();
        }
    }

}
