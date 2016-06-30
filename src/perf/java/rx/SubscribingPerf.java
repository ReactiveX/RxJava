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

/**
 * Benchmark the cost of subscription and initial request management.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*SubscribingPerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*SubscribingPerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class SubscribingPerf {
    
    Observable<Integer> just = Observable.just(1);
    Observable<Integer> range = Observable.range(1, 2);
    
    @Benchmark
    public void justDirect(Blackhole bh) {
        DirectSubscriber<Integer> subscriber = new DirectSubscriber<Integer>(Long.MAX_VALUE, bh);
        bh.consume(subscriber);
        just.subscribe(subscriber);
    }

    @Benchmark
    public void justStarted(Blackhole bh) {
        StartedSubscriber<Integer> subscriber = new StartedSubscriber<Integer>(Long.MAX_VALUE, bh);
        bh.consume(subscriber);
        just.subscribe(subscriber);
    }

    @Benchmark
    public void justUsual(Blackhole bh) {
        UsualSubscriber<Integer> subscriber = new UsualSubscriber<Integer>(Long.MAX_VALUE, bh);
        bh.consume(subscriber);
        just.subscribe(subscriber);
    }

    @Benchmark
    public void rangeDirect(Blackhole bh) {
        DirectSubscriber<Integer> subscriber = new DirectSubscriber<Integer>(Long.MAX_VALUE, bh);
        bh.consume(subscriber);
        range.subscribe(subscriber);
    }

    @Benchmark
    public void rangeStarted(Blackhole bh) {
        StartedSubscriber<Integer> subscriber = new StartedSubscriber<Integer>(Long.MAX_VALUE, bh);
        bh.consume(subscriber);
        range.subscribe(subscriber);
    }

    @Benchmark
    public void rangeUsual(Blackhole bh) {
        UsualSubscriber<Integer> subscriber = new UsualSubscriber<Integer>(Long.MAX_VALUE, bh);
        bh.consume(subscriber);
        range.subscribe(subscriber);
    }

    @Benchmark
    public void justDirectUnsafe(Blackhole bh) {
        DirectSubscriber<Integer> subscriber = new DirectSubscriber<Integer>(Long.MAX_VALUE, bh);
        bh.consume(subscriber);
        just.unsafeSubscribe(subscriber);
    }

    @Benchmark
    public void justStartedUnsafe(Blackhole bh) {
        StartedSubscriber<Integer> subscriber = new StartedSubscriber<Integer>(Long.MAX_VALUE, bh);
        bh.consume(subscriber);
        just.unsafeSubscribe(subscriber);
    }

    @Benchmark
    public void justUsualUnsafe(Blackhole bh) {
        UsualSubscriber<Integer> subscriber = new UsualSubscriber<Integer>(Long.MAX_VALUE, bh);
        bh.consume(subscriber);
        just.unsafeSubscribe(subscriber);
    }

    @Benchmark
    public void rangeDirectUnsafe(Blackhole bh) {
        DirectSubscriber<Integer> subscriber = new DirectSubscriber<Integer>(Long.MAX_VALUE, bh);
        bh.consume(subscriber);
        range.unsafeSubscribe(subscriber);
    }

    @Benchmark
    public void rangeStartedUnsafe(Blackhole bh) {
        StartedSubscriber<Integer> subscriber = new StartedSubscriber<Integer>(Long.MAX_VALUE, bh);
        bh.consume(subscriber);
        range.unsafeSubscribe(subscriber);
    }

    @Benchmark
    public void rangeUsualUnsafe(Blackhole bh) {
        UsualSubscriber<Integer> subscriber = new UsualSubscriber<Integer>(Long.MAX_VALUE, bh);
        bh.consume(subscriber);
        range.unsafeSubscribe(subscriber);
    }

    @State(Scope.Thread)
    public static class Chain {
        @Param({"10", "1000", "1000000"})
        public int times;
        
        @Param({"1", "2", "3", "4", "5"})
        public int maps;
        
        Observable<Integer> source;
        
        @Setup
        public void setup() {
            Observable<Integer> o = Observable.range(1, times);
            
            for (int i = 0; i < maps; i++) {
                o = o.map(new Func1<Integer, Integer>() {
                    @Override
                    public Integer call(Integer v) {
                        return v + 1;
                    }
                });
            }
            
            source = o;
        }
        
        @Benchmark
        public void mapped(Chain c, Blackhole bh) {
            DirectSubscriber<Integer> subscriber = new DirectSubscriber<Integer>(Long.MAX_VALUE, bh);
            bh.consume(subscriber);
            c.source.subscribe(subscriber);
        }
    }
    
    static final class DirectSubscriber<T> extends Subscriber<T> {
        final long r;
        final Blackhole bh;
        public DirectSubscriber(long r, Blackhole bh) {
            this.r = r;
            this.bh = bh;
        }
        @Override
        public void onNext(T t) {
            bh.consume(t);
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
            p.request(r);
        }
    }

    static final class StartedSubscriber<T> extends Subscriber<T> {
        final long r;
        final Blackhole bh;
        public StartedSubscriber(long r, Blackhole bh) {
            this.r = r;
            this.bh = bh;
        }
        
        @Override
        public void onStart() {
            request(r);
        }
        
        @Override
        public void onNext(T t) {
            bh.consume(t);
        }
        
        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
        }
        
        @Override
        public void onCompleted() {
            
        }
    }

    /**
     * This requests in the constructor.
     * @param <T> the value type
     */
    static final class UsualSubscriber<T> extends Subscriber<T> {
        final Blackhole bh;
        public UsualSubscriber(long r, Blackhole bh) {
            this.bh = bh;
            request(r);
        }
        
        @Override
        public void onNext(T t) {
            bh.consume(t);
        }
        
        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
        }
        
        @Override
        public void onCompleted() {
            
        }
    }
}
