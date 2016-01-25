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

import java.util.concurrent.*;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Benchmark Single.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*SingleSourcePerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*SingleSourcePerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class SingleSourcePerf {
    
    Single<Integer> source;

    Single<Integer> flatmapped;

    Single<Integer> flatmappedConst;

    Single<Integer> sourceObserveOn;

    Single<Integer> sourceSubscribeOn;

    Single<Integer> sourceObserveOnExecutor;

    Single<Integer> sourceSubscribeOnExecutor;

    Single<Integer> sourceObserveOnScheduledExecutor;

    Single<Integer> sourceSubscribeOnScheduledExecutor;

//    Single<Integer> sourceObserveOnFJ;

//    Single<Integer> sourceSubscribeOnFJ;

    ScheduledExecutorService scheduledExecutor;
    
    ExecutorService executor;

    @Setup
    public void setup() {
        source = Single.just(1);
        
        flatmapped = source.flatMap(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer t) {
                return Single.just(t);
            }
        });
        
        flatmapped = source.flatMap(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer t) {
                return source;
            }
        });
        
        sourceObserveOn = source.observeOn(Schedulers.computation());

        sourceSubscribeOn = source.subscribeOn(Schedulers.computation());
        
        // ----------
        
        scheduledExecutor = Executors.newScheduledThreadPool(1);
        
        Scheduler s = Schedulers.from(scheduledExecutor);
        
        sourceObserveOnScheduledExecutor = source.observeOn(s);

        sourceSubscribeOnScheduledExecutor = source.subscribeOn(s);
        
        // ----------
        
        executor = Executors.newSingleThreadExecutor();
        
        Scheduler se = Schedulers.from(executor);
        
        sourceObserveOnExecutor = source.observeOn(se);

        sourceSubscribeOnExecutor = source.subscribeOn(se);
        
        // --------
        
//        Scheduler fj = Schedulers.from(ForkJoinPool.commonPool());
        
//        sourceObserveOnFJ = source.observeOn(fj);

//        sourceSubscribeOnFJ = source.subscribeOn(fj);
    }
    
    @TearDown
    public void teardown() {
        scheduledExecutor.shutdownNow();
        
        executor.shutdownNow();
    }
    
    static final class PlainSingleSubscriber extends SingleSubscriber<Object> {
        final Blackhole bh;
        
        public PlainSingleSubscriber(Blackhole bh) {
            this.bh = bh;
        }
        
        @Override
        public void onSuccess(Object value) {
            bh.consume(value);
        }

        @Override
        public void onError(Throwable error) {
            bh.consume(error);
        }
    }

    static final class LatchedSingleSubscriber extends SingleSubscriber<Object> {
        final Blackhole bh;
        
        final CountDownLatch cdl;
        
        public LatchedSingleSubscriber(Blackhole bh) {
            this.bh = bh;
            this.cdl = new CountDownLatch(1);
        }
        
        @Override
        public void onSuccess(Object value) {
            bh.consume(value);
            cdl.countDown();
        }

        @Override
        public void onError(Throwable error) {
            bh.consume(error);
            cdl.countDown();
        }
        
        public void await() {
            try {
                cdl.await();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
        
        public void awaitSpin() {
            while (cdl.getCount() != 0L) ;
        }
    }

    @Benchmark
    public void direct(Blackhole bh) {
        source.subscribe(new PlainSingleSubscriber(bh));
    }

    @Benchmark
    public void flatmap(Blackhole bh) {
        flatmapped.subscribe(new PlainSingleSubscriber(bh));
    }
    
    @Benchmark
    public void flatmapConst(Blackhole bh) {
        flatmapped.subscribe(new PlainSingleSubscriber(bh));
    }

    @Benchmark
    public void observeOn(Blackhole bh) {
        LatchedSingleSubscriber o = new LatchedSingleSubscriber(bh);
        
        sourceObserveOn.subscribe(o);
        
        o.awaitSpin();
    }

    @Benchmark
    public void observeOnExec(Blackhole bh) {
        LatchedSingleSubscriber o = new LatchedSingleSubscriber(bh);
        
        sourceObserveOnExecutor.subscribe(o);
        
        o.awaitSpin();
    }

    @Benchmark
    public void subscribeOn(Blackhole bh) {
        LatchedSingleSubscriber o = new LatchedSingleSubscriber(bh);
        
        sourceSubscribeOn.subscribe(o);
        
        o.awaitSpin();
    }

    @Benchmark
    public void subscribeOnExec(Blackhole bh) {
        LatchedSingleSubscriber o = new LatchedSingleSubscriber(bh);
        
        sourceSubscribeOnExecutor.subscribe(o);
        
        o.awaitSpin();
    }

    @Benchmark
    public void subscribeOnSchExec(Blackhole bh) {
        LatchedSingleSubscriber o = new LatchedSingleSubscriber(bh);
        
        sourceSubscribeOnScheduledExecutor.subscribe(o);
        
        o.awaitSpin();
    }

//    @Benchmark
//    public void subscribeOnFJ(Blackhole bh) {
//        LatchedSingleSubscriber o = new LatchedSingleSubscriber(bh);
//        
//        sourceSubscribeOnFJ.subscribe(o);
//        
//        o.awaitSpin();
//    }

    @Benchmark
    public void observeOnSchExec(Blackhole bh) {
        LatchedSingleSubscriber o = new LatchedSingleSubscriber(bh);
        
        sourceObserveOnScheduledExecutor.subscribe(o);
        
        o.awaitSpin();
    }

//    @Benchmark
//    public void observeOnFJ(Blackhole bh) {
//        LatchedSingleSubscriber o = new LatchedSingleSubscriber(bh);
//        
//        sourceObserveOnFJ.subscribe(o);
//        
//        o.awaitSpin();
//    }

}
