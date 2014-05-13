package rx.schedulers;

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.logic.BlackHole;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class MultipleObserversOnOneBusyObservable {

    @GenerateMicroBenchmark
    public void runAll(final Input input) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(input.numObservers);
        Observable<Integer> smallObservable = Observable.from(Arrays.asList(1, 2, 3, 4, 5));
        Observable<Integer> bigObservable = Observable.from(input.numbers);
        for(int o=0; o<input.numObservers; o++) {
            final int foo = o;
            Observable<Integer> theObservable = smallObservable;
            if(((o+1) % 4) != 0) {
                theObservable = bigObservable;
            }
            theObservable
                    .observeOn(input.schedulers[input.schedulerIndex])
                    .subscribe(new Observer<Integer>() {
                        @Override
                        public void onCompleted() {
                            latch.countDown();
                        }

                        @Override
                        public void onError(Throwable e) {
                            throw new RuntimeException(e);
                        }

                        @Override
                        public void onNext(Integer integer) {
                            input.blackHole.consume(integer*integer);
                        }
                    });
        }
        latch.await();
    }

    @State(Scope.Benchmark)
    public static class Input {
        private static final int NUM_INTEGERS=5000;
        private final int NUM_CORES = Runtime.getRuntime().availableProcessors();
        private int numObservers;
        private Scheduler[] schedulers;
        private List<Integer> numbers;
        private BlackHole blackHole = new BlackHole();

        @Param({ "0", "1", "2" })
        public int schedulerIndex;

        @Setup
        public void setup() {
            schedulers = new Scheduler[]{new EventLoopsScheduler(), new BalancingEventLoopScheduler(),
                    new ReroutingEventLoopScheduler()};
            numObservers = (int)(NUM_CORES * 10);
            numbers = new ArrayList<Integer>(NUM_INTEGERS);
            for(int n=0; n<NUM_INTEGERS; n++)
                numbers.add((int)(Math.random()*1000.0));
        }

    }

}
