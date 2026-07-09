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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.core.Streamable;
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
}
