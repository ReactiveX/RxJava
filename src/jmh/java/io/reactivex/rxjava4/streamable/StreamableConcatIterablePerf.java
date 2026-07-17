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

import org.openjdk.jmh.annotations.*;

import io.reactivex.rxjava4.core.Streamable;

///
/// The concat(Iterable) seems to be one of the high allocators in the Scrabble benchmark
/// because of the continuation probably?
///
/// i9 275HX, 32GB LPDDR5 6400MT CL52, Windows 25H2, JDK 26.0.1
///
/// # 0. Baseline
/// ```
/// Benchmark                               (times)   Mode  Cnt        Score       Error  Units
/// StreamableConcatIterablePerf.benchmark        1  thrpt    5  3474744,064 ┬▒ 92593,235  ops/s
/// StreamableConcatIterablePerf.benchmark       10  thrpt    5  1640843,357 ┬▒ 30500,201  ops/s
/// StreamableConcatIterablePerf.benchmark      100  thrpt    5   236067,804 ┬▒  1324,984  ops/s
/// StreamableConcatIterablePerf.benchmark     1000  thrpt    5    24348,807 ┬▒   494,837  ops/s
/// StreamableConcatIterablePerf.benchmark    10000  thrpt    5     2469,689 ┬▒    30,614  ops/s
/// StreamableConcatIterablePerf.benchmark   100000  thrpt    5      241,203 ┬▒     5,330  ops/s
/// StreamableConcatIterablePerf.benchmark  1000000  thrpt    5       24,330 ┬▒     0,886  ops/s
/// ```
///
/// # 1. Reduce allocation in whenComplete
///
/// No practical effect
///
/// ```
/// Benchmark                               (times)   Mode  Cnt        Score        Error  Units
/// StreamableConcatIterablePerf.benchmark        1  thrpt    5  3517630,344 ┬▒ 203989,280  ops/s
/// StreamableConcatIterablePerf.benchmark       10  thrpt    5  1633567,521 ┬▒  40147,286  ops/s
/// StreamableConcatIterablePerf.benchmark      100  thrpt    5   236815,630 ┬▒   6185,836  ops/s
/// StreamableConcatIterablePerf.benchmark     1000  thrpt    5    23939,185 ┬▒    319,632  ops/s
/// StreamableConcatIterablePerf.benchmark    10000  thrpt    5     2452,654 ┬▒     98,012  ops/s
/// StreamableConcatIterablePerf.benchmark   100000  thrpt    5      239,677 ┬▒      6,460  ops/s
/// StreamableConcatIterablePerf.benchmark  1000000  thrpt    5       24,930 ┬▒      1,884  ops/s
/// ```
///
/// # 2. avoid calling `whenComplete`
///
/// +35% performance vs 1 for longer sequences
///
/// ```
/// Benchmark                               (times)   Mode  Cnt        Score        Error  Units
/// StreamableConcatIterablePerf.benchmark        1  thrpt    5  3785671,506 ┬▒ 124882,806  ops/s
/// StreamableConcatIterablePerf.benchmark       10  thrpt    5  2028842,852 ┬▒  76338,957  ops/s
/// StreamableConcatIterablePerf.benchmark      100  thrpt    5   300832,578 ┬▒   9916,135  ops/s
/// StreamableConcatIterablePerf.benchmark     1000  thrpt    5    33103,046 ┬▒    856,542  ops/s
/// StreamableConcatIterablePerf.benchmark    10000  thrpt    5     3272,894 ┬▒    129,658  ops/s
/// StreamableConcatIterablePerf.benchmark   100000  thrpt    5      335,497 ┬▒      4,678  ops/s
/// StreamableConcatIterablePerf.benchmark  1000000  thrpt    5       33,685 ┬▒      0,565  ops/s
/// ```
///
///  # 3. avoid calling decrementAndGet every time on a synchronous/reentrant usage
///
/// +6% for the 1 case but, -5% performance regression vs optimization 2
/// ```
/// Benchmark                               (times)   Mode  Cnt        Score       Error  Units
/// StreamableConcatIterablePerf.benchmark        1  thrpt    5  4021165,636 ┬▒ 93456,776  ops/s
/// StreamableConcatIterablePerf.benchmark       10  thrpt    5  1990327,224 ┬▒ 24421,179  ops/s
/// StreamableConcatIterablePerf.benchmark      100  thrpt    5   301872,212 ┬▒  2880,776  ops/s
/// StreamableConcatIterablePerf.benchmark     1000  thrpt    5    30909,533 ┬▒   620,143  ops/s
/// StreamableConcatIterablePerf.benchmark    10000  thrpt    5     3174,424 ┬▒    37,618  ops/s
/// StreamableConcatIterablePerf.benchmark   100000  thrpt    5      316,643 ┬▒     4,920  ops/s
/// StreamableConcatIterablePerf.benchmark  1000000  thrpt    5       31,970 ┬▒     0,521  ops/s
/// ```
///
/// # 4. restore the decrementAndGet use
///
/// +/- 1% vs optimization 3
///
/// ```
/// Benchmark                               (times)   Mode  Cnt        Score        Error  Units
/// StreamableConcatIterablePerf.benchmark        1  thrpt    5  4034863,551 ┬▒ 147716,809  ops/s
/// StreamableConcatIterablePerf.benchmark       10  thrpt    5  1951987,827 ┬▒  29883,362  ops/s
/// StreamableConcatIterablePerf.benchmark      100  thrpt    5   302013,530 ┬▒   4251,994  ops/s
/// StreamableConcatIterablePerf.benchmark     1000  thrpt    5    33260,723 ┬▒    755,076  ops/s
/// StreamableConcatIterablePerf.benchmark    10000  thrpt    5     3329,779 ┬▒     81,860  ops/s
/// StreamableConcatIterablePerf.benchmark   100000  thrpt    5      333,358 ┬▒      8,474  ops/s
/// StreamableConcatIterablePerf.benchmark  1000000  thrpt    5       32,943 ┬▒      1,187  ops/s
/// ```
/// # 5. fall through to getting the first item from the next source when it is picked, save a drain call
///
/// +5% for short sequences, +22% for short sequences
///
/// ```
/// Benchmark                               (times)   Mode  Cnt        Score       Error  Units
/// StreamableConcatIterablePerf.benchmark        1  thrpt    5  4252027,061 ┬▒ 65983,230  ops/s
/// StreamableConcatIterablePerf.benchmark       10  thrpt    5  2125263,936 ┬▒ 46999,223  ops/s
/// StreamableConcatIterablePerf.benchmark      100  thrpt    5   304414,571 ┬▒  3778,558  ops/s
/// StreamableConcatIterablePerf.benchmark     1000  thrpt    5    32123,342 ┬▒   915,151  ops/s
/// StreamableConcatIterablePerf.benchmark    10000  thrpt    5     3328,728 ┬▒    38,444  ops/s
/// StreamableConcatIterablePerf.benchmark   100000  thrpt    5      331,795 ┬▒     4,418  ops/s
/// StreamableConcatIterablePerf.benchmark  1000000  thrpt    5       33,268 ┬▒     0,899  ops/s
/// ```
///
/// # 6. synchronous-biased next()
///
/// +77% short sequences, +762% for long sequences
///
/// ```
/// Benchmark                               (times)   Mode  Cnt        Score        Error  Units
/// StreamableConcatIterablePerf.benchmark        1  thrpt    5  6172450,906 ┬▒  66554,843  ops/s
/// StreamableConcatIterablePerf.benchmark       10  thrpt    5  5367812,331 ┬▒ 115663,523  ops/s
/// StreamableConcatIterablePerf.benchmark      100  thrpt    5  1820204,546 ┬▒ 143049,591  ops/s
/// StreamableConcatIterablePerf.benchmark     1000  thrpt    5   218017,883 ┬▒  49050,865  ops/s
/// StreamableConcatIterablePerf.benchmark    10000  thrpt    5    17406,789 ┬▒   1079,512  ops/s
/// StreamableConcatIterablePerf.benchmark   100000  thrpt    5     2103,477 ┬▒    360,090  ops/s
/// StreamableConcatIterablePerf.benchmark  1000000  thrpt    5      207,888 ┬▒     22,014  ops/s
/// ```
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class StreamableConcatIterablePerf {
    @Param({ "1", "10", "100", "1000", "10000", "100000", "1000000" })
    public int times;

    Streamable<Integer> result;

    @Setup
    public void setup() {
        result = Streamable.concat(List.of(Streamable.range(1, times), Streamable.range(times + 1, times)));
    }

    @Benchmark
    public Object benchmark() {
        return result.blockingLast();
    }
}
