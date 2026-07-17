/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava4.streamable;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import io.reactivex.rxjava4.core.*;

///
/// The map is one of the most used operator in the ecosystem so it must be fast and
/// it must support operator fusion across itself.
///
/// i9 275HX, 32GB LPDDR5 6400MT CL52, Windows 25H2, JDK 26.0.1
///
/// # 0. Baseline
/// ```
/// Benchmark                             (times)   Mode  Cnt         Score        Error  Units
/// StreamableLastAsSinglePerf.benchmark        1  thrpt    5  14165950,142 ┬▒ 277135,092  ops/s
/// StreamableLastAsSinglePerf.benchmark       10  thrpt    5   5437536,431 ┬▒  89291,499  ops/s
/// StreamableLastAsSinglePerf.benchmark      100  thrpt    5    688500,242 ┬▒   9755,649  ops/s
/// StreamableLastAsSinglePerf.benchmark     1000  thrpt    5     67446,091 ┬▒   4208,462  ops/s
/// StreamableLastAsSinglePerf.benchmark    10000  thrpt    5      6880,385 ┬▒    223,058  ops/s
/// StreamableLastAsSinglePerf.benchmark   100000  thrpt    5       690,980 ┬▒     29,543  ops/s
/// StreamableLastAsSinglePerf.benchmark  1000000  thrpt    5        66,935 ┬▒      1,086  ops/s
/// ```
///
/// # 1. avoid whenComplete
///
/// +17% for times 1, +50% for a million
///
/// ```
/// Benchmark                             (times)   Mode  Cnt         Score        Error  Units
/// StreamableLastAsSinglePerf.benchmark        1  thrpt    5  16684817,192 ┬▒ 337671,603  ops/s
/// StreamableLastAsSinglePerf.benchmark       10  thrpt    5   7713698,470 ┬▒  72192,494  ops/s
/// StreamableLastAsSinglePerf.benchmark      100  thrpt    5    992063,105 ┬▒  31518,477  ops/s
/// StreamableLastAsSinglePerf.benchmark     1000  thrpt    5    105383,988 ┬▒   4062,065  ops/s
/// StreamableLastAsSinglePerf.benchmark    10000  thrpt    5     10103,558 ┬▒    494,420  ops/s
/// StreamableLastAsSinglePerf.benchmark   100000  thrpt    5      1022,181 ┬▒     38,701  ops/s
/// StreamableLastAsSinglePerf.benchmark  1000000  thrpt    5       100,744 ┬▒      1,390  ops/s
/// ```
///
/// # 2. batch wip accounting
///
/// +6.8% for times 1 vs optimization 1. +23.3% for times million vs #0
/// +25.8% for times 1 vs baseline, +82% for times million vs baseline
///
/// ```
/// Benchmark                             (times)   Mode  Cnt         Score        Error  Units
/// StreamableLastAsSinglePerf.benchmark        1  thrpt    5  17823989,674 ┬▒ 243372,183  ops/s
/// StreamableLastAsSinglePerf.benchmark       10  thrpt    5   9319936,731 ┬▒ 157326,848  ops/s
/// StreamableLastAsSinglePerf.benchmark      100  thrpt    5   1308207,680 ┬▒  14246,778  ops/s
/// StreamableLastAsSinglePerf.benchmark     1000  thrpt    5    138722,075 ┬▒  29515,013  ops/s
/// StreamableLastAsSinglePerf.benchmark    10000  thrpt    5     11938,692 ┬▒    354,381  ops/s
/// StreamableLastAsSinglePerf.benchmark   100000  thrpt    5      1408,117 ┬▒    134,934  ops/s
/// StreamableLastAsSinglePerf.benchmark  1000000  thrpt    5       123,326 ┬▒     29,358  ops/s
/// ```
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class StreamableLastAsSinglePerf {
    @Param({ "1", "10", "100", "1000", "10000", "100000", "1000000" })
    public int times;

    Single<Integer> result;

    @Setup
    public void setup() {
        result = Streamable.range(1, times).lastOrError();
    }

    @Benchmark
    public Object benchmark() {
        return result.blockingGet();
    }
}
