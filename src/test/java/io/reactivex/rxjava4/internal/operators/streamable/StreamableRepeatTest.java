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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;

public class StreamableRepeatTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.just(1)
        .repeat(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void zero() throws Throwable {
        Streamable.just(1)
        .repeat(0)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void one() throws Throwable {
        Streamable.just(1)
        .repeat(1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void empty() throws Throwable {
        Streamable.empty()
        .repeat(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void error() throws Throwable {
        Streamable.error(new TestException())
        .repeat(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void finishCrash() throws Throwable {
        StreamableFailingFinish.MAIN_COMPLETES
        .repeat(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void normalDeferred() throws Throwable {
        var counter = new AtomicInteger();
        Streamable.defer(() -> Streamable.just(counter.incrementAndGet()))
        .repeat(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normalDontRepeat() throws Throwable {
        Streamable.range(1, 5)
        .repeatWhen(_ -> CompletableFuture.completedStage(false))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void failDontRepeat() throws Throwable {
        Streamable.range(1, 5)
        .repeatWhen(_ -> CompletableFuture.failedStage(new TestException()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void timeoutWhen() throws Throwable {
        var ts = Streamable.empty()
        .repeatWhen(_ -> new CompletableFuture<>())
        .test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        Thread.sleep(100);

        ts.cancel();
    }

    @Test
    public void functionCrashes() throws Throwable {
        Streamable.just(1)
        .repeatWhen(_ -> { throw new TestException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void functionCrashesDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.just(1)
            .repeatWhen(_ -> { throw new TestException(); })
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(TestException.class, 1);
        });
    }

    @Test
    public void functionCrashesEmptyDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.empty()
            .repeatWhen(_ -> { throw new TestException(); })
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(TestException.class);
        });
    }
}
