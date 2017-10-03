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

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Publisher;

import io.reactivex.functions.Function;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class XMapYPerf {
    @Param({ "1", "10", "100", "1000", "10000", "100000", "1000000" })
    public int times;

    Flowable<Integer> flowFlatMapIterable1;

    Flowable<Integer> flowFlatMapIterable0;

    Flowable<Integer> flowFlatMapFlowable0;

    Flowable<Integer> flowFlatMapFlowable1;

    Flowable<Integer> flowFlatMapSingle1;

    Flowable<Integer> flowFlatMapMaybe1;

    Flowable<Integer> flowFlatMapMaybe0;

    Completable flowFlatMapCompletable0;

    // oooooooooooooooooooooooooooooooooooooooooo

    Flowable<Integer> flowFlatMapSingleAsFlow1;

    Flowable<Integer> flowFlatMapMaybeAsFlow1;

    Flowable<Integer> flowFlatMapMaybeAsFlow0;

    Flowable<Integer> flowFlatMapCompletableAsFlow0;

    Flowable<Integer> flowFlatMapIterableAsFlow1;

    Flowable<Integer> flowFlatMapIterableAsFlow0;

    // -----------------------------------------------------------------

    Observable<Integer> obsFlatMapIterable0;

    Observable<Integer> obsFlatMapIterable1;

    Observable<Integer> obsFlatMapObservable0;

    Observable<Integer> obsFlatMapObservable1;

    Observable<Integer> obsFlatMapSingle1;

    Observable<Integer> obsFlatMapMaybe1;

    Observable<Integer> obsFlatMapMaybe0;

    Completable obsFlatMapCompletable0;

    // oooooooooooooooooooooooooooooooooooooooooo

    Observable<Integer> obsFlatMapSingleAsObs1;

    Observable<Integer> obsFlatMapMaybeAsObs1;

    Observable<Integer> obsFlatMapMaybeAsObs0;

    Observable<Integer> obsFlatMapCompletableAsObs0;

    Observable<Integer> obsFlatMapIterableAsObs1;

    Observable<Integer> obsFlatMapIterableAsObs0;


    @Setup
    public void setup() {
        Integer[] values = new Integer[times];
        Arrays.fill(values, 777);

        Flowable<Integer> fsource = Flowable.fromArray(values);

        flowFlatMapFlowable1 = fsource.flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.just(v);
            }
        });

        flowFlatMapFlowable0 = fsource.flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.empty();
            }
        });

        flowFlatMapSingle1 = fsource.flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.just(v);
            }
        });

        flowFlatMapMaybe1 = fsource.flatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v);
            }
        });

        flowFlatMapMaybe0 = fsource.flatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.empty();
            }
        });

        flowFlatMapCompletable0 = fsource.flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        });

        flowFlatMapIterable1 = fsource.flatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Collections.singletonList(v);
            }
        });

        flowFlatMapIterable0 = fsource.flatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Collections.<Integer>emptyList();
            }
        });


        flowFlatMapSingle1 = fsource.flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.just(v);
            }
        });

        flowFlatMapMaybe1 = fsource.flatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v);
            }
        });

        flowFlatMapMaybe0 = fsource.flatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.empty();
            }
        });

        flowFlatMapCompletable0 = fsource.flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        });

        // ooooooooooooooooooooooooo

        flowFlatMapSingleAsFlow1 = fsource.flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Single.just(v).toFlowable();
            }
        });

        flowFlatMapMaybeAsFlow1 = fsource.flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v).toFlowable();
            }
        });

        flowFlatMapMaybeAsFlow0 = fsource.flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Maybe.<Integer>empty().toFlowable();
            }
        });

        flowFlatMapCompletableAsFlow0 = fsource.flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Completable.complete().<Integer>toFlowable();
            }
        });

        flowFlatMapIterableAsFlow1 = fsource.flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.fromIterable(Collections.singletonList(v));
            }
        });

        flowFlatMapIterableAsFlow0 = fsource.flatMap(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.fromIterable(Collections.<Integer>emptyList());
            }
        });


        // -------------------------------------------------------------------

        Observable<Integer> osource = Observable.fromArray(values);

        obsFlatMapObservable1 = osource.flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) throws Exception {
                return Observable.just(v);
            }
        });

        obsFlatMapObservable0 = osource.flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) throws Exception {
                return Observable.empty();
            }
        });

        obsFlatMapSingle1 = osource.flatMapSingle(new Function<Integer, SingleSource<Integer>>() {
            @Override
            public SingleSource<Integer> apply(Integer v) throws Exception {
                return Single.just(v);
            }
        });

        obsFlatMapMaybe1 = osource.flatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v);
            }
        });

        obsFlatMapMaybe0 = osource.flatMapMaybe(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.empty();
            }
        });


        obsFlatMapCompletable0 = osource.flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        });

        obsFlatMapIterable1 = osource.flatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Collections.singletonList(v);
            }
        });

        obsFlatMapIterable0 = osource.flatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Collections.<Integer>emptyList();
            }
        });

        // ooooooooooooooooooooooooo

        obsFlatMapSingleAsObs1 = osource.flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) throws Exception {
                return Single.just(v).toObservable();
            }
        });

        obsFlatMapMaybeAsObs1 = osource.flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v).toObservable();
            }
        });

        obsFlatMapMaybeAsObs0 = osource.flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) throws Exception {
                return Maybe.<Integer>empty().toObservable();
            }
        });

        obsFlatMapCompletableAsObs0 = osource.flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) throws Exception {
                return Completable.complete().<Integer>toObservable();
            }
        });

        obsFlatMapIterableAsObs1 = osource.flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) throws Exception {
                return Observable.fromIterable(Collections.singletonList(v));
            }
        });

        obsFlatMapIterableAsObs0 = osource.flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) throws Exception {
                return Observable.fromIterable(Collections.<Integer>emptyList());
            }
        });
    }

    @Benchmark
    public void flowFlatMapIterable1(Blackhole bh) {
        flowFlatMapIterable1.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void flowFlatMapIterable0(Blackhole bh) {
        flowFlatMapIterable0.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void flowFlatMapFlowable0(Blackhole bh) {
        flowFlatMapFlowable0.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void flowFlatMapFlowable1(Blackhole bh) {
        flowFlatMapFlowable1.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void flowFlatMapSingle1(Blackhole bh) {
        flowFlatMapSingle1.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void flowFlatMapMaybe1(Blackhole bh) {
        flowFlatMapMaybe1.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void flowFlatMapMaybe0(Blackhole bh) {
        flowFlatMapMaybe0.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void flowFlatMapCompletable0(Blackhole bh) {
        flowFlatMapCompletable0.subscribe(new PerfConsumer(bh));
    }

    // oooooooooooooooooooooooooooooooo

    @Benchmark
    public void flowFlatMapIterableAsFlow1(Blackhole bh) {
        flowFlatMapIterableAsFlow1.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void flowFlatMapIterableAsFlow0(Blackhole bh) {
        flowFlatMapIterableAsFlow0.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void flowFlatMapSingleAsFlow1(Blackhole bh) {
        flowFlatMapSingleAsFlow1.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void flowFlatMapMaybeAsFlow1(Blackhole bh) {
        flowFlatMapMaybeAsFlow1.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void flowFlatMapMaybeAsFlow0(Blackhole bh) {
        flowFlatMapMaybeAsFlow0.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void flowFlatMapCompletableAsFlow0(Blackhole bh) {
        flowFlatMapCompletableAsFlow0.subscribe(new PerfConsumer(bh));
    }

    // --------------------------------------------------------------------------------

    @Benchmark
    public void obsFlatMapIterable0(Blackhole bh) {
        obsFlatMapIterable0.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void obsFlatMapIterable1(Blackhole bh) {
        obsFlatMapIterable1.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void obsFlatMapObservable0(Blackhole bh) {
        obsFlatMapObservable0.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void obsFlatMapObservable1(Blackhole bh) {
        obsFlatMapObservable1.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void obsFlatMapSingle1(Blackhole bh) {
        obsFlatMapSingle1.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void obsFlatMapMaybe1(Blackhole bh) {
        obsFlatMapMaybe1.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void obsFlatMapMaybe0(Blackhole bh) {
        obsFlatMapMaybe0.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void obsFlatMapCompletable0(Blackhole bh) {
        obsFlatMapCompletable0.subscribe(new PerfConsumer(bh));
    }

    // oooooooooooooooooooooooooooooooo

    @Benchmark
    public void obsFlatMapIterableAsObs1(Blackhole bh) {
        obsFlatMapIterableAsObs1.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void obsFlatMapIterableAsObs0(Blackhole bh) {
        obsFlatMapIterableAsObs0.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void obsFlatMapSingleAsObs1(Blackhole bh) {
        obsFlatMapSingleAsObs1.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void obsFlatMapMaybeAsObs1(Blackhole bh) {
        obsFlatMapMaybeAsObs1.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void obsFlatMapMaybeAsObs0(Blackhole bh) {
        obsFlatMapMaybeAsObs0.subscribe(new PerfConsumer(bh));
    }

    @Benchmark
    public void obsFlatMapCompletableAsObs0(Blackhole bh) {
        obsFlatMapCompletableAsObs0.subscribe(new PerfConsumer(bh));
    }
}
