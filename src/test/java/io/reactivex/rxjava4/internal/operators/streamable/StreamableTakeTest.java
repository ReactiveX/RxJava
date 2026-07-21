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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.processors.DispatchStreamProcessor;

public class StreamableTakeTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        var isCancelled = new AtomicBoolean();

        Flowable.range(1, 10)
        .doOnCancel(() -> isCancelled.set(true))
        .toStreamable()
        .take(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);

        assertTrue(isCancelled.get(), "Cancel was not propagated");
    }

    @Test
    public void fewer() throws Throwable {
        var isCancelled = new AtomicBoolean();

        Flowable.range(1, 4)
        .doOnCancel(() -> isCancelled.set(true))
        .toStreamable()
        .take(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4);

        assertFalse(isCancelled.get(), "Cancel was propagated!");
    }

    @Test
    public void error() {
        Streamable.error(new TestException())
        .take(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleTake() {
        Streamable.range(1, 5)
        .take(3)
        .take(1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1)
        ;
    }

    @Test
    public void cancelled() throws Throwable {
        var dsp = new DispatchStreamProcessor<>();

        var ts = dsp.take(3).test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);
        awaitStreamers(dsp, 1000);

        dsp.next(1).toCompletableFuture().join();
        dsp.next(2).toCompletableFuture().join();
        dsp.next(3).toCompletableFuture().join();
        dsp.next(4).toCompletableFuture().join();

        awaitNoStreamers(dsp, 1000);

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);
    }

    @Test
    public void normalHidden() throws Throwable {
        Streamable.range(1, 10)
        .hide()
        .take(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normalIndexed() throws Throwable {
        Streamable.range(1, 10)
        .take(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normalEnumerable() throws Throwable {
        Streamable.range(1, 10)
        .filter(v -> v >= 2)
        .take(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2, 3, 4, 5, 6);
    }

    @Test
    public void normalEnumerableDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.range(1, 10)
            .filter(v -> v >= 2)
            .take(5)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(2, 3, 4, 5, 6);
        });
    }

    @Test
    public void normalDeferredEnumerable() throws Throwable {
        Single.just(1)
        .flattenAsStreamable(v -> List.of(v, v + 1, v + 2, v + 3, v + 4, v + 5, v + 6))
        .take(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normalIndexedCollect() throws Throwable {
        Streamable.range(1, 10)
        .take(5)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1, 2, 3, 4, 5));
    }

    @Test
    public void normalIndexedCollect2() throws Throwable {
        Streamable.range(1, 3)
        .take(5)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1, 2, 3));
    }

    @Test
    public void normalEnumerableCollect() throws Throwable {
        Streamable.range(1, 10)
        .filter(v -> v >= 2)
        .take(5)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(2, 3, 4, 5, 6));
    }

    @Test
    public void normalDeferredEnumerableCollect() throws Throwable {
        Single.just(1)
        .flattenAsStreamable(v -> List.of(v, v + 1, v + 2, v + 3, v + 4, v + 5, v + 6))
        .take(5)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1, 2, 3, 4, 5));
    }
}
