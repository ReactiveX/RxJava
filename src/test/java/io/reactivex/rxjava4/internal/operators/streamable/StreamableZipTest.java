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

package io.reactivex.rxjava4.internal.operators.streamable;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.*;

public class StreamableZipTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.zip(List.of(Streamable.range(1, 5), Streamable.range(6, 5)))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1, 6), List.of(2, 7), List.of(3, 8), List.of(4, 9), List.of(5, 10));
    }

    @Test
    public void single() throws Throwable {
        Streamable.zip(List.of(Streamable.range(1, 5)))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1), List.of(2), List.of(3), List.of(4), List.of(5));
    }

    @Test
    public void singleDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.zip(List.of(Streamable.range(1, 5)))
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(List.of(1), List.of(2), List.of(3), List.of(4), List.of(5));
        });
    }

    @Test
    public void crash() throws Throwable {
        Streamable.zip(List.of(Streamable.error(new TestException()), Streamable.range(6, 5)))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void differentSizes() throws Throwable {
        Streamable.zip(List.of(Streamable.range(1, 4), Streamable.range(6, 5)))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1, 6), List.of(2, 7), List.of(3, 8), List.of(4, 9));
    }

    @Test
    public void differentSizes2() throws Throwable {
        Streamable.zip(List.of(Streamable.range(1, 5), Streamable.range(6, 4)))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1, 6), List.of(2, 7), List.of(3, 8), List.of(4, 9));
    }

    @Test
    public void empty() throws Throwable {
        Streamable.zip(List.of())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void oneIsNull() throws Throwable {
        Streamable.zip(Arrays.asList(Streamable.just(1), null))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void just() throws Throwable {
        Streamable.zip(List.of(Streamable.just(1), Streamable.just(6)))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1, 6));
    }

    @Test
    public void finishCrash() throws Throwable {
        Streamable.zip(List.of(Streamable.just(1), StreamableFailingFinish.MAIN_COMPLETES))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void finishCrash2() throws Throwable {
        Streamable.zip(List.of(StreamableFailingFinish.MAIN_COMPLETES, StreamableFailingFinish.MAIN_COMPLETES))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(CompositeException.class);
    }
}
