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

import java.util.concurrent.*;

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
public class CallableAsyncPerf {

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

        Callable<Integer> c = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return 1;
            }
        };

        subscribeOnFlowable = Flowable.fromCallable(c).subscribeOn(s);

        observeOnFlowable = Flowable.fromCallable(c).observeOn(s);

        pipelineFlowable = Flowable.fromCallable(c).subscribeOn(s).observeOn(s2);

        // ----

        subscribeOnObservable = Observable.fromCallable(c).subscribeOn(s);

        observeOnObservable = Observable.fromCallable(c).observeOn(s);

        pipelineObservable = Observable.fromCallable(c).subscribeOn(s).observeOn(s2);

        // ----

        observeOnSingle = Single.fromCallable(c).observeOn(s);

        subscribeOnSingle = Single.fromCallable(c).subscribeOn(s);

        pipelineSingle = Single.fromCallable(c).subscribeOn(s).observeOn(s2);

        // ----

        observeOnCompletable = Completable.fromCallable(c).observeOn(s);

        subscribeOnCompletable = Completable.fromCallable(c).subscribeOn(s);

        pipelineCompletable = Completable.fromCallable(c).subscribeOn(s).observeOn(s2);

        // ----

        observeOnMaybe = Maybe.fromCallable(c).observeOn(s);

        subscribeOnMaybe = Maybe.fromCallable(c).subscribeOn(s);

        pipelineMaybe = Maybe.fromCallable(c).subscribeOn(s).observeOn(s2);
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
