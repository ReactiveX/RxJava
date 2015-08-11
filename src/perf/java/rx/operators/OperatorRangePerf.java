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
import rx.internal.operators.OnSubscribeRange;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class OperatorRangePerf {

    @Benchmark
    public void rangeWithBackpressureRequest(InputUsingRequest input) throws InterruptedException {
        input.observable.subscribe(input.newSubscriber());
    }

    @State(Scope.Thread)
    public static class InputUsingRequest {

        @Param({ "1", "1000", "1000000" })
        public int size;

        public Observable<Integer> observable;
        Blackhole bh;

        @Setup
        public void setup(final Blackhole bh) {
            observable = Observable.create(new OnSubscribeRange(0, size));
            this.bh = bh;
        }

        public Subscriber<Integer> newSubscriber() {
            return new Subscriber<Integer>() {

                @Override
                public void onStart() {
                    request(size);
                }
                
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Integer t) {
                    bh.consume(t);
                }

            };
        }

    }

    @Benchmark
    public void rangeWithoutBackpressure(InputWithoutRequest input) throws InterruptedException {
        input.observable.subscribe(input.newSubscriber());
    }

    @State(Scope.Thread)
    public static class InputWithoutRequest {

        @Param({ "1", "1000", "1000000" })
        public int size;

        public Observable<Integer> observable;
        Blackhole bh;

        @Setup
        public void setup(final Blackhole bh) {
            observable = Observable.create(new OnSubscribeRange(0, size));
            this.bh = bh;

        }

        public Subscriber<Integer> newSubscriber() {
            return new Subscriber<Integer>() {

                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(Integer t) {
                    bh.consume(t);
                }

            };
        }

    }

}
