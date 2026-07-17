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
/// Benchmark                     (times)   Mode  Cnt          Score          Error  Units
/// StreamableMapPerf.basic             1  thrpt    5  596170000,249 ┬▒ 58386686,173  ops/s
/// StreamableMapPerf.basic            10  thrpt    5   88996451,855 ┬▒  7879743,705  ops/s
/// StreamableMapPerf.basic           100  thrpt    5   12136698,201 ┬▒  1117754,568  ops/s
/// StreamableMapPerf.basic          1000  thrpt    5     183021,386 ┬▒     7623,362  ops/s
/// StreamableMapPerf.basic         10000  thrpt    5      17876,769 ┬▒     1359,162  ops/s
/// StreamableMapPerf.basic        100000  thrpt    5       1809,422 ┬▒       31,426  ops/s
/// StreamableMapPerf.basic       1000000  thrpt    5        175,036 ┬▒       11,687  ops/s
/// StreamableMapPerf.enumerated        1  thrpt    5   16123906,915 ┬▒  1069401,973  ops/s
/// StreamableMapPerf.enumerated       10  thrpt    5    5693840,612 ┬▒    66583,096  ops/s
/// StreamableMapPerf.enumerated      100  thrpt    5     761972,822 ┬▒    23829,829  ops/s
/// StreamableMapPerf.enumerated     1000  thrpt    5      74078,580 ┬▒     9930,126  ops/s
/// StreamableMapPerf.enumerated    10000  thrpt    5       7340,915 ┬▒     1049,583  ops/s
/// StreamableMapPerf.enumerated   100000  thrpt    5        679,864 ┬▒       11,764  ops/s
/// StreamableMapPerf.enumerated  1000000  thrpt    5         70,816 ┬▒        0,515  ops/s
/// StreamableMapPerf.indexed           1  thrpt    5   11560646,034 ┬▒  1034530,694  ops/s
/// StreamableMapPerf.indexed          10  thrpt    5    5607940,249 ┬▒   305032,627  ops/s
/// StreamableMapPerf.indexed         100  thrpt    5     804783,705 ┬▒    12452,982  ops/s
/// StreamableMapPerf.indexed        1000  thrpt    5      79891,842 ┬▒     8009,334  ops/s
/// StreamableMapPerf.indexed       10000  thrpt    5       8005,019 ┬▒      894,673  ops/s
/// StreamableMapPerf.indexed      100000  thrpt    5        800,412 ┬▒       94,950  ops/s
/// StreamableMapPerf.indexed     1000000  thrpt    5         80,535 ┬▒        1,576  ops/s
/// ```
///
/// # 1. remove current volatile, avoid whenComplete calls when possible
///
/// +86% on the longest basic, +45% on the indexed case (but no fusion yet),
/// -24% on the single element case.
///
/// ```
/// Benchmark                     (times)   Mode  Cnt          Score          Error  Units
/// StreamableMapPerf.basic             1  thrpt    5  455082027,159 ┬▒ 12415682,459  ops/s
/// StreamableMapPerf.basic            10  thrpt    5   81956627,142 ┬▒ 12197594,471  ops/s
/// StreamableMapPerf.basic           100  thrpt    5   11348376,707 ┬▒   812958,859  ops/s
/// StreamableMapPerf.basic          1000  thrpt    5     319588,236 ┬▒    56712,576  ops/s
/// StreamableMapPerf.basic         10000  thrpt    5      33363,038 ┬▒    12143,283  ops/s
/// StreamableMapPerf.basic        100000  thrpt    5       3237,479 ┬▒      308,847  ops/s
/// StreamableMapPerf.basic       1000000  thrpt    5        326,468 ┬▒       57,581  ops/s
/// StreamableMapPerf.enumerated        1  thrpt    5   16730548,276 ┬▒  1509643,233  ops/s
/// StreamableMapPerf.enumerated       10  thrpt    5    7214156,686 ┬▒   204146,871  ops/s
/// StreamableMapPerf.enumerated      100  thrpt    5    1020491,947 ┬▒   108128,423  ops/s
/// StreamableMapPerf.enumerated     1000  thrpt    5      99493,896 ┬▒     3355,031  ops/s
/// StreamableMapPerf.enumerated    10000  thrpt    5      10197,248 ┬▒      809,997  ops/s
/// StreamableMapPerf.enumerated   100000  thrpt    5       1050,388 ┬▒       85,799  ops/s
/// StreamableMapPerf.enumerated  1000000  thrpt    5         99,972 ┬▒        0,566  ops/s
/// StreamableMapPerf.indexed           1  thrpt    5   14901677,115 ┬▒   234094,126  ops/s
/// StreamableMapPerf.indexed          10  thrpt    5    7654935,342 ┬▒    54572,642  ops/s
/// StreamableMapPerf.indexed         100  thrpt    5    1198988,599 ┬▒   211554,095  ops/s
/// StreamableMapPerf.indexed        1000  thrpt    5     115874,465 ┬▒    12076,343  ops/s
/// StreamableMapPerf.indexed       10000  thrpt    5      13358,450 ┬▒      286,266  ops/s
/// StreamableMapPerf.indexed      100000  thrpt    5       1221,610 ┬▒      268,159  ops/s
/// StreamableMapPerf.indexed     1000000  thrpt    5        116,228 ┬▒        1,277  ops/s
/// ```
///
/// # 2. fuse with indexable source
///
/// +158% improvement on the longest chain.
///
/// ```
/// Benchmark                     (times)   Mode  Cnt          Score          Error  Units
/// StreamableMapPerf.basic             1  thrpt    5  458349718,751 ┬▒ 34799991,230  ops/s
/// StreamableMapPerf.basic            10  thrpt    5   77759584,162 ┬▒   484470,610  ops/s
/// StreamableMapPerf.basic           100  thrpt    5   10927029,754 ┬▒   439513,962  ops/s
/// StreamableMapPerf.basic          1000  thrpt    5     486654,649 ┬▒    28948,227  ops/s
/// StreamableMapPerf.basic         10000  thrpt    5      39169,073 ┬▒     1954,250  ops/s
/// StreamableMapPerf.basic        100000  thrpt    5       3823,117 ┬▒      128,942  ops/s
/// StreamableMapPerf.basic       1000000  thrpt    5        385,872 ┬▒       83,309  ops/s
/// StreamableMapPerf.enumerated        1  thrpt    5   17568827,861 ┬▒  1188973,311  ops/s
/// StreamableMapPerf.enumerated       10  thrpt    5    7346307,943 ┬▒    62971,019  ops/s
/// StreamableMapPerf.enumerated      100  thrpt    5     974720,167 ┬▒    55352,752  ops/s
/// StreamableMapPerf.enumerated     1000  thrpt    5     101154,336 ┬▒    10030,194  ops/s
/// StreamableMapPerf.enumerated    10000  thrpt    5      10020,188 ┬▒      154,534  ops/s
/// StreamableMapPerf.enumerated   100000  thrpt    5       1005,707 ┬▒       15,859  ops/s
/// StreamableMapPerf.enumerated  1000000  thrpt    5         98,836 ┬▒        5,676  ops/s
/// StreamableMapPerf.indexed           1  thrpt    5   14395237,213 ┬▒   648713,966  ops/s
/// StreamableMapPerf.indexed          10  thrpt    5   60821106,582 ┬▒  1870263,863  ops/s
/// StreamableMapPerf.indexed         100  thrpt    5    3844820,803 ┬▒   714985,119  ops/s
/// StreamableMapPerf.indexed        1000  thrpt    5     288269,160 ┬▒    33882,613  ops/s
/// StreamableMapPerf.indexed       10000  thrpt    5      29448,704 ┬▒     3040,234  ops/s
/// StreamableMapPerf.indexed      100000  thrpt    5       2941,029 ┬▒       77,884  ops/s
/// StreamableMapPerf.indexed     1000000  thrpt    5        300,212 ┬▒        7,796  ops/s
/// ```
///
/// # 3. use CompletableFuture.state to instead of isDone + isCompletedExceptionally overhead
///
/// +7.7% for times 1, +12.5% for times 10.
/// Still -18% relative to the baseline.
///
/// ```
/// Benchmark                     (times)   Mode  Cnt          Score          Error  Units
/// StreamableMapPerf.basic             1  thrpt    5  493964135,333 ┬▒ 31919189,421  ops/s
/// StreamableMapPerf.basic            10  thrpt    5   87476563,366 ┬▒ 37519691,468  ops/s
/// StreamableMapPerf.basic           100  thrpt    5   10107439,791 ┬▒   267335,311  ops/s
/// StreamableMapPerf.basic          1000  thrpt    5     523564,103 ┬▒   105090,078  ops/s
/// StreamableMapPerf.basic         10000  thrpt    5      36818,972 ┬▒     1944,375  ops/s
/// StreamableMapPerf.basic        100000  thrpt    5       3859,654 ┬▒       46,790  ops/s
/// StreamableMapPerf.basic       1000000  thrpt    5        379,326 ┬▒       43,010  ops/s
/// StreamableMapPerf.enumerated        1  thrpt    5   17760226,834 ┬▒  1599737,873  ops/s
/// StreamableMapPerf.enumerated       10  thrpt    5    7297322,361 ┬▒   105078,437  ops/s
/// StreamableMapPerf.enumerated      100  thrpt    5    1006007,580 ┬▒   121150,559  ops/s
/// StreamableMapPerf.enumerated     1000  thrpt    5     101828,572 ┬▒     9235,915  ops/s
/// StreamableMapPerf.enumerated    10000  thrpt    5      10712,997 ┬▒      271,737  ops/s
/// StreamableMapPerf.enumerated   100000  thrpt    5       1001,925 ┬▒       39,411  ops/s
/// StreamableMapPerf.enumerated  1000000  thrpt    5        100,114 ┬▒        3,677  ops/s
/// StreamableMapPerf.indexed           1  thrpt    5   14962166,447 ┬▒  1351752,478  ops/s
/// StreamableMapPerf.indexed          10  thrpt    5   18176023,720 ┬▒   799380,855  ops/s
/// StreamableMapPerf.indexed         100  thrpt    5    2947669,689 ┬▒   108127,841  ops/s
/// StreamableMapPerf.indexed        1000  thrpt    5     282842,159 ┬▒    47754,366  ops/s
/// StreamableMapPerf.indexed       10000  thrpt    5      28008,555 ┬▒     1513,803  ops/s
/// StreamableMapPerf.indexed      100000  thrpt    5       2830,012 ┬▒       67,427  ops/s
/// StreamableMapPerf.indexed     1000000  thrpt    5        290,707 ┬▒       25,138  ops/s
/// ```
///
/// # 4. enumerable fusion
///
/// +37% on small times, +60% throughput on large times
/// ```
/// enchmark                     (times)   Mode  Cnt          Score           Error  Units
/// StreamableMapPerf.basic             1  thrpt    5  471719643,711 ┬▒ 106189283,745  ops/s
/// StreamableMapPerf.basic            10  thrpt    5   85398301,091 ┬▒  17409080,046  ops/s
/// StreamableMapPerf.basic           100  thrpt    5   10869015,976 ┬▒   1011070,084  ops/s
/// StreamableMapPerf.basic          1000  thrpt    5     549938,103 ┬▒     42822,156  ops/s
/// StreamableMapPerf.basic         10000  thrpt    5      42652,848 ┬▒      4870,733  ops/s
/// StreamableMapPerf.basic        100000  thrpt    5       4219,335 ┬▒       905,798  ops/s
/// StreamableMapPerf.basic       1000000  thrpt    5        420,184 ┬▒        77,257  ops/s
/// StreamableMapPerf.enumerated        1  thrpt    5   24390785,396 ┬▒    748866,046  ops/s
/// StreamableMapPerf.enumerated       10  thrpt    5   32365811,876 ┬▒   2323645,881  ops/s
/// StreamableMapPerf.enumerated      100  thrpt    5    2091169,420 ┬▒    255405,462  ops/s
/// StreamableMapPerf.enumerated     1000  thrpt    5     182290,531 ┬▒      9181,593  ops/s
/// StreamableMapPerf.enumerated    10000  thrpt    5      17774,650 ┬▒       474,070  ops/s
/// StreamableMapPerf.enumerated   100000  thrpt    5       1774,772 ┬▒       112,585  ops/s
/// StreamableMapPerf.enumerated  1000000  thrpt    5        164,270 ┬▒         5,778  ops/s
/// StreamableMapPerf.indexed           1  thrpt    5   14900664,969 ┬▒    213717,815  ops/s
/// StreamableMapPerf.indexed          10  thrpt    5   63037359,537 ┬▒   1085133,528  ops/s
/// StreamableMapPerf.indexed         100  thrpt    5    3160805,643 ┬▒    883774,226  ops/s
/// StreamableMapPerf.indexed        1000  thrpt    5     284112,887 ┬▒     50755,549  ops/s
/// StreamableMapPerf.indexed       10000  thrpt    5      27770,464 ┬▒      1669,842  ops/s
/// StreamableMapPerf.indexed      100000  thrpt    5       2966,157 ┬▒       181,954  ops/s
/// StreamableMapPerf.indexed     1000000  thrpt    5        286,102 ┬▒        52,939  ops/s
/// ```
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class StreamableMapPerf {
    @Param({ "1", "10", "100", "1000", "10000", "100000", "1000000" })
    public int times;

    Streamable<Integer> result;
    Streamable<Optional<Integer>> indexedMax;
    Streamable<Optional<Integer>> enumeratedMax;

    @Setup
    public void setup() {
        result = Streamable.range(1, times).map(v -> v + 1);
        indexedMax = result.collect(Collectors.maxBy(Comparator.naturalOrder()));
        enumeratedMax = Streamable.fromIterable(() -> IntStream.range(1, 1 + times).iterator())
                .map(v -> v + 1)
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
