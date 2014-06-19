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

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.BlackHole;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.jmh.InputWithIncrementingIntegerTo1000000;
import rx.jmh.InputWithIncrementingIntegerTo128;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OperatorMergePerf {

    @GenerateMicroBenchmark
    public void mergeSynchronous(final InputWithIncrementingIntegerTo128 input) throws InterruptedException {
        Observable<Observable<Integer>> os = input.observable.map(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer i) {
                return Observable.range(0, input.size);
            }

        });
        TestSubscriber<Integer> ts = input.newSubscriber();
        Observable.merge(os).subscribe(ts);
        ts.awaitTerminalEvent();
    }

    @GenerateMicroBenchmark
    public void mergeAsynchronous(final InputWithIncrementingIntegerTo128 input) throws InterruptedException {
        Observable<Observable<Integer>> os = input.observable.map(new Func1<Integer, Observable<Integer>>() {

            @Override
            public Observable<Integer> call(Integer i) {
                return Observable.range(0, input.size).subscribeOn(Schedulers.computation());
            }

        });
        TestSubscriber<Integer> ts = input.newSubscriber();
        Observable.merge(os).subscribe(ts);
        ts.awaitTerminalEvent();
    }

    @GenerateMicroBenchmark
    public void mergeTwoAsyncStreams(final InputWithIncrementingIntegerTo1000000 input) throws InterruptedException {
        TestSubscriber<Integer> ts = input.newSubscriber();
        Observable<Integer> o = Observable.range(0, input.size).subscribeOn(Schedulers.computation());
        Observable.merge(o, o).subscribe(ts);
        ts.awaitTerminalEvent();
    }

    @GenerateMicroBenchmark
    public void mergeNStreams(final InputForMergeN input) throws InterruptedException {
        TestSubscriber<Integer> ts = input.newSubscriber();
        Observable.merge(input.observables).subscribe(ts);
        ts.awaitTerminalEvent();
    }

    @State(Scope.Thread)
    public static class InputForMergeN {
        @Param({ "1", "100", "1000" })
        public int size;

        private BlackHole bh;
        List<Observable<Integer>> observables;

        @Setup
        public void setup(final BlackHole bh) {
            this.bh = bh;
            observables = new ArrayList<Observable<Integer>>();
            for (int i = 0; i < size; i++) {
                observables.add(Observable.just(i));
            }
        }

        public TestSubscriber<Integer> newSubscriber() {
            return new TestSubscriber<Integer>(new Observer<Integer>() {
                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                    throw new RuntimeException(e);
                }

                @Override
                public void onNext(Integer value) {
                    bh.consume(value);
                }
            });
        }
    }

}
