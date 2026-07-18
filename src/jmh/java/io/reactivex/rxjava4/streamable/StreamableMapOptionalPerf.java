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
/// Benchmark                             (times)   Mode  Cnt         Score        Error  Units
/// StreamableMapOptionalPerf.basic             1  thrpt    5  25824335,224 ┬▒ 307158,457  ops/s
/// StreamableMapOptionalPerf.basic            10  thrpt    5   4182696,220 ┬▒  41200,808  ops/s
/// StreamableMapOptionalPerf.basic           100  thrpt    5    475284,966 ┬▒  10753,634  ops/s
/// StreamableMapOptionalPerf.basic          1000  thrpt    5     46365,872 ┬▒    283,392  ops/s
/// StreamableMapOptionalPerf.basic         10000  thrpt    5      4635,538 ┬▒     54,545  ops/s
/// StreamableMapOptionalPerf.basic        100000  thrpt    5       455,119 ┬▒      7,502  ops/s
/// StreamableMapOptionalPerf.basic       1000000  thrpt    5        50,573 ┬▒      4,533  ops/s
/// StreamableMapOptionalPerf.enumerated        1  thrpt    5  13233507,814 ┬▒ 150935,433  ops/s
/// StreamableMapOptionalPerf.enumerated       10  thrpt    5   3433233,629 ┬▒  83102,643  ops/s
/// StreamableMapOptionalPerf.enumerated      100  thrpt    5    417478,655 ┬▒  19913,394  ops/s
/// StreamableMapOptionalPerf.enumerated     1000  thrpt    5     42681,071 ┬▒    399,061  ops/s
/// StreamableMapOptionalPerf.enumerated    10000  thrpt    5      4662,471 ┬▒    246,930  ops/s
/// StreamableMapOptionalPerf.enumerated   100000  thrpt    5       470,937 ┬▒      6,526  ops/s
/// StreamableMapOptionalPerf.enumerated  1000000  thrpt    5        41,139 ┬▒      1,678  ops/s
/// StreamableMapOptionalPerf.indexed           1  thrpt    5  17011123,156 ┬▒ 430025,426  ops/s
/// StreamableMapOptionalPerf.indexed          10  thrpt    5   3775634,947 ┬▒  36893,569  ops/s
/// StreamableMapOptionalPerf.indexed         100  thrpt    5    448558,858 ┬▒   9594,675  ops/s
/// StreamableMapOptionalPerf.indexed        1000  thrpt    5     49462,107 ┬▒    436,808  ops/s
/// StreamableMapOptionalPerf.indexed       10000  thrpt    5      4979,096 ┬▒     80,306  ops/s
/// StreamableMapOptionalPerf.indexed      100000  thrpt    5       499,161 ┬▒      2,866  ops/s
/// StreamableMapOptionalPerf.indexed     1000000  thrpt    5        47,332 ┬▒      1,081  ops/s
/// ```
///
/// # 1. Use the Claude Fable state machine for avoiding whenComplete and atomics
///
/// + 72% on times 1 vs baseline, +212% on times million vs bsaeline
/// ```
/// Benchmark                             (times)   Mode  Cnt         Score        Error  Units
/// StreamableMapOptionalPerf.basic             1  thrpt    5  44420406,456 ┬▒ 467265,689  ops/s
/// StreamableMapOptionalPerf.basic            10  thrpt    5  14548571,112 ┬▒ 204608,524  ops/s
/// StreamableMapOptionalPerf.basic           100  thrpt    5   1696277,847 ┬▒  26481,422  ops/s
/// StreamableMapOptionalPerf.basic          1000  thrpt    5    171547,633 ┬▒   5148,566  ops/s
/// StreamableMapOptionalPerf.basic         10000  thrpt    5     16837,603 ┬▒    237,627  ops/s
/// StreamableMapOptionalPerf.basic        100000  thrpt    5      1556,065 ┬▒     18,238  ops/s
/// StreamableMapOptionalPerf.basic       1000000  thrpt    5       157,966 ┬▒      3,666  ops/s
/// StreamableMapOptionalPerf.enumerated        1  thrpt    5  20465896,376 ┬▒ 338492,857  ops/s
/// StreamableMapOptionalPerf.enumerated       10  thrpt    5  10045416,375 ┬▒ 119490,078  ops/s
/// StreamableMapOptionalPerf.enumerated      100  thrpt    5   1403277,356 ┬▒  26248,074  ops/s
/// StreamableMapOptionalPerf.enumerated     1000  thrpt    5    126858,293 ┬▒   1749,556  ops/s
/// StreamableMapOptionalPerf.enumerated    10000  thrpt    5     12249,540 ┬▒    173,441  ops/s
/// StreamableMapOptionalPerf.enumerated   100000  thrpt    5      1248,849 ┬▒     72,192  ops/s
/// StreamableMapOptionalPerf.enumerated  1000000  thrpt    5       125,087 ┬▒      2,269  ops/s
/// StreamableMapOptionalPerf.indexed           1  thrpt    5  23867521,911 ┬▒ 794423,143  ops/s
/// StreamableMapOptionalPerf.indexed          10  thrpt    5  10788609,481 ┬▒ 266403,589  ops/s
/// StreamableMapOptionalPerf.indexed         100  thrpt    5   1595466,285 ┬▒  31921,714  ops/s
/// StreamableMapOptionalPerf.indexed        1000  thrpt    5    159969,124 ┬▒   2228,065  ops/s
/// StreamableMapOptionalPerf.indexed       10000  thrpt    5     16042,879 ┬▒    268,651  ops/s
/// StreamableMapOptionalPerf.indexed      100000  thrpt    5      1586,144 ┬▒     20,156  ops/s
/// StreamableMapOptionalPerf.indexed     1000000  thrpt    5       152,721 ┬▒      5,313  ops/s
/// ```
///
/// # 2. add Deferred and direct EnumerableSource paths
///
/// Basic has too big of a variance compared to 1.
/// Enumerated: +46% over 1, +126% over baseline for times 1
///             +68 over 1, +413% over baseline for times million
///
/// ```
/// Benchmark                             (times)   Mode  Cnt         Score          Error  Units
/// StreamableMapOptionalPerf.basic             1  thrpt    5  61437824,388 ┬▒ 19274231,452  ops/s
/// StreamableMapOptionalPerf.basic            10  thrpt    5  16686712,361 ┬▒  1317978,227  ops/s
/// StreamableMapOptionalPerf.basic           100  thrpt    5   2037714,671 ┬▒    59005,537  ops/s
/// StreamableMapOptionalPerf.basic          1000  thrpt    5    185415,957 ┬▒    26578,676  ops/s
/// StreamableMapOptionalPerf.basic         10000  thrpt    5     18706,795 ┬▒      384,281  ops/s
/// StreamableMapOptionalPerf.basic        100000  thrpt    5      1522,088 ┬▒       43,043  ops/s
/// StreamableMapOptionalPerf.basic       1000000  thrpt    5       170,135 ┬▒       11,148  ops/s
/// StreamableMapOptionalPerf.enumerated        1  thrpt    5  29930443,412 ┬▒  1169887,938  ops/s
/// StreamableMapOptionalPerf.enumerated       10  thrpt    5  52029667,470 ┬▒  2723918,189  ops/s
/// StreamableMapOptionalPerf.enumerated      100  thrpt    5   6716969,521 ┬▒  1999419,008  ops/s
/// StreamableMapOptionalPerf.enumerated     1000  thrpt    5    234588,596 ┬▒     7713,319  ops/s
/// StreamableMapOptionalPerf.enumerated    10000  thrpt    5     24133,205 ┬▒      725,742  ops/s
/// StreamableMapOptionalPerf.enumerated   100000  thrpt    5      2209,969 ┬▒      240,498  ops/s
/// StreamableMapOptionalPerf.enumerated  1000000  thrpt    5       211,364 ┬▒       24,358  ops/s
/// StreamableMapOptionalPerf.indexed           1  thrpt    5  91243822,427 ┬▒  2337628,568  ops/s
/// StreamableMapOptionalPerf.indexed          10  thrpt    5  51131100,389 ┬▒  5848224,433  ops/s
/// StreamableMapOptionalPerf.indexed         100  thrpt    5   4407887,744 ┬▒   240068,362  ops/s
/// StreamableMapOptionalPerf.indexed        1000  thrpt    5    452836,278 ┬▒   113450,062  ops/s
/// StreamableMapOptionalPerf.indexed       10000  thrpt    5     44535,947 ┬▒    10809,809  ops/s
/// StreamableMapOptionalPerf.indexed      100000  thrpt    5      4528,836 ┬▒      509,763  ops/s
/// StreamableMapOptionalPerf.indexed     1000000  thrpt    5       411,513 ┬▒       10,556  ops/s
/// ```
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class StreamableMapOptionalPerf {
    @Param({ "1", "10", "100", "1000", "10000", "100000", "1000000" })
    public int times;

    Streamable<Integer> result;
    Streamable<Optional<Integer>> indexedMax;
    Streamable<Optional<Integer>> enumeratedMax;

    @Setup
    public void setup() {
        result = Streamable.range(1, times)
                .mapOptional(v -> (v & 1) != 0 ? Optional.of(v + 1) : Optional.empty())
            ;
        indexedMax = result.collect(Collectors.maxBy(Comparator.naturalOrder()));
        enumeratedMax = Streamable.fromIterable(() -> IntStream.range(1, 1 + times).iterator())
                .mapOptional(v -> (v & 1) != 0 ? Optional.of(v + 1) : Optional.empty())
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
