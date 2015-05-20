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

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.functions.Func1;
import rx.jmh.*;
import rx.schedulers.Schedulers;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class OperatorSerializePerf {

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
    public void noSerializationSingleThreaded(Input input) throws InterruptedException {
        LatchedObserver<Integer> o = input.newLatchedObserver();
        input.firehose.subscribe(o);
        o.latch.await();
    }

    @Benchmark
    public void serializedSingleStream(Input input) throws InterruptedException {
        LatchedObserver<Integer> o = input.newLatchedObserver();
        input.firehose.serialize().subscribe(o);
        o.latch.await();
    }

    @Benchmark
    public void serializedTwoStreamsHighlyContended(final Input input) throws InterruptedException {
        LatchedObserver<Integer> o = input.newLatchedObserver();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> s) {
                // break the contract here and concurrently onNext
                input.firehose.subscribeOn(Schedulers.computation()).unsafeSubscribe(s);
                input.firehose.subscribeOn(Schedulers.computation()).unsafeSubscribe(s);
                // they will be serialized after
            }

        }).serialize().subscribe(o);
        o.latch.await();
    }

    @State(Scope.Thread)
    public static class InputWithInterval extends InputWithIncrementingInteger implements Func1<Long, Integer> {

        @Param({ "1", "1000" })
        public int size;

        public Observable<Integer> interval;

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public void setup(Blackhole bh) {
            super.setup(bh);

            interval = Observable.interval(0, 1, TimeUnit.MILLISECONDS).take(size).map(this);
        }
        @Override
        public Integer call(Long t1) {
            return t1.intValue();
        }
    }

    @Benchmark
    public void serializedTwoStreamsSlightlyContended(final InputWithInterval input) throws InterruptedException {
        LatchedObserver<Integer> o = input.newLatchedObserver();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> s) {
                // break the contract here and concurrently onNext
                input.interval.subscribeOn(Schedulers.computation()).unsafeSubscribe(s);
                input.interval.subscribeOn(Schedulers.computation()).unsafeSubscribe(s);
                // they will be serialized after
            }

        }).serialize().subscribe(o);
        o.latch.await();
    }

    @Benchmark
    public void serializedTwoStreamsOneFastOneSlow(final InputWithInterval input) throws InterruptedException {
        LatchedObserver<Integer> o = input.newLatchedObserver();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> s) {
                // break the contract here and concurrently onNext
                input.interval.subscribeOn(Schedulers.computation()).unsafeSubscribe(s);
                input.firehose.subscribeOn(Schedulers.computation()).unsafeSubscribe(s);
                // they will be serialized after
            }

        }).serialize().subscribe(o);
        o.latch.await();
    }

}
