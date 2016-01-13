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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import rx.*;
import rx.internal.operators.*;

/**
 * Benchmark typical atomic operations on volatile fields and AtomicXYZ classes.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*FromComparison.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*FromComparison.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class FromComparison {
    @Param({ "1", "10", "100", "1000", "1000000" })
    public int times;
    
    Observable<Integer> iterableSource;
    
    Observable<Integer> arraySource;
    
    @Setup
    public void setup() {
        Integer[] array = new Integer[times];
        
        Arrays.fill(array, 1);
        
        iterableSource = Observable.create(new OnSubscribeFromIterable<Integer>(Arrays.asList(array)));
        arraySource = Observable.create(new OnSubscribeFromArray<Integer>(array));
    }
    
    @Benchmark
    public void fastpathIterable(Blackhole bh) {
        iterableSource.subscribe(new RequestingSubscriber<Object>(bh, Long.MAX_VALUE));
    }

    @Benchmark
    public void fastpathArray(Blackhole bh) {
        arraySource.subscribe(new RequestingSubscriber<Object>(bh, Long.MAX_VALUE));
    }

    @Benchmark
    public void slowpathIterable(Blackhole bh) {
        iterableSource.subscribe(new RequestingSubscriber<Object>(bh, times + 1));
    }

    @Benchmark
    public void slowpathArray(Blackhole bh) {
        arraySource.subscribe(new RequestingSubscriber<Object>(bh, times + 1));
    }

    @Benchmark
    public void slowpathIterable2(Blackhole bh) {
        iterableSource.subscribe(new RequestingSubscriber<Object>(bh, 128));
    }

    @Benchmark
    public void slowpathArray2(Blackhole bh) {
        arraySource.subscribe(new RequestingSubscriber<Object>(bh, 128));
    }

    
    static final class RequestingSubscriber<T> extends Subscriber<T> {
        final Blackhole bh;
        final long limit;
        long received;
        Producer p;
        
        public RequestingSubscriber(Blackhole bh, long limit) {
            this.bh = bh;
            this.limit = limit;
        }
        
        @Override
        public void onNext(T t) {
            bh.consume(t);
            if (++received >= limit) {
                received = 0L;
                p.request(limit);
            }
        }
        
        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
        }
        
        @Override
        public void onCompleted() {
            
        }
        
        @Override
        public void setProducer(Producer p) {
            this.p = p;
            p.request(limit);
        }
    }
}
