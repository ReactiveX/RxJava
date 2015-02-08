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
package rx;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import rx.Single.OnSubscribe;
import rx.jmh.LatchedObserver;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SinglePerfBaseline {


    @Benchmark
    public void singleConsumption(Input input) throws InterruptedException {
        input.single.subscribe(input.newSubscriber());
    }
    
    @Benchmark
    public void singleConsumptionUnsafe(Input input) throws InterruptedException {
        input.single.unsafeSubscribe(input.newSubscriber());
    }
    
    @Benchmark
    public void newSingleAndSubscriberEachTime(Input input) throws InterruptedException {
        input.newSingle().subscribe(input.newSubscriber());
    }

    @State(Scope.Thread)
    public static class Input {
        public Single<Integer> single;
        public Blackhole bh;

        @Setup
        public void setup(final Blackhole bh) {
            this.bh = bh;
            single = Single.just(1);
        }

        public LatchedObserver<Integer> newLatchedObserver() {
            return new LatchedObserver<Integer>(bh);
        }
        
        public Single<Integer> newSingle() {
            return Single.create(new OnSubscribe<Integer>() {

                @Override
                public void call(SingleSubscriber<? super Integer> t) {
                    t.onSuccess(1);
                }
                
            });
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
