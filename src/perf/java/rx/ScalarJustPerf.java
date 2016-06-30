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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import rx.functions.Func1;
import rx.jmh.LatchedObserver;
import rx.schedulers.Schedulers;

/**
 * Benchmark the cost of just and its various optimizations.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*ScalarJustPerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*ScalarJustPerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class ScalarJustPerf {
    /** A subscriber without a CountDownLatch; use it for synchronous testing only. */
    static final class PlainSubscriber extends Subscriber<Integer> {
        final Blackhole bh;
        public PlainSubscriber(Blackhole bh) {
            this.bh = bh;
        }
        
        @Override
        public void onNext(Integer t) {
            bh.consume(t);
        }
        
        @Override
        public void onError(Throwable e) {
            bh.consume(e);
        }
        
        @Override
        public void onCompleted() {
            bh.consume(false);
        }
    }
    
    /** This is a simple just. */
    Observable<Integer> simple;
    /** 
     * This is a simple just observed on the computation scheduler.
     * The current computation scheduler supports direct scheduling and should have
     * lower overhead than a regular createWorker-use-unsubscribe.
     */
    Observable<Integer> observeOn;
    /** This is a simple just observed on the IO thread. */
    Observable<Integer> observeOnIO;
    
    /** 
     * This is a simple just subscribed to on the computation scheduler.
     * In theory, for non-backpressured just(), this should be the
     * same as observeOn.
     */
    Observable<Integer> subscribeOn;
    /** This is a simple just subscribed to on the IO scheduler. */
    Observable<Integer> subscribeOnIO;

    /** This is a just mapped to itself which should skip the operator flatMap completely. */
    Observable<Integer> justFlatMapJust;
    /** 
     * This is a just mapped to a range of 2 elements; it tests the case where the inner
     * Observable isn't a just().
     */
    Observable<Integer> justFlatMapRange;
    
    @Setup
    public void setup() {
        simple = Observable.just(1);
        
        observeOn = simple.observeOn(Schedulers.computation());
        observeOnIO = simple.observeOn(Schedulers.io());
        
        subscribeOn = simple.subscribeOn(Schedulers.computation());
        subscribeOnIO = simple.subscribeOn(Schedulers.io());
        
        justFlatMapJust = simple.flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.just(v);
            }
        });
        
        justFlatMapRange = simple.flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.range(v, 2);
            }
        });
    }

    /**
     * Common routine to create a latched observer, subscribe it to the
     * given source and spin-wait for its completion.
     * <p>Don't use this with long sources. The spin-wait is there
     * to avoid operating-system level scheduling-wakeup granularity problems with
     * short sources.
     * @param bh the black hole to sink values and prevent dead code elimination
     * @param source the source observable to observe
     */
    void runAsync(Blackhole bh, Observable<Integer> source) {
        LatchedObserver<Integer> lo = new LatchedObserver<Integer>(bh);
        
        source.subscribe(lo);
        
        while (lo.latch.getCount() != 0L);
    }
    
    @Benchmark
    public void simple(Blackhole bh) {
        PlainSubscriber s = new PlainSubscriber(bh);
        simple.subscribe(s);
    }

    @Benchmark
    public void simpleEscape(Blackhole bh) {
        PlainSubscriber s = new PlainSubscriber(bh);
        bh.consume(s);
        simple.subscribe(s);
    }

    @Benchmark
    public Object simpleEscapeAll(Blackhole bh) {
        PlainSubscriber s = new PlainSubscriber(bh);
        bh.consume(s);
        return simple.subscribe(s);
    }
    
    @Benchmark
    public void observeOn(Blackhole bh) {
        runAsync(bh, observeOn);
    }
    
    @Benchmark
    public void observeOnIO(Blackhole bh) {
        runAsync(bh, observeOnIO);
    }

    @Benchmark
    public void subscribeOn(Blackhole bh) {
        runAsync(bh, subscribeOn);
    }
    
    @Benchmark
    public void subscribeOnIO(Blackhole bh) {
        runAsync(bh, subscribeOnIO);
    }
    
    @Benchmark
    public void justFlatMapJust(Blackhole bh) {
        PlainSubscriber s = new PlainSubscriber(bh);
        justFlatMapJust.subscribe(s);
    }
    
    @Benchmark
    public void justFlatMapJustEscape(Blackhole bh) {
        PlainSubscriber s = new PlainSubscriber(bh);
        bh.consume(s);
        justFlatMapJust.subscribe(s);
    }

    @Benchmark
    public void justFlatMapRange(Blackhole bh) {
        PlainSubscriber s = new PlainSubscriber(bh);
        justFlatMapRange.subscribe(s);
    }
    
    @Benchmark
    public void justFlatMapRangeEscape(Blackhole bh) {
        PlainSubscriber s = new PlainSubscriber(bh);
        bh.consume(s);
        justFlatMapRange.subscribe(s);
    }
}
