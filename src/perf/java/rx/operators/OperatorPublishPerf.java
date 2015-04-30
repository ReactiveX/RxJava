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

import java.util.*;
import java.util.concurrent.*;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import rx.*;
import rx.Observable;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

/**
 * Benchmark typical atomic operations on volatile fields and AtomicXYZ classes.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*OperatorPublishPerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*OperatorPublishPerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class OperatorPublishPerf {
    static final class SharedLatchObserver extends Subscriber<Integer> {
        final CountDownLatch cdl;
        final int batchFrequency;
        final Blackhole bh;
        int received;
        public SharedLatchObserver(CountDownLatch cdl, int batchFrequency, Blackhole bh) {
            this.cdl = cdl;
            this.batchFrequency = batchFrequency;
            this.bh = bh;
        }
        @Override
        public void onStart() {
            request(batchFrequency);
        }
        @Override
        public void onNext(Integer t) {
            if (bh != null) {
                bh.consume(t);
            }
            if (++received == batchFrequency) {
                received = 0;
                request(batchFrequency);
            }
        }
        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
            cdl.countDown();
        }
        @Override
        public void onCompleted() {
            cdl.countDown();
        }
    }
    
    
    /** How long the range should be. */
    @Param({"1", "1000", "1000000"})
    private int size;
    /** Should children use observeOn? */
    @Param({"false", "true"})
    private boolean async;
    /** Number of child subscribers. */
    @Param({"0", "1", "2", "3", "4", "5"})
    private int childCount;
    /** How often the child subscribers should re-request. */
    @Param({"1", "2", "4", "8", "16", "32", "64"})
    private int batchFrequency;
    
    private ConnectableObservable<Integer> source;
    private Observable<Integer> observable;
    @Setup
    public void setup() {
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        Observable<Integer> src = Observable.from(list);
        source = src.publish();
        observable = async ? source.observeOn(Schedulers.computation()) : source;
    }
    
    @Benchmark
    public void benchmark(Blackhole bh) throws InterruptedException, 
            TimeoutException, BrokenBarrierException {
        CountDownLatch completion = null;
        int cc = childCount;
        
        if (cc > 0) {
            completion = new CountDownLatch(cc);
            Observable<Integer> o = observable;
            for (int i = 0; i < childCount; i++) {
                o.subscribe(new SharedLatchObserver(completion, batchFrequency, bh));
            }
        }
        
        Subscription s = source.connect();
        
        if (completion != null && !completion.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Source hung!");
        }
        s.unsubscribe();
    }
    public static void main(String[] args) throws Exception {
        OperatorPublishPerf o = new OperatorPublishPerf();
        o.async = false;
        o.batchFrequency = 1;
        o.childCount = 0;
        o.size = 1;
        o.setup();
        for (int j = 0; j < 1000; j++) {
            o.benchmark(null);
        }
    }
}
