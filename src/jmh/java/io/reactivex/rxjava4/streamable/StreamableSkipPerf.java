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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openjdk.jmh.annotations.*;

import io.reactivex.rxjava4.core.Streamable;

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
///
/// # 3. sync bias via Claude Fable atomics
///
/// Small regression on 1, but +200% on million vs 2., almost +410% vs baseline
///
/// ```
/// Benchmark                     (times)   Mode  Cnt           Score           Error  Units
/// StreamableSkipPerf.benchmark        1  thrpt    5  1392748873,097 ┬▒ 240564597,648  ops/s
/// StreamableSkipPerf.benchmark       10  thrpt    5    50478482,677 ┬▒   2503440,823  ops/s
/// StreamableSkipPerf.benchmark      100  thrpt    5     8260512,961 ┬▒    832256,445  ops/s
/// StreamableSkipPerf.benchmark     1000  thrpt    5      617903,033 ┬▒    127135,231  ops/s
/// StreamableSkipPerf.benchmark    10000  thrpt    5       65116,345 ┬▒      8425,364  ops/s
/// StreamableSkipPerf.benchmark   100000  thrpt    5        6298,539 ┬▒       739,686  ops/s
/// StreamableSkipPerf.benchmark  1000000  thrpt    5         634,945 ┬▒        99,134  ops/s
/// ```
///
/// # 4. indexable/enumerable/deferredenumerable
///
/// ```
/// Benchmark                      (times)   Mode  Cnt           Score           Error  Units
/// StreamableSkipPerf.benchmark         1  thrpt    5  1371065918,333 ┬▒ 114154178,555  ops/s
/// StreamableSkipPerf.benchmark        10  thrpt    5    48387844,296 ┬▒   1998160,368  ops/s
/// StreamableSkipPerf.benchmark       100  thrpt    5     8344391,259 ┬▒    251723,213  ops/s
/// StreamableSkipPerf.benchmark      1000  thrpt    5      700332,413 ┬▒     28667,777  ops/s
/// StreamableSkipPerf.benchmark     10000  thrpt    5       68645,229 ┬▒      3123,103  ops/s
/// StreamableSkipPerf.benchmark    100000  thrpt    5        6047,152 ┬▒       725,594  ops/s
/// StreamableSkipPerf.benchmark   1000000  thrpt    5         649,134 ┬▒        36,349  ops/s
/// StreamableSkipPerf.enumerable        1  thrpt    5    71117553,552 ┬▒   6738602,495  ops/s
/// StreamableSkipPerf.enumerable       10  thrpt    5    43426136,653 ┬▒   2744303,836  ops/s
/// StreamableSkipPerf.enumerable      100  thrpt    5     2819347,969 ┬▒     20437,839  ops/s
/// StreamableSkipPerf.enumerable     1000  thrpt    5      298000,822 ┬▒     28754,714  ops/s
/// StreamableSkipPerf.enumerable    10000  thrpt    5       28839,499 ┬▒      3096,840  ops/s
/// StreamableSkipPerf.enumerable   100000  thrpt    5        2657,147 ┬▒        23,023  ops/s
/// StreamableSkipPerf.enumerable  1000000  thrpt    5         187,770 ┬▒        42,082  ops/s
/// StreamableSkipPerf.indexed           1  thrpt    5    75403131,814 ┬▒   4676965,390  ops/s
/// StreamableSkipPerf.indexed          10  thrpt    5    42730509,975 ┬▒   6004178,786  ops/s
/// StreamableSkipPerf.indexed         100  thrpt    5     3561284,547 ┬▒    381609,301  ops/s
/// StreamableSkipPerf.indexed        1000  thrpt    5      346781,462 ┬▒     28909,008  ops/s
/// StreamableSkipPerf.indexed       10000  thrpt    5       54254,177 ┬▒      7440,753  ops/s
/// StreamableSkipPerf.indexed      100000  thrpt    5        5153,256 ┬▒       310,895  ops/s
/// StreamableSkipPerf.indexed     1000000  thrpt    5         259,912 ┬▒        76,746  ops/s
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
    Streamable<List<Integer>> resultIndexed;
    Streamable<List<Integer>> resultEnumerable;

    @Setup
    public void setup() {
        result = Streamable.range(1, times).skip(times / 2);
        resultIndexed = Streamable.range(1, times).skip(times / 2).collect(Collectors.toList());
        resultEnumerable = Streamable.range(1, times).filter(_ -> true).skip(times / 2).collect(Collectors.toList());
    }

    @Benchmark
    public Object benchmark() {
        return result.blockingLast();
    }

    @Benchmark
    public Object indexed() {
        return resultIndexed.blockingLast();
    }

    @Benchmark
    public Object enumerable() {
        return resultEnumerable.blockingLast();
    }
}
