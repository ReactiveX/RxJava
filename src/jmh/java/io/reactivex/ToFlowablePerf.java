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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.reactivestreams.Publisher;

import io.reactivex.functions.*;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class ToFlowablePerf {
    @Param({ "1", "1000", "1000000" })
    public int times;

    Maybe<Integer> flowable;

    Flowable<Integer> flowableInner;

    Observable<Integer> observable;

    Observable<Integer> observableInner;

    @Setup
    public void setup() {
        Integer[] array = new Integer[times];
        Arrays.fill(array, 777);

        Flowable<Integer> source = Flowable.fromArray(array);

        final BiFunction<Integer, Integer, Integer> second = new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) throws Exception {
                return b;
            }
        };

        flowable = source.reduce(second);

        flowableInner = source.concatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.range(1, 50).reduce(second).toFlowable();
            }
        });

        Observable<Integer> sourceObs = Observable.fromArray(array);

        observable = sourceObs.reduce(second).toObservable();

        observableInner = sourceObs.concatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) throws Exception {
                return Observable.range(1, 50).reduce(second).toObservable();
            }
        });
    }

    @Benchmark
    public Object flowable() {
        return flowable.blockingGet();
    }
    @Benchmark
    public Object flowableInner() {
        return flowableInner.blockingLast();
    }

    @Benchmark
    public Object observable() {
        return observable.blockingLast();
    }

    @Benchmark
    public Object observableInner() {
        return observableInner.blockingLast();
    }

    static volatile Object o;

    public static void main(String[] args) {
        ToFlowablePerf p = new ToFlowablePerf();
        p.times = 1000000;
        p.setup();

        for (int j = 0; j < 15; j++) {
            for (int i = 0; i < 600; i++) {
                o = p.flowable();
            }
            System.out.println("--- " + j);
        }
    }
}
