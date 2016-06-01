/**
 * Copyright 2016 Netflix, Inc.
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

import rx.Observable.OnSubscribe;
import rx.functions.Func1;
import rx.internal.producers.SingleProducer;
import rx.jmh.*;

/**
 * Benchmark operators working on a one-item source.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*OneItemPerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*OneItemPerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class OneItemPerf {
    
    Observable<Integer> scalar;
    Observable<Integer> scalarHidden;
    Observable<Integer> one;
    Single<Integer> single;
    Single<Integer> singleHidden;

    Observable<Integer> scalarConcat;
    Observable<Integer> scalarHiddenConcat;
    Observable<Integer> oneConcat;

    Observable<Integer> scalarMerge;
    Observable<Integer> scalarHiddenMerge;
    Observable<Integer> oneMerge;
    Single<Integer> singleMerge;
    Single<Integer> singleHiddenMerge;

    Observable<Integer> scalarSwitch;
    Observable<Integer> scalarHiddenSwitch;
    Observable<Integer> oneSwitch;
    
    <T> Single<T> hide(final Single<T> single) {
        return Single.create(new Single.OnSubscribe<T>() {
            @Override
            public void call(SingleSubscriber<? super T> t) {
                single.subscribe(t);
            }
        });
    }
    
    @Setup
    public void setup() {
        scalar = Observable.just(1);
        one = Observable.create(new OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> t) {
                t.setProducer(new SingleProducer<Integer>(t, 1));
            }
        });
        single = Single.just(1);
        singleHidden = hide(single);
        scalarHidden = scalar.asObservable();
        
        // ----------------------------------------------------------------------------
        
        scalarConcat = scalar.concatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return scalar;
            }
        });
        scalarHiddenConcat = scalarHidden.concatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return scalar;
            }
        });
        
        oneConcat  = one.concatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return scalar;
            }
        });

        // ----------------------------------------------------------------------------

        scalarMerge = scalar.flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return scalar;
            }
        });
        scalarHiddenMerge = scalarHidden.flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return scalar;
            }
        });
        
        oneMerge  = one.flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return scalar;
            }
        });
        singleMerge = single.flatMap(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return single;
            }
        });
        singleHiddenMerge = hide(single).flatMap(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return single;
            }
        });
        
        // ----------------------------------------------------------------------------
        
        scalarSwitch = scalar.switchMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return scalar;
            }
        });
        scalarHiddenSwitch = scalarHidden.switchMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return scalar;
            }
        });
        
        oneSwitch  = one.switchMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return scalar;
            }
        });
    }
    
    @Benchmark
    public void scalar(Blackhole bh) {
        scalar.subscribe(new LatchedObserver<Integer>(bh));
    }
    @Benchmark
    public void scalarHidden(Blackhole bh) {
        scalarHidden.subscribe(new LatchedObserver<Integer>(bh));
    }
    @Benchmark
    public void one(Blackhole bh) {
        one.subscribe(new LatchedObserver<Integer>(bh));
    }
    @Benchmark
    public void single(Blackhole bh) {
        single.subscribe(new PerfSingleSubscriber(bh));
    }
    @Benchmark
    public void singleHidden(Blackhole bh) {
        singleHidden.subscribe(new PerfSingleSubscriber(bh));
    }

    @Benchmark
    public void scalarConcat(Blackhole bh) {
        scalarConcat.subscribe(new LatchedObserver<Integer>(bh));
    }
    @Benchmark
    public void scalarHiddenConcat(Blackhole bh) {
        scalarHidden.subscribe(new LatchedObserver<Integer>(bh));
    }
    @Benchmark
    public void oneConcat(Blackhole bh) {
        oneConcat.subscribe(new LatchedObserver<Integer>(bh));
    }

    @Benchmark
    public void scalarMerge(Blackhole bh) {
        scalarMerge.subscribe(new LatchedObserver<Integer>(bh));
    }
    @Benchmark
    public void scalarHiddenMerge(Blackhole bh) {
        scalarHidden.subscribe(new LatchedObserver<Integer>(bh));
    }
    @Benchmark
    public void oneMerge(Blackhole bh) {
        oneMerge.subscribe(new LatchedObserver<Integer>(bh));
    }
    @Benchmark
    public void singleMerge(Blackhole bh) {
        single.subscribe(new PerfSingleSubscriber(bh));
    }
    @Benchmark
    public void singleHiddenMerge(Blackhole bh) {
        singleHiddenMerge.subscribe(new PerfSingleSubscriber(bh));
    }

    @Benchmark
    public void scalarSwitch(Blackhole bh) {
        scalarSwitch.subscribe(new LatchedObserver<Integer>(bh));
    }
    @Benchmark
    public void scalarHiddenSwitch(Blackhole bh) {
        scalarHiddenSwitch.subscribe(new LatchedObserver<Integer>(bh));
    }
    @Benchmark
    public void oneSwitch(Blackhole bh) {
        oneSwitch.subscribe(new LatchedObserver<Integer>(bh));
    }

}
