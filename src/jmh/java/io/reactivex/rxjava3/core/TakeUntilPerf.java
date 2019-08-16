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

import java.util.concurrent.*;

import org.openjdk.jmh.annotations.*;

import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.schedulers.Schedulers;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
@AuxCounters
public class TakeUntilPerf implements Consumer<Integer> {

    public volatile int items;

    static final int count = 10000;

    Flowable<Integer> flowable;

    Observable<Integer> observable;

    @Override
    public void accept(Integer t) throws Exception {
        items++;
    }

    @Setup
    public void setup() {

        flowable = Flowable.range(1, 1000 * 1000).takeUntil(Flowable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                int c = count;
                while (items < c) { }
                return 1;
            }
        }).subscribeOn(Schedulers.single()));

        observable = Observable.range(1, 1000 * 1000).takeUntil(Observable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                int c = count;
                while (items < c) { }
                return 1;
            }
        }).subscribeOn(Schedulers.single()));
    }

    @Benchmark
    public void flowable() {
        final CountDownLatch cdl = new CountDownLatch(1);

        flowable.subscribe(this, Functions.emptyConsumer(), new Action() {
            @Override
            public void run() throws Exception {
                cdl.countDown();
            }
        });

        while (cdl.getCount() != 0) { }
    }

    @Benchmark
    public void observable() {
        final CountDownLatch cdl = new CountDownLatch(1);

        observable.subscribe(this, Functions.emptyConsumer(), new Action() {
            @Override
            public void run() throws Exception {
                cdl.countDown();
            }
        });

        while (cdl.getCount() != 0) { }
    }
}
