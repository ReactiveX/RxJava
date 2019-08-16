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

package io.reactivex.rxjava3.core;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.functions.Function;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class BinaryFlatMapPerf {
    @Param({ "1", "1000", "1000000" })
    public int times;

    Flowable<Integer> singleFlatMapPublisher;

    Flowable<Integer> singleFlatMapHidePublisher;

    Flowable<Integer> singleFlattenAsPublisher;

    Flowable<Integer> maybeFlatMapPublisher;

    Flowable<Integer> maybeFlatMapHidePublisher;

    Flowable<Integer> maybeFlattenAsPublisher;

    Flowable<Integer> completableFlatMapPublisher;

    Flowable<Integer> completableFlattenAsPublisher;

    Observable<Integer> singleFlatMapObservable;

    Observable<Integer> singleFlatMapHideObservable;

    Observable<Integer> singleFlattenAsObservable;

    Observable<Integer> maybeFlatMapObservable;

    Observable<Integer> maybeFlatMapHideObservable;

    Observable<Integer> maybeFlattenAsObservable;

    Observable<Integer> completableFlatMapObservable;

    Observable<Integer> completableFlattenAsObservable;

    @Setup
    public void setup() {

        // --------------------------------------------------------------------------

        final Integer[] array = new Integer[times];
        Arrays.fill(array, 777);

        final List<Integer> list = Arrays.asList(array);

        final Flowable<Integer> arrayFlowable = Flowable.fromArray(array);
        final Flowable<Integer> arrayFlowableHide = Flowable.fromArray(array).hide();
        final Flowable<Integer> listFlowable = Flowable.fromIterable(list);

        final Observable<Integer> arrayObservable = Observable.fromArray(array);
        final Observable<Integer> arrayObservableHide = Observable.fromArray(array).hide();
        final Observable<Integer> listObservable = Observable.fromIterable(list);

        // --------------------------------------------------------------------------

        singleFlatMapPublisher = Single.just(1).flatMapPublisher(new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v)
                    throws Exception {
                return arrayFlowable;
            }
        });

        singleFlatMapHidePublisher = Single.just(1).flatMapPublisher(new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v)
                    throws Exception {
                return arrayFlowableHide;
            }
        });

        singleFlattenAsPublisher = Single.just(1).flattenAsFlowable(new Function<Integer, Iterable<? extends Integer>>() {
            @Override
            public Iterable<? extends Integer> apply(Integer v)
                    throws Exception {
                return list;
            }
        });

        maybeFlatMapPublisher = Maybe.just(1).flatMapPublisher(new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v)
                    throws Exception {
                return arrayFlowable;
            }
        });

        maybeFlatMapHidePublisher = Maybe.just(1).flatMapPublisher(new Function<Integer, Publisher<? extends Integer>>() {
            @Override
            public Publisher<? extends Integer> apply(Integer v)
                    throws Exception {
                return arrayFlowableHide;
            }
        });

        maybeFlattenAsPublisher = Maybe.just(1).flattenAsFlowable(new Function<Integer, Iterable<? extends Integer>>() {
            @Override
            public Iterable<? extends Integer> apply(Integer v)
                    throws Exception {
                return list;
            }
        });

        completableFlatMapPublisher = Completable.complete().andThen(listFlowable);

        completableFlattenAsPublisher = Completable.complete().andThen(arrayFlowable);

        // --------------------------------------------------------------------------

        singleFlatMapObservable = Single.just(1).flatMapObservable(new Function<Integer, Observable<? extends Integer>>() {
            @Override
            public Observable<? extends Integer> apply(Integer v)
                    throws Exception {
                return arrayObservable;
            }
        });

        singleFlatMapHideObservable = Single.just(1).flatMapObservable(new Function<Integer, Observable<? extends Integer>>() {
            @Override
            public Observable<? extends Integer> apply(Integer v)
                    throws Exception {
                return arrayObservableHide;
            }
        });

        singleFlattenAsObservable = Single.just(1).flattenAsObservable(new Function<Integer, Iterable<? extends Integer>>() {
            @Override
            public Iterable<? extends Integer> apply(Integer v)
                    throws Exception {
                return list;
            }
        });

        maybeFlatMapObservable = Maybe.just(1).flatMapObservable(new Function<Integer, Observable<? extends Integer>>() {
            @Override
            public Observable<? extends Integer> apply(Integer v)
                    throws Exception {
                return arrayObservable;
            }
        });

        maybeFlatMapHideObservable = Maybe.just(1).flatMapObservable(new Function<Integer, Observable<? extends Integer>>() {
            @Override
            public Observable<? extends Integer> apply(Integer v)
                    throws Exception {
                return arrayObservableHide;
            }
        });

        maybeFlattenAsObservable = Maybe.just(1).flattenAsObservable(new Function<Integer, Iterable<? extends Integer>>() {
            @Override
            public Iterable<? extends Integer> apply(Integer v)
                    throws Exception {
                return list;
            }
        });

        completableFlatMapObservable = Completable.complete().andThen(listObservable);

        completableFlattenAsObservable = Completable.complete().andThen(arrayObservable);

    }

    @Benchmark
    public void singleFlatMapPublisher(Blackhole bh) {
        singleFlatMapPublisher.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void singleFlatMapHidePublisher(Blackhole bh) {
        singleFlatMapHidePublisher.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void singleFlattenAsPublisher(Blackhole bh) {
        singleFlattenAsPublisher.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void maybeFlatMapPublisher(Blackhole bh) {
        maybeFlatMapPublisher.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void maybeFlatMapHidePublisher(Blackhole bh) {
        maybeFlatMapHidePublisher.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void maybeFlattenAsPublisher(Blackhole bh) {
        maybeFlattenAsPublisher.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void completableFlatMapPublisher(Blackhole bh) {
        completableFlatMapPublisher.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void completableFlattenAsPublisher(Blackhole bh) {
        completableFlattenAsPublisher.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void singleFlatMapObservable(Blackhole bh) {
        singleFlatMapObservable.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void singleFlatMapHideObservable(Blackhole bh) {
        singleFlatMapHideObservable.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void singleFlattenAsObservable(Blackhole bh) {
        singleFlattenAsObservable.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void maybeFlatMapObservable(Blackhole bh) {
        maybeFlatMapObservable.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void maybeFlatMapHideObservable(Blackhole bh) {
        maybeFlatMapHideObservable.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void maybeFlattenAsObservable(Blackhole bh) {
        maybeFlattenAsObservable.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void completableFlatMapObservable(Blackhole bh) {
        completableFlatMapObservable.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void completableFlattenAsObservable(Blackhole bh) {
        completableFlattenAsObservable.subscribe(new PerfConsumer(bh));
    }
}
