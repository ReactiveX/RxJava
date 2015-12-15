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
package rx.observables;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.internal.operators.OnSubscribeFromIterable;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class SyncOnSubscribePerf {

    public static void main(String[] args) {
        SingleInput singleInput = new SingleInput();
        singleInput.size = 1;
        singleInput.setup(generated._jmh_tryInit_());
        SyncOnSubscribePerf perf = new SyncOnSubscribePerf();
        perf.benchSyncOnSubscribe(singleInput);
    }
    private static class generated {
        private static Blackhole _jmh_tryInit_() {
            return new Blackhole();
        }
    }
    
    private static OnSubscribe<Integer> createSyncOnSubscribe(final Iterator<Integer> iterator) {
        return new SyncOnSubscribe<Void, Integer>(){

            @Override
            protected Void generateState() {
                return null;
            }

            @Override
            protected Void next(Void state, Observer<? super Integer> observer) {
                if (iterator.hasNext()) {
                    observer.onNext(iterator.next());
                }
                else
                    observer.onCompleted();
                return null;
                }
            };
    }
    
//    @Benchmark
//  @Group("single")
    public void benchSyncOnSubscribe(final SingleInput input) {
        createSyncOnSubscribe(input.iterable.iterator()).call(input.newSubscriber());
    }

//    @Benchmark
//    @Group("single")
    public void benchFromIterable(final SingleInput input) {
        new OnSubscribeFromIterable<Integer>(input.iterable).call(input.newSubscriber());
    }
    
    @Benchmark
//    @Group("multi")
    public void benchSyncOnSubscribe2(final MultiInput input) {
        createSyncOnSubscribe(input.iterable.iterator()).call(input.newSubscriber());
    }
    
    @Benchmark
//    @Group("multi")
    public void benchFromIterable2(final MultiInput input) {
        new OnSubscribeFromIterable<Integer>(input.iterable).call(input.newSubscriber());
    }
}
