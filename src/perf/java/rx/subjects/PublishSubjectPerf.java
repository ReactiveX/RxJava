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

package rx.subjects;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import rx.jmh.LatchedObserver;

/**
 * Benchmark PublishSubject.
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu s -bm thrpt -wi 5 -i 5 -r 1 .*PublishSubjectPerf.*"
 * <p>
 * gradlew benchmarks "-Pjmh=-f 1 -tu ns -bm avgt -wi 5 -i 5 -r 1 .*PublishSubjectPerf.*"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class PublishSubjectPerf {

    @Param({ "1", "2", "4", "8"})
    public int subscribers;

    @Param({ "1", "1000", "1000000"})
    public int count;

    @Benchmark
    public Object benchmark(Blackhole bh) {
        PublishSubject<Integer> ps = PublishSubject.create();

        int s = subscribers;
        for (int i = 0; i < s; i++) {
            ps.subscribe(new LatchedObserver<Integer>(bh));
        }

        int c = count;
        for (int i = 0; i < c; i++) {
            ps.onNext(777);
        }

        ps.onCompleted();

        return ps;
    }
}
