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
/// Benchmark                     (times)   Mode  Cnt           Score           Error  Units
/// StreamableSkipPerf.benchmark        1  thrpt    5  1414799252,380 ┬▒ 237083836,864  ops/s
/// StreamableSkipPerf.benchmark       10  thrpt    5    10427857,800 ┬▒    458808,883  ops/s
/// StreamableSkipPerf.benchmark      100  thrpt    5     1242428,922 ┬▒     11522,536  ops/s
/// StreamableSkipPerf.benchmark     1000  thrpt    5      123656,684 ┬▒      1351,735  ops/s
/// StreamableSkipPerf.benchmark    10000  thrpt    5       12247,572 ┬▒       250,378  ops/s
/// StreamableSkipPerf.benchmark   100000  thrpt    5        1213,043 ┬▒        21,316  ops/s
/// StreamableSkipPerf.benchmark  1000000  thrpt    5         124,518 ┬▒         2,140  ops/s
/// ```
///
/// # 1. avoid whenComplete
///
///  +39% for times 10 and million
///
/// ```
/// Benchmark                     (times)   Mode  Cnt           Score           Error  Units
/// StreamableSkipPerf.benchmark        1  thrpt    5  1354250907,646 ┬▒ 261597823,396  ops/s
/// StreamableSkipPerf.benchmark       10  thrpt    5    14537792,082 ┬▒    941666,637  ops/s
/// StreamableSkipPerf.benchmark      100  thrpt    5     1921936,485 ┬▒     35720,007  ops/s
/// StreamableSkipPerf.benchmark     1000  thrpt    5      164997,708 ┬▒      2391,852  ops/s
/// StreamableSkipPerf.benchmark    10000  thrpt    5       17398,539 ┬▒       660,608  ops/s
/// StreamableSkipPerf.benchmark   100000  thrpt    5        1719,680 ┬▒        22,035  ops/s
/// StreamableSkipPerf.benchmark  1000000  thrpt    5         173,791 ┬▒         4,496  ops/s
/// ```
///
/// # 2. batch wip accounting
///
///  +74% for times 10, +72% for million
///
/// ```
/// Benchmark                     (times)   Mode  Cnt           Score           Error  Units
/// StreamableSkipPerf.benchmark        1  thrpt    5  1430961236,046 ┬▒ 181925205,170  ops/s
/// StreamableSkipPerf.benchmark       10  thrpt    5    18226537,908 ┬▒    671950,718  ops/s
/// StreamableSkipPerf.benchmark      100  thrpt    5     2395626,005 ┬▒     88190,509  ops/s
/// StreamableSkipPerf.benchmark     1000  thrpt    5      215646,667 ┬▒      2682,422  ops/s
/// StreamableSkipPerf.benchmark    10000  thrpt    5       21791,516 ┬▒      1607,471  ops/s
/// StreamableSkipPerf.benchmark   100000  thrpt    5        2168,925 ┬▒        32,591  ops/s
/// StreamableSkipPerf.benchmark  1000000  thrpt    5         214,055 ┬▒         5,629  ops/s
/// ```
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class StreamableSkipPerf {
    @Param({ "1", "10", "100", "1000", "10000", "100000", "1000000" })
    public int times;

    Streamable<Integer> result;

    @Setup
    public void setup() {
        result = Streamable.range(1, times).skip(times / 2);
    }

    @Benchmark
    public Object benchmark() {
        return result.blockingLast();
    }
}
