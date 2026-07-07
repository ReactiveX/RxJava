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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;

public class StreamableConcatIterableTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.concat(List.of(Streamable.range(1, 5), Streamable.range(6, 5), Streamable.range(11, 5)))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(
                1, 2, 3, 4, 5,
                6, 7, 8, 9, 10,
                11, 12, 13, 14, 15
                );
    }

    @Test
    public void crash() throws Throwable {
        Streamable.concat(List.of(Streamable.range(1, 5), Streamable.error(new TestException()), Streamable.range(11, 5)))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class,
                1, 2, 3, 4, 5);
    }

    @Test
    public void none() throws Throwable {
        Streamable.concat(List.of())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void solo() throws Throwable {
        Streamable.concat(List.of(Streamable.range(1, 5)))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void nullStreamable() throws Throwable {
        Streamable.concat(Arrays.asList(Streamable.range(1, 5), null))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void lot() {
        var n = 10_000;
        List<Streamable<Integer>> sources = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            sources.add(Streamable.just(i));
        }

        var ts = Streamable.concat(sources)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(n)
        .assertNoErrors()
        .assertComplete();

        for (int i = 0; i < n; i++) {
            assertEquals(i, ts.values().get(i));
        }
    }

    @Test
    public void finishCrash() throws Throwable {
        Streamable.concat(List.of(Streamable.empty(), StreamableFailingFinish.MAIN_COMPLETES))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void finishCrashDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.concat(List.of(Streamable.empty(), StreamableFailingFinish.MAIN_COMPLETES))
            .test(exec)
            .awaitDone(5, TimeUnit.MINUTES)
            .assertFailure(TestException.class);
        });
    }

    @Test
    public void lotEmpties() {
        var n = 10_000;
        List<Streamable<Integer>> sources = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            sources.add(Streamable.empty());
        }

        Streamable.concat(sources)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }
}
