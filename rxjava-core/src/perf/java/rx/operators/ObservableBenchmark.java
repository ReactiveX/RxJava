package rx.operators;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.util.functions.Func1;

public class ObservableBenchmark {

    @GenerateMicroBenchmark
    public void timeBaseline() {
        observableOfInts.subscribe(newObserver());
        awaitAllObservers();
    }

    @GenerateMicroBenchmark
    public int timeMapIterate() {
        int x = 0;
        for (int j = 0; j < intValues.length; j++) {
            // use hash code to make sure the JIT doesn't optimize too much and remove all of
            // our code.
            x |= ident.call(intValues[j]).hashCode();
        }
        return x;
    }

    @GenerateMicroBenchmark
    public void timeMap() {
        timeOperator(new OperatorMap<Integer, Object>(ident));
    }

    /**************************************************************************
     * Below is internal stuff to avoid object allocation and time overhead of anything that isn't
     * being tested.
     * 
     * @throws RunnerException
     **************************************************************************/

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ObservableBenchmark.class.getName()+".*")
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    private void timeOperator(Operator<Object, Integer> op) {
        observableOfInts.lift(op).subscribe(newObserver());
        awaitAllObservers();
    }

    private final static AtomicInteger outstanding = new AtomicInteger(0);
    private final static CountDownLatch latch = new CountDownLatch(1);

    private static <T> Observer<T> newObserver() {
        outstanding.incrementAndGet();
        return new Observer<T>() {
            @Override
            public void onCompleted() {
                int left = outstanding.decrementAndGet();
                if (left == 0) {
                    latch.countDown();
                }
            }

            @Override
            public void onError(Throwable e) {
                int left = outstanding.decrementAndGet();
                if (left == 0) {
                    latch.countDown();
                }
            }

            @Override
            public void onNext(T t) {
                // do nothing
            }
        };
    }

    private static void awaitAllObservers() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            return;
        }
    }

    private static final Integer[] intValues = new Integer[1000];
    static {
        for (int i = 0; i < intValues.length; i++) {
            intValues[i] = i;
        }
    }

    private static final Observable<Integer> observableOfInts = Observable.create(new OnSubscribe<Integer>() {
        @Override
        public void call(Subscriber<? super Integer> o) {
            for (int i = 0; i < intValues.length; i++) {
                if (o.isUnsubscribed())
                    return;
                o.onNext(intValues[i]);
            }
            o.onCompleted();
        }
    });
    private static final Func1<Integer, Object> ident = new Func1<Integer, Object>() {
        @Override
        public Object call(Integer t) {
            return t;
        }
    };
}
