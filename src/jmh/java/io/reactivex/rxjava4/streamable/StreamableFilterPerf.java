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
import java.util.concurrent.TimeUnit;
import java.util.stream.*;

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
/// Benchmark                        (times)   Mode  Cnt         Score        Error  Units
/// StreamableFilterPerf.basic             1  thrpt    5  26170031,056 ┬▒ 100892,180  ops/s
/// StreamableFilterPerf.basic            10  thrpt    5   4174832,259 ┬▒ 118915,456  ops/s
/// StreamableFilterPerf.basic           100  thrpt    5    485563,469 ┬▒   6977,640  ops/s
/// StreamableFilterPerf.basic          1000  thrpt    5     52859,526 ┬▒    392,085  ops/s
/// StreamableFilterPerf.basic         10000  thrpt    5      4757,976 ┬▒     67,715  ops/s
/// StreamableFilterPerf.basic        100000  thrpt    5       475,093 ┬▒      6,846  ops/s
/// StreamableFilterPerf.basic       1000000  thrpt    5        47,684 ┬▒      1,866  ops/s
/// StreamableFilterPerf.enumerated        1  thrpt    5   9351050,757 ┬▒ 267315,421  ops/s
/// StreamableFilterPerf.enumerated       10  thrpt    5   3315310,614 ┬▒  72388,028  ops/s
/// StreamableFilterPerf.enumerated      100  thrpt    5    377616,877 ┬▒   8096,437  ops/s
/// StreamableFilterPerf.enumerated     1000  thrpt    5     41714,551 ┬▒    813,698  ops/s
/// StreamableFilterPerf.enumerated    10000  thrpt    5      4084,944 ┬▒     64,690  ops/s
/// StreamableFilterPerf.enumerated   100000  thrpt    5       367,438 ┬▒     10,352  ops/s
/// StreamableFilterPerf.enumerated  1000000  thrpt    5        37,380 ┬▒      0,771  ops/s
/// StreamableFilterPerf.indexed           1  thrpt    5   8582157,437 ┬▒ 215730,754  ops/s
/// StreamableFilterPerf.indexed          10  thrpt    5   3370403,716 ┬▒  44180,727  ops/s
/// StreamableFilterPerf.indexed         100  thrpt    5    392725,429 ┬▒   5825,883  ops/s
/// StreamableFilterPerf.indexed        1000  thrpt    5     42956,064 ┬▒    774,701  ops/s
/// StreamableFilterPerf.indexed       10000  thrpt    5      4274,859 ┬▒     48,781  ops/s
/// StreamableFilterPerf.indexed      100000  thrpt    5       427,545 ┬▒      6,204  ops/s
/// StreamableFilterPerf.indexed     1000000  thrpt    5        38,761 ┬▒      1,574  ops/s
/// ```
///
/// # 1. Avoid whenComplete and tidy up the internals, better wip management
///
///  1 times regression -22%, 1 million case +63%
///
/// ```
/// Benchmark                        (times)   Mode  Cnt         Score        Error  Units
/// StreamableFilterPerf.basic             1  thrpt    5  20450133,688 ┬▒ 292577,808  ops/s
/// StreamableFilterPerf.basic            10  thrpt    5   7001560,283 ┬▒  95757,407  ops/s
/// StreamableFilterPerf.basic           100  thrpt    5    801954,196 ┬▒  25583,620  ops/s
/// StreamableFilterPerf.basic          1000  thrpt    5     81090,359 ┬▒   1422,785  ops/s
/// StreamableFilterPerf.basic         10000  thrpt    5      7861,791 ┬▒    174,594  ops/s
/// StreamableFilterPerf.basic        100000  thrpt    5       791,253 ┬▒     10,498  ops/s
/// StreamableFilterPerf.basic       1000000  thrpt    5        78,027 ┬▒      1,068  ops/s
/// StreamableFilterPerf.enumerated        1  thrpt    5  11758684,924 ┬▒ 308081,461  ops/s
/// StreamableFilterPerf.enumerated       10  thrpt    5   4399154,841 ┬▒ 143732,147  ops/s
/// StreamableFilterPerf.enumerated      100  thrpt    5    581006,390 ┬▒  15226,107  ops/s
/// StreamableFilterPerf.enumerated     1000  thrpt    5     52474,101 ┬▒    900,684  ops/s
/// StreamableFilterPerf.enumerated    10000  thrpt    5      5364,237 ┬▒     55,065  ops/s
/// StreamableFilterPerf.enumerated   100000  thrpt    5       575,620 ┬▒     11,915  ops/s
/// StreamableFilterPerf.enumerated  1000000  thrpt    5        53,243 ┬▒      0,953  ops/s
/// StreamableFilterPerf.indexed           1  thrpt    5  10378233,365 ┬▒ 320717,352  ops/s
/// StreamableFilterPerf.indexed          10  thrpt    5   4450787,764 ┬▒  16570,500  ops/s
/// StreamableFilterPerf.indexed         100  thrpt    5    589007,872 ┬▒  17229,304  ops/s
/// StreamableFilterPerf.indexed        1000  thrpt    5     61222,108 ┬▒   1699,621  ops/s
/// StreamableFilterPerf.indexed       10000  thrpt    5      6181,191 ┬▒    174,083  ops/s
/// StreamableFilterPerf.indexed      100000  thrpt    5       582,777 ┬▒     14,846  ops/s
/// StreamableFilterPerf.indexed     1000000  thrpt    5        54,477 ┬▒      1,366  ops/s
/// ```
///
/// # 2. Synchronous bias via CAS-based state management, via Claude Fable suggestions
///
/// times-1 +70% vs baseline, times million +240% vs baseline
/// ```
/// Benchmark                        (times)   Mode  Cnt         Score         Error  Units
/// StreamableFilterPerf.basic             1  thrpt    5  44649205,329 ┬▒ 1318761,330  ops/s
/// StreamableFilterPerf.basic            10  thrpt    5  14573791,114 ┬▒  307959,449  ops/s
/// StreamableFilterPerf.basic           100  thrpt    5   1704743,707 ┬▒   27297,095  ops/s
/// StreamableFilterPerf.basic          1000  thrpt    5    169935,412 ┬▒    1894,944  ops/s
/// StreamableFilterPerf.basic         10000  thrpt    5     16747,005 ┬▒     149,331  ops/s
/// StreamableFilterPerf.basic        100000  thrpt    5      1630,870 ┬▒      39,286  ops/s
/// StreamableFilterPerf.basic       1000000  thrpt    5       162,566 ┬▒       4,036  ops/s
/// StreamableFilterPerf.enumerated        1  thrpt    5  13220834,886 ┬▒  157990,717  ops/s
/// StreamableFilterPerf.enumerated       10  thrpt    5   6266989,179 ┬▒  168298,570  ops/s
/// StreamableFilterPerf.enumerated      100  thrpt    5    894912,487 ┬▒    4979,227  ops/s
/// StreamableFilterPerf.enumerated     1000  thrpt    5     89633,958 ┬▒    1631,029  ops/s
/// StreamableFilterPerf.enumerated    10000  thrpt    5      8973,714 ┬▒     197,897  ops/s
/// StreamableFilterPerf.enumerated   100000  thrpt    5       887,542 ┬▒      12,736  ops/s
/// StreamableFilterPerf.enumerated  1000000  thrpt    5        88,537 ┬▒       0,994  ops/s
/// StreamableFilterPerf.indexed           1  thrpt    5  11534659,877 ┬▒  281862,191  ops/s
/// StreamableFilterPerf.indexed          10  thrpt    5   6663720,805 ┬▒  127958,849  ops/s
/// StreamableFilterPerf.indexed         100  thrpt    5    973299,004 ┬▒   35818,034  ops/s
/// StreamableFilterPerf.indexed        1000  thrpt    5     98248,858 ┬▒    1445,809  ops/s
/// StreamableFilterPerf.indexed       10000  thrpt    5      9801,805 ┬▒     183,524  ops/s
/// StreamableFilterPerf.indexed      100000  thrpt    5       983,497 ┬▒      23,374  ops/s
/// StreamableFilterPerf.indexed     1000000  thrpt    5        97,605 ┬▒       1,170  ops/s
/// ```
///
/// # 3. Add EnumerableSource and DeferredEnumerableSource paths.
///
/// +193% times 1 vs baseline, +607% times million vs baseline
/// ```
/// Benchmark                        (times)   Mode  Cnt         Score         Error  Units
/// StreamableFilterPerf.basic             1  thrpt    5  44129567,500 ┬▒ 3907413,532  ops/s
/// StreamableFilterPerf.basic            10  thrpt    5  15360293,459 ┬▒  134453,730  ops/s
/// StreamableFilterPerf.basic           100  thrpt    5   1811371,583 ┬▒   32392,354  ops/s
/// StreamableFilterPerf.basic          1000  thrpt    5    163322,591 ┬▒    4944,111  ops/s
/// StreamableFilterPerf.basic         10000  thrpt    5     16124,871 ┬▒     578,538  ops/s
/// StreamableFilterPerf.basic        100000  thrpt    5      1618,494 ┬▒      68,714  ops/s
/// StreamableFilterPerf.basic       1000000  thrpt    5       157,161 ┬▒       1,401  ops/s
/// StreamableFilterPerf.enumerated        1  thrpt    5  27449227,121 ┬▒  639347,534  ops/s
/// StreamableFilterPerf.enumerated       10  thrpt    5  43288619,923 ┬▒  787987,036  ops/s
/// StreamableFilterPerf.enumerated      100  thrpt    5   3013969,186 ┬▒   83719,137  ops/s
/// StreamableFilterPerf.enumerated     1000  thrpt    5    272763,930 ┬▒    4591,739  ops/s
/// StreamableFilterPerf.enumerated    10000  thrpt    5     27699,074 ┬▒     591,416  ops/s
/// StreamableFilterPerf.enumerated   100000  thrpt    5      2769,250 ┬▒      75,561  ops/s
/// StreamableFilterPerf.enumerated  1000000  thrpt    5       264,589 ┬▒       6,425  ops/s
/// StreamableFilterPerf.indexed           1  thrpt    5  11637720,830 ┬▒  318248,700  ops/s
/// StreamableFilterPerf.indexed          10  thrpt    5   6651438,800 ┬▒  179861,878  ops/s
/// StreamableFilterPerf.indexed         100  thrpt    5    978543,147 ┬▒   34745,778  ops/s
/// StreamableFilterPerf.indexed        1000  thrpt    5     97398,438 ┬▒    1071,570  ops/s
/// StreamableFilterPerf.indexed       10000  thrpt    5      9768,628 ┬▒     221,269  ops/s
/// StreamableFilterPerf.indexed      100000  thrpt    5       972,717 ┬▒      31,004  ops/s
/// StreamableFilterPerf.indexed     1000000  thrpt    5        95,972 ┬▒       2,552  ops/s
/// ```
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class StreamableFilterPerf {
    @Param({ "1", "10", "100", "1000", "10000", "100000", "1000000" })
    public int times;

    Streamable<Integer> result;
    Streamable<Optional<Integer>> indexedMax;
    Streamable<Optional<Integer>> enumeratedMax;

    @Setup
    public void setup() {
        result = Streamable.range(1, times).filter(v -> (v & 1) != 0);
        indexedMax = result.collect(Collectors.maxBy(Comparator.naturalOrder()));
        enumeratedMax = Streamable.fromIterable(() -> IntStream.range(1, 1 + times).iterator())
                .filter(v -> (v & 1) != 0)
                .collect(Collectors.maxBy(Comparator.naturalOrder()));
    }

    @Benchmark
    public Object basic() {
        return result.blockingLast();
    }

    @Benchmark
    public Object indexed() {
        return indexedMax.blockingLast();
    }

    @Benchmark
    public Object enumerated() {
        return enumeratedMax.blockingLast();
    }
}
