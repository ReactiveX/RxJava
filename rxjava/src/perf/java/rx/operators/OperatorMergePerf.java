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
package rx.operators;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import rx.Observable;
import rx.functions.Func1;
import rx.jmh.InputWithIncrementingInteger;
import rx.jmh.LatchedObserver;
import rx.schedulers.Schedulers;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class OperatorMergePerf {

    // flatMap
    @Benchmark
    public void oneStreamOfNthatMergesIn1(final InputMillion input) throws InterruptedException {
        Observable<Observable<Integer>> os = Observable.range(1, input.size).map(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer i) {
                return Observable.just(i);
            }

        });
        LatchedObserver<Integer> o = input.newLatchedObserver();
        Observable.merge(os).subscribe(o);
        o.latch.await();
    }

    // flatMap
    @Benchmark
    public void merge1SyncStreamOfN(final InputMillion input) throws InterruptedException {
        Observable<Observable<Integer>> os = Observable.just(1).map(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer i) {
                return Observable.range(0, input.size);
            }

        });
        LatchedObserver<Integer> o = input.newLatchedObserver();
        Observable.merge(os).subscribe(o);
        o.latch.await();
    }

    @Benchmark
    public void mergeNSyncStreamsOfN(final InputThousand input) throws InterruptedException {
        Observable<Observable<Integer>> os = input.observable.map(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer i) {
                return Observable.range(0, input.size);
            }

        });
        LatchedObserver<Integer> o = input.newLatchedObserver();
        Observable.merge(os).subscribe(o);
        o.latch.await();
    }

    @Benchmark
    public void mergeNAsyncStreamsOfN(final InputThousand input) throws InterruptedException {
        Observable<Observable<Integer>> os = input.observable.map(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer i) {
                return Observable.range(0, input.size).subscribeOn(Schedulers.computation());
            }

        });
        LatchedObserver<Integer> o = input.newLatchedObserver();
        Observable.merge(os).subscribe(o);
        o.latch.await();
    }

    @Benchmark
    public void mergeTwoAsyncStreamsOfN(final InputThousand input) throws InterruptedException {
        LatchedObserver<Integer> o = input.newLatchedObserver();
        Observable<Integer> ob = Observable.range(0, input.size).subscribeOn(Schedulers.computation());
        Observable.merge(ob, ob).subscribe(o);
        o.latch.await();
    }

    @Benchmark
    public void mergeNSyncStreamsOf1(final InputForMergeN input) throws InterruptedException {
        LatchedObserver<Integer> o = input.newLatchedObserver();
        Observable.merge(input.observables).subscribe(o);
        o.latch.await();
    }

    @State(Scope.Thread)
    public static class InputForMergeN {
        @Param({ "1", "100", "1000" })
        //        @Param({ "1000" })
        public int size;

        private Blackhole bh;
        List<Observable<Integer>> observables;

        @Setup
        public void setup(final Blackhole bh) {
            this.bh = bh;
            observables = new ArrayList<Observable<Integer>>();
            for (int i = 0; i < size; i++) {
                observables.add(Observable.just(i));
            }
        }

        public LatchedObserver<Integer> newLatchedObserver() {
            return new LatchedObserver<Integer>(bh);
        }
    }

    @State(Scope.Thread)
    public static class InputMillion extends InputWithIncrementingInteger {

        @Param({ "1", "1000", "1000000" })
        //        @Param({ "1000" })
        public int size;

        @Override
        public int getSize() {
            return size;
        }

    }

    @State(Scope.Thread)
    public static class InputThousand extends InputWithIncrementingInteger {

        @Param({ "1", "1000" })
        //        @Param({ "1000" })
        public int size;

        @Override
        public int getSize() {
            return size;
        }

    }
}
