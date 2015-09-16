package io.reactivex;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import io.reactivex.schedulers.*;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class OperatorFlatMapPerf {

    @State(Scope.Thread)
    public static class Input extends InputWithIncrementingInteger {

        @Param({ "1", "1000", "1000000" })
        public int size;

        @Override
        public int getSize() {
            return size;
        }

    }

    @Benchmark
    public void flatMapIntPassthruSync(Input input) throws InterruptedException {
        input.observable.flatMap(Observable::just).subscribe(input.newSubscriber());
    }

    @Benchmark
    public void flatMapIntPassthruAsync(Input input) throws InterruptedException {
        LatchedObserver<Integer> latchedObserver = input.newLatchedObserver();
        input.observable.flatMap(i -> {
                return Observable.just(i).subscribeOn(Schedulers.computation());
        }).subscribe(latchedObserver);
        latchedObserver.latch.await();
    }

    @Benchmark
    public void flatMapTwoNestedSync(final Input input) throws InterruptedException {
        Observable.range(1, 2).flatMap(i -> {
                return input.observable;
        }).subscribe(input.newSubscriber());
    }

    // this runs out of memory currently
    //    @Benchmark
    //    public void flatMapTwoNestedAsync(final Input input) throws InterruptedException {
    //        Observable.range(1, 2).flatMap(new Func1<Integer, Observable<Integer>>() {
    //
    //            @Override
    //            public Observable<Integer> call(Integer i) {
    //                return input.observable.subscribeOn(Schedulers.computation());
    //            }
    //
    //        }).subscribe(input.observer);
    //    }

}