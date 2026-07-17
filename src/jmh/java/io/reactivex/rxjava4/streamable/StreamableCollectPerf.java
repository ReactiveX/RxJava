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

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.openjdk.jmh.annotations.*;

import io.reactivex.rxjava4.core.*;

///
/// Measure how much overhead and optimization is possible with the trampoline-reducer design
/// of operators where each upstream item normally incurs 2 atomic operation due to needing
/// a non-reentrant and serialized method [Streamer#next()] and [Streamer#finish()] calls,
/// plus the cost of [CompletionStage#whenComplete(java.util.function.BiConsumer)] which allocates
/// a continuation unnecessary for our particular operator.
///
/// Specs: i7 13700K, 64GB DDR5 4800MT CL40 RAM, Windows 11 25H2, Adoptium 26.0.1.8
///
/// ## 0 - Standard implementation
///
/// ```
/// Benchmark                        (times)   Mode  Cnt         Score        Error  Units
/// StreamableCollectPerf.benchmark        1  thrpt    5  13655087,463 ┬▒ 101002,376  ops/s
/// StreamableCollectPerf.benchmark       10  thrpt    5   4345229,158 ┬▒  57122,655  ops/s
/// StreamableCollectPerf.benchmark      100  thrpt    5    565231,578 ┬▒  10040,906  ops/s
/// StreamableCollectPerf.benchmark     1000  thrpt    5     57845,486 ┬▒    459,777  ops/s
/// StreamableCollectPerf.benchmark    10000  thrpt    5      5572,254 ┬▒    100,655  ops/s
/// StreamableCollectPerf.benchmark   100000  thrpt    5       556,943 ┬▒      5,274  ops/s
/// StreamableCollectPerf.benchmark  1000000  thrpt    5        56,532 ┬▒      1,475  ops/s
/// ```
///
/// ## 1 - Avoid decrementing the `wip` counter as much as possible
///
/// +21% on 1M
///
/// ```
/// Benchmark                        (times)   Mode  Cnt         Score        Error  Units
/// StreamableCollectPerf.benchmark        1  thrpt    5  14576437,071 ┬▒ 213560,086  ops/s
/// StreamableCollectPerf.benchmark       10  thrpt    5   5136926,834 ┬▒  75954,060  ops/s
/// StreamableCollectPerf.benchmark      100  thrpt    5    682086,450 ┬▒  12237,157  ops/s
/// StreamableCollectPerf.benchmark     1000  thrpt    5     69919,864 ┬▒   1098,636  ops/s
/// StreamableCollectPerf.benchmark    10000  thrpt    5      6810,584 ┬▒    214,358  ops/s
/// StreamableCollectPerf.benchmark   100000  thrpt    5       679,188 ┬▒      9,162  ops/s
/// StreamableCollectPerf.benchmark  1000000  thrpt    5        68,844 ┬▒      6,202  ops/s
/// ```
///
/// ## 2 - Avoid calling whenComplete
///
/// + 21% vs optimization 1, + 47% vs original
///
/// ```
/// Benchmark                        (times)   Mode  Cnt         Score       Error  Units
/// StreamableCollectPerf.benchmark        1  thrpt    5  16142384,643 ┬▒ 35614,613  ops/s
/// StreamableCollectPerf.benchmark       10  thrpt    5   6145162,207 ┬▒ 81621,754  ops/s
/// StreamableCollectPerf.benchmark      100  thrpt    5    832527,797 ┬▒ 13667,557  ops/s
/// StreamableCollectPerf.benchmark     1000  thrpt    5     83811,957 ┬▒  2609,034  ops/s
/// StreamableCollectPerf.benchmark    10000  thrpt    5      8411,203 ┬▒    31,486  ops/s
/// StreamableCollectPerf.benchmark   100000  thrpt    5       836,305 ┬▒    14,383  ops/s
/// StreamableCollectPerf.benchmark  1000000  thrpt    5        83,539 ┬▒     1,681  ops/s
/// ```
///
///  ## 3 - Using a synchronous indexer
///
/// +413% vs optimization 2, +659% vs original
///
/// ```
/// Benchmark                        (times)   Mode  Cnt         Score        Error  Units
/// StreamableCollectPerf.benchmark        1  thrpt    5  16030652,636 ┬▒ 106257,177  ops/s
/// StreamableCollectPerf.benchmark       10  thrpt    5  56457006,395 ┬▒ 407256,208  ops/s
/// StreamableCollectPerf.benchmark      100  thrpt    5  22649008,057 ┬▒  87657,793  ops/s
/// StreamableCollectPerf.benchmark     1000  thrpt    5    449961,073 ┬▒   4267,484  ops/s
/// StreamableCollectPerf.benchmark    10000  thrpt    5     45892,148 ┬▒    367,249  ops/s
/// StreamableCollectPerf.benchmark   100000  thrpt    5      4548,625 ┬▒     15,360  ops/s
/// StreamableCollectPerf.benchmark  1000000  thrpt    5       429,078 ┬▒      6,829  ops/s
/// ```
///
/// ## 4 - Removing the unnecessary volatile and double bookkeeping from [Streamable#range(int, int)]
///
/// +105% vs original
///
/// ```
/// Benchmark                              (times)   Mode  Cnt         Score        Error  Units
/// StreamableCollectPerf.benchmarkHidden        1  thrpt    5  16018548,246 ┬▒ 134658,704  ops/s
/// StreamableCollectPerf.benchmarkHidden       10  thrpt    5   7977042,864 ┬▒ 126909,304  ops/s
/// StreamableCollectPerf.benchmarkHidden      100  thrpt    5   1140735,027 ┬▒  14580,951  ops/s
/// StreamableCollectPerf.benchmarkHidden     1000  thrpt    5    115843,240 ┬▒   1519,896  ops/s
/// StreamableCollectPerf.benchmarkHidden    10000  thrpt    5     11606,956 ┬▒    181,325  ops/s
/// StreamableCollectPerf.benchmarkHidden   100000  thrpt    5      1161,570 ┬▒     20,902  ops/s
/// StreamableCollectPerf.benchmarkHidden  1000000  thrpt    5       116,332 ┬▒      1,313  ops/s
/// ```
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class StreamableCollectPerf {
    @Param({ "1", "10", "100", "1000", "10000", "100000", "1000000" })
    public int times;

    Streamable<Optional<Integer>> result;

    Streamable<Optional<Integer>> resultHidden;

    @Setup
    public void setup() {
        result = Streamable.range(1, times).collect(Collectors.maxBy(Comparator.naturalOrder()));
        resultHidden = Streamable.range(1, times).hide().collect(Collectors.maxBy(Comparator.naturalOrder()));
    }

    @Benchmark
    public Object benchmark() {
        return result.blockingFirst();
    }

    @Benchmark
    public Object benchmarkHidden() {
        return resultHidden.blockingFirst();
    }
}
