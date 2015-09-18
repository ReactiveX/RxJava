/**
 * Copyright 2015 Netflix, Inc.
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

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import io.reactivex.schedulers.*;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class OperatorMergePerf {

    // flatMap
    @Benchmark
    public void oneStreamOfNthatMergesIn1(final InputMillion input) throws InterruptedException {
        Observable<Observable<Integer>> os = Observable.range(1, input.size).map(Observable::just);
        LatchedObserver<Integer> o = input.newLatchedObserver();
        Observable.merge(os).subscribe(o);

        if (input.size == 1) {
            while (o.latch.getCount() != 0);
        } else {
            o.latch.await();
        }
    }

    // flatMap
    @Benchmark
    public void merge1SyncStreamOfN(final InputMillion input) throws InterruptedException {
        Observable<Observable<Integer>> os = Observable.just(1).map(i -> {
                return Observable.range(0, input.size);
        });
        LatchedObserver<Integer> o = input.newLatchedObserver();
        Observable.merge(os).subscribe(o);
        
        if (input.size == 1) {
            while (o.latch.getCount() != 0);
        } else {
            o.latch.await();
        }
    }

    @Benchmark
    public void mergeNSyncStreamsOfN(final InputThousand input) throws InterruptedException {
        Observable<Observable<Integer>> os = input.observable.map(i -> {
                return Observable.range(0, input.size);
        });
        LatchedObserver<Integer> o = input.newLatchedObserver();
        Observable.merge(os).subscribe(o);
        if (input.size == 1) {
            while (o.latch.getCount() != 0);
        } else {
            o.latch.await();
        }
    }

    @Benchmark
    public void mergeNAsyncStreamsOfN(final InputThousand input) throws InterruptedException {
        Observable<Observable<Integer>> os = input.observable.map(i -> {
                return Observable.range(0, input.size).subscribeOn(Schedulers.computation());
        });
        LatchedObserver<Integer> o = input.newLatchedObserver();
        Observable.merge(os).subscribe(o);
        if (input.size == 1) {
            while (o.latch.getCount() != 0);
        } else {
            o.latch.await();
        }
    }

    @Benchmark
    public void mergeTwoAsyncStreamsOfN(final InputThousand input) throws InterruptedException {
        LatchedObserver<Integer> o = input.newLatchedObserver();
        Observable<Integer> ob = Observable.range(0, input.size).subscribeOn(Schedulers.computation());
        Observable.merge(ob, ob).subscribe(o);
        if (input.size == 1) {
            while (o.latch.getCount() != 0);
        } else {
            o.latch.await();
        }
    }

    @Benchmark
    public void mergeNSyncStreamsOf1(final InputForMergeN input) throws InterruptedException {
        LatchedObserver<Integer> o = input.newLatchedObserver();
        Observable.merge(input.observables).subscribe(o);
        if (input.size == 1) {
            while (o.latch.getCount() != 0);
        } else {
            o.latch.await();
        }
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
            observables = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                observables.add(Observable.just(i));
            }
        }

        public LatchedObserver<Integer> newLatchedObserver() {
            return new LatchedObserver<>(bh);
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