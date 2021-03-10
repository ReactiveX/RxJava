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

package io.reactivex.rxjava3.internal.operators.single;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;

public class SingleZipTest extends RxJavaTest {

    @Test
    public void zip2() {
        Single.zip(Single.just(1), Single.just(2), (BiFunction<Integer, Integer, Object>) (a, b) -> a + "" + b)
        .test()
        .assertResult("12");
    }

    @Test
    public void zip3() {
        Single.zip(Single.just(1), Single.just(2), Single.just(3), (Function3<Integer, Integer, Integer, Object>) (a, b, c) -> a + "" + b + c)
        .test()
        .assertResult("123");
    }

    @Test
    public void zip4() {
        Single.zip(Single.just(1), Single.just(2), Single.just(3),
                Single.just(4),
                (Function4<Integer, Integer, Integer, Integer, Object>) (a, b, c, d) -> a + "" + b + c + d)
        .test()
        .assertResult("1234");
    }

    @Test
    public void zip5() {
        Single.zip(Single.just(1), Single.just(2), Single.just(3),
                Single.just(4), Single.just(5),
                (Function5<Integer, Integer, Integer, Integer, Integer, Object>) (a, b, c, d, e) -> a + "" + b + c + d + e)
        .test()
        .assertResult("12345");
    }

    @Test
    public void zip6() {
        Single.zip(Single.just(1), Single.just(2), Single.just(3),
                Single.just(4), Single.just(5), Single.just(6),
                (Function6<Integer, Integer, Integer, Integer, Integer, Integer, Object>) (a, b, c, d, e, f) -> a + "" + b + c + d + e + f)
        .test()
        .assertResult("123456");
    }

    @Test
    public void zip7() {
        Single.zip(Single.just(1), Single.just(2), Single.just(3),
                Single.just(4), Single.just(5), Single.just(6),
                Single.just(7),
                (Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Object>) (a, b, c, d, e, f, g) -> a + "" + b + c + d + e + f + g)
        .test()
        .assertResult("1234567");
    }

    @Test
    public void zip8() {
        Single.zip(Single.just(1), Single.just(2), Single.just(3),
                Single.just(4), Single.just(5), Single.just(6),
                Single.just(7), Single.just(8),
                (Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Object>) (a, b, c, d, e, f, g, h) -> a + "" + b + c + d + e + f + g + h)
        .test()
        .assertResult("12345678");
    }

    @Test
    public void zip9() {
        Single.zip(Single.just(1), Single.just(2), Single.just(3),
                Single.just(4), Single.just(5), Single.just(6),
                Single.just(7), Single.just(8), Single.just(9),
                (Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Object>) (a, b, c, d, e, f, g, h, i) -> a + "" + b + c + d + e + f + g + h + i)
        .test()
        .assertResult("123456789");
    }

    @Test
    public void noDisposeOnAllSuccess() {
        final AtomicInteger counter = new AtomicInteger();

        Single<Integer> source = Single.just(1).doOnDispose(counter::getAndIncrement);

        Single.zip(source, source, (BiFunction<Integer, Integer, Object>) Integer::sum)
        .test()
        .assertResult(2);

        assertEquals(0, counter.get());
    }

    @Test
    public void noDisposeOnAllSuccess2() {
        final AtomicInteger counter = new AtomicInteger();

        Single<Integer> source = Single.just(1).doOnDispose(counter::getAndIncrement);

        Single.zip(Arrays.asList(source, source), (Function<Object[], Object>) o -> (Integer)o[0] + (Integer)o[1])
        .test()
        .assertResult(2);

        assertEquals(0, counter.get());
    }
}
