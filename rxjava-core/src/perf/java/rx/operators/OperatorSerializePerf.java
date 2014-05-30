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

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.logic.BlackHole;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OperatorSerializePerf {

    public static void main(String[] args) {

    }

    @GenerateMicroBenchmark
    public void noSerializationSingleThreaded(Input input) {
        TestSubscriber<Long> ts = input.newSubscriber();
        input.firehose.subscribe(ts);
        ts.awaitTerminalEvent();
    }

    @GenerateMicroBenchmark
    public void serializedSingleStream(Input input) {
        TestSubscriber<Long> ts = input.newSubscriber();
        input.firehose.serialize().subscribe(ts);
        ts.awaitTerminalEvent();
    }

    @GenerateMicroBenchmark
    public void serializedTwoStreamsSlightlyContended(final Input input) {
        TestSubscriber<Long> ts = input.newSubscriber();
        Observable.create(new OnSubscribe<Long>() {

            @Override
            public void call(Subscriber<? super Long> s) {
                // break the contract here and concurrently onNext
                input.interval.subscribeOn(Schedulers.computation()).unsafeSubscribe(s);
                input.interval.subscribeOn(Schedulers.computation()).unsafeSubscribe(s);
                // they will be serialized after
            }

        }).serialize().subscribe(ts);
        ts.awaitTerminalEvent();
    }

    @GenerateMicroBenchmark
    public void serializedTwoStreamsHighlyContended(final Input input) {
        TestSubscriber<Long> ts = input.newSubscriber();
        Observable.create(new OnSubscribe<Long>() {

            @Override
            public void call(Subscriber<? super Long> s) {
                // break the contract here and concurrently onNext
                input.firehose.subscribeOn(Schedulers.computation()).unsafeSubscribe(s);
                input.firehose.subscribeOn(Schedulers.computation()).unsafeSubscribe(s);
                // they will be serialized after
            }

        }).serialize().subscribe(ts);
        ts.awaitTerminalEvent();
    }

    @GenerateMicroBenchmark
    public void serializedTwoStreamsOneFastOneSlow(final Input input) {
        TestSubscriber<Long> ts = input.newSubscriber();
        Observable.create(new OnSubscribe<Long>() {

            @Override
            public void call(final Subscriber<? super Long> s) {
                // break the contract here and concurrently onNext
                input.interval.subscribeOn(Schedulers.computation()).unsafeSubscribe(s);
                input.firehose.subscribeOn(Schedulers.computation()).unsafeSubscribe(s);
                // they will be serialized after
            }

        }).serialize().subscribe(ts);
        ts.awaitTerminalEvent();
    }

    @State(Scope.Benchmark)
    public static class Input {

        @Param({ "1", "1000" })
        public int size;

        public Observable<Long> firehose;
        public Observable<Long> interval;

        private BlackHole bh;

        @Setup
        public void setup(final BlackHole bh) {
            this.bh = bh;
            firehose = Observable.create(new OnSubscribe<Long>() {
                @Override
                public void call(Subscriber<? super Long> o) {
                    for (long value = 0; value < size; value++) {
                        if (o.isUnsubscribed())
                            return;
                        o.onNext(value);
                    }
                    o.onCompleted();
                }
            });

            interval = Observable.timer(0, 1, TimeUnit.MILLISECONDS).take(size);
        }

        public TestSubscriber<Long> newSubscriber() {
            return new TestSubscriber<Long>(new Observer<Long>() {
                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                    throw new RuntimeException(e);
                }

                @Override
                public void onNext(Long value) {
                    bh.consume(value);
                }
            });
        }

    }
}
