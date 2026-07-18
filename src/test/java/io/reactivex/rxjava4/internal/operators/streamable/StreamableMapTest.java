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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.processors.DispatchStreamProcessor;
import io.reactivex.rxjava4.schedulers.Schedulers;

public class StreamableMapTest extends StreamableBaseTest {

    @Test
    public void basic() {
        Streamable.range(1, 5)
        .map(v -> v.toString())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("1", "2", "3", "4", "5");
    }

    @Test
    public void basicHidden() {
        Streamable.range(1, 5)
        .hide()
        .map(v -> v.toString())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("1", "2", "3", "4", "5");
    }

    @Test
    public void mapperNull() {
        Streamable.range(1, 5)
        .map(_ -> null)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void error() {
        Streamable.error(new TestException())
        .map(v -> v.toString())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperCrash() {
        Streamable.range(1, 5)
        .map(_ -> { throw new TestException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void delayed() {
        Streamable.range(1, 5)
        .delay(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .map(v -> v.toString())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("1", "2", "3", "4", "5");
    }

    @Test
    public void delayedEmpty() {
        Streamable.empty()
        .delay(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .map(v -> v.toString())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void delayedMapperNull() {
        Streamable.range(1, 5)
        .delay(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .map(_ -> null)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void delayedMapperCrash() {
        Streamable.range(1, 5)
        .delay(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .map(_ -> { throw new TestException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void complete() throws Throwable {
        var dsp = new DispatchStreamProcessor<Integer>();

        var ts = dsp.map(v -> v.toString())
        .test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000);

        dsp.finish(null);

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void indexed() {
        Streamable.range(1, 5)
        .map(v -> v.toString())
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of("1", "2", "3", "4", "5"));
    }

    @Test
    public void indexedNull() {
        Streamable.range(1, 5)
        .map(_ -> null)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void indexedCrash() {
        Streamable.range(1, 5)
        .map(_ -> { throw new TestException(); })
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void enumerated() {
        Streamable.fromIterable(() -> IntStream.range(1, 6).iterator())
        .map(v -> v.toString())
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of("1", "2", "3", "4", "5"));
    }

    @Test
    public void enumeratedNull() {
        Streamable.fromIterable(() -> IntStream.range(1, 6).iterator())
        .map(_ -> null)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void enumeratedCrash() {
        Streamable.fromIterable(() -> IntStream.range(1, 6).iterator())
        .map(_ -> { throw new TestException(); })
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void deferredEnumerable() throws Throwable {
        Single.just(List.of(1, 2, 3, 4, 5))
        .flattenAsStreamable(v -> v)
        .map(v -> v + 1)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(2, 3, 4, 5, 6));
    }

    @Test
    public void deferredEnumerableDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Single.just(List.of(1, 2, 3, 4, 5))
            .flattenAsStreamable(v -> v)
            .map(v -> v + 1)
            .collect(Collectors.toList())
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(List.of(2, 3, 4, 5, 6));
        });
    }

    @Test
    public void indexedToEnumerable() throws Throwable {
        Streamable.range(1, 5)
        .map(v -> v + 1)
        .filter(v -> v % 2 == 0)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(2, 4, 6));
    }
}
