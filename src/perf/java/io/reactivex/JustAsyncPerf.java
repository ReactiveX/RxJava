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
import org.openjdk.jmh.infra.Blackhole;

import io.reactivex.internal.schedulers.SingleScheduler;
import io.reactivex.schedulers.Schedulers;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class JustAsyncPerf {

    Flowable<Integer> subscribeOnFlowable;

    Flowable<Integer> observeOnFlowable;

    Flowable<Integer> pipelineFlowable;

    Observable<Integer> subscribeOnObservable;

    Observable<Integer> observeOnObservable;

    Observable<Integer> pipelineObservable;

    Single<Integer> observeOnSingle;

    Single<Integer> subscribeOnSingle;

    Single<Integer> pipelineSingle;

    Completable observeOnCompletable;

    Completable subscribeOnCompletable;

    Completable pipelineCompletable;

    Maybe<Integer> observeOnMaybe;

    Maybe<Integer> subscribeOnMaybe;

    Maybe<Integer> pipelineMaybe;

    @Setup
    public void setup() {

        Scheduler s = Schedulers.single();

        Scheduler s2 = new SingleScheduler();

        subscribeOnFlowable = Flowable.just(1).subscribeOn(s);

        observeOnFlowable = Flowable.just(1).observeOn(s);

        pipelineFlowable = Flowable.just(1).subscribeOn(s).observeOn(s2);

        // ----

        subscribeOnObservable = Observable.just(1).subscribeOn(s);

        observeOnObservable = Observable.just(1).observeOn(s);

        pipelineObservable = Observable.just(1).subscribeOn(s).observeOn(s2);

        // ----

        observeOnSingle = Single.just(1).observeOn(s);

        subscribeOnSingle = Single.just(1).subscribeOn(s);

        pipelineSingle = Single.just(1).subscribeOn(s).observeOn(s2);

        // ----

        observeOnCompletable = Completable.complete().observeOn(s);

        subscribeOnCompletable = Completable.complete().subscribeOn(s);

        pipelineCompletable = Completable.complete().subscribeOn(s).observeOn(s2);

        // ----

        observeOnMaybe = Maybe.just(1).observeOn(s);

        subscribeOnMaybe = Maybe.just(1).subscribeOn(s);

        pipelineMaybe = Maybe.just(1).subscribeOn(s).observeOn(s2);
    }

    @Benchmark
    public void subscribeOnFlowable(Blackhole bh) {
        subscribeOnFlowable.subscribeWith(new PerfAsyncConsumer(bh)).await(1);
    }

    @Benchmark
    public void observeOnFlowable(Blackhole bh) {
        observeOnFlowable.subscribeWith(new PerfAsyncConsumer(bh)).await(1);
    };

    @Benchmark
    public void pipelineFlowable(Blackhole bh) {
        pipelineFlowable.subscribeWith(new PerfAsyncConsumer(bh)).await(1);
    };

    @Benchmark
    public void subscribeOnObservable(Blackhole bh) {
        subscribeOnObservable.subscribeWith(new PerfAsyncConsumer(bh)).await(1);
    };

    @Benchmark
    public void observeOnObservable(Blackhole bh) {
        observeOnObservable.subscribeWith(new PerfAsyncConsumer(bh)).await(1);
    };

    @Benchmark
    public void pipelineObservable(Blackhole bh) {
        pipelineObservable.subscribeWith(new PerfAsyncConsumer(bh)).await(1);
    };

    @Benchmark
    public void observeOnSingle(Blackhole bh) {
        observeOnSingle.subscribeWith(new PerfAsyncConsumer(bh)).await(1);
    };

    @Benchmark
    public void subscribeOnSingle(Blackhole bh) {
        subscribeOnSingle.subscribeWith(new PerfAsyncConsumer(bh)).await(1);
    };

    @Benchmark
    public void pipelineSingle(Blackhole bh) {
        pipelineSingle.subscribeWith(new PerfAsyncConsumer(bh)).await(1);
    };

    @Benchmark
    public void observeOnCompletable(Blackhole bh) {
        observeOnCompletable.subscribeWith(new PerfAsyncConsumer(bh)).await(1);
    };

    @Benchmark
    public void subscribeOnCompletable(Blackhole bh) {
        subscribeOnCompletable.subscribeWith(new PerfAsyncConsumer(bh)).await(1);
    };

    @Benchmark
    public void pipelineCompletable(Blackhole bh) {
        pipelineCompletable.subscribeWith(new PerfAsyncConsumer(bh)).await(1);
    };

    @Benchmark
    public void observeOnMaybe(Blackhole bh) {
        observeOnMaybe.subscribeWith(new PerfAsyncConsumer(bh)).await(1);
    };

    @Benchmark
    public void subscribeOnMaybe(Blackhole bh) {
        subscribeOnMaybe.subscribeWith(new PerfAsyncConsumer(bh)).await(1);
    };

    @Benchmark
    public void pipelineMaybe(Blackhole bh) {
        pipelineMaybe.subscribeWith(new PerfAsyncConsumer(bh)).await(1);
    };

}
