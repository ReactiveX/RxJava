package rx.operators;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.util.functions.Func1;

import com.google.caliper.Benchmark;
import com.google.caliper.runner.CaliperMain;

public class ObservableBenchmark extends Benchmark {
    public void timeBaseline(int reps) {
        for (int i = 0; i < reps; i++) {
            observableOfInts.subscribe(newObserver());
        }
        awaitAllObservers();
    }

    public int timeMapIterate(long reps) {
        int x = 0;
        for (int i = 0; i < reps; i++) {
            for (int j = 0; j < intValues.length; j++) {
                // use hash code to make sure the JIT doesn't optimize too much and remove all of
                // our code.
                x |= ident.call(intValues[j]).hashCode();
            }
        }
        return x;
    }

    public void timeMap(long reps) {
        timeOperator(reps, new OperatorMap<Integer, Object>(ident));
    }

    /**************************************************************************
     * Below is internal stuff to avoid object allocation and time overhead of anything that isn't
     * being tested.
     **************************************************************************/

    public static void main(String[] args) {
        CaliperMain.main(ObservableBenchmark.class, args);
    }

    private void timeOperator(long reps, Operator<Object, Integer> op) {
        for (int i = 0; i < reps; i++) {
            observableOfInts.lift(op).subscribe(newObserver());
        }
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
        public void call(Observer<? super Integer> o) {
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
