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

package io.reactivex;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

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
        input.observable.flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) {
                return Flowable.just(v);
            }
        }).subscribe(input.newSubscriber());
    }

    @Benchmark
    public void flatMapIntPassthruAsync(Input input) throws InterruptedException {
        PerfSubscriber latchedObserver = input.newLatchedObserver();
        input.observable.flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer i) {
                    return Flowable.just(i).subscribeOn(Schedulers.computation());
            }
        }).subscribe(latchedObserver);
        if (input.size == 1) {
            while (latchedObserver.latch.getCount() != 0) { }
        } else {
            latchedObserver.latch.await();
        }
    }

    @Benchmark
    public void flatMapTwoNestedSync(final Input input) throws InterruptedException {
        Flowable.range(1, 2).flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer i) {
                    return input.observable;
            }
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
