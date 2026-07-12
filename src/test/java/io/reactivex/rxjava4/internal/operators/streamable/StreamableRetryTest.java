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

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;

public class StreamableRetryTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.range(1, 5)
        .retry(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void errorDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.error(new TestException())
            .retry(5)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(TestException.class);
        });
    }

    @Test
    public void error() throws Throwable {
        Streamable.error(new TestException())
        .retry(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void firstErrors() throws Throwable {
        AtomicInteger counter = new AtomicInteger();
        Streamable.defer(() -> {
            if (counter.getAndIncrement() == 0) {
                return Streamable.error(new TestException());
            }
            return Streamable.range(1, 5);
        })
        .retry(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void firstErrorsDebug() throws Throwable {
        withCachedExecutor(exec -> {
            AtomicInteger counter = new AtomicInteger();
            Streamable.defer(() -> {
                if (counter.getAndIncrement() == 0) {
                    return Streamable.error(new TestException());
                }
                return Streamable.range(1, 5);
            })
            .retry(5)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5);
        });
    }

    @Test
    public void functionCrashes() throws Throwable {
        Streamable.error(new TestException())
        .retry(_ -> { throw new IOException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class)
        .assertError(e -> e.getSuppressed()[0] instanceof TestException);
    }

    @Test
    public void errorComplete() throws Throwable {
        Streamable.error(new TestException())
        .retryWhen((_, _) -> CompletableFuture.completedStage(false))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void errorAndFinishCrash() throws Throwable {
        StreamableFailingFinish.MAIN_FAILS
        .retryWhen((_, _) -> CompletableFuture.completedStage(false))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void timeoutWhen() throws Throwable {
        var ts = Streamable.error(new TestException())
        .retryWhen((_, _) -> new CompletableFuture<>())
        .test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        Thread.sleep(100);

        ts.cancel();
    }

    @Test
    public void retryPredicateFalse() {
        Streamable.error(new TestException())
        .retry(_ -> false)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void retryBiPredicateFalse() {
        Streamable.error(new TestException())
        .retry((_, _) -> false)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void retryBiPredicateTrueOnce() {
        AtomicInteger counter = new AtomicInteger();
        Streamable.defer(() -> {
            if (counter.getAndIncrement() == 0) {
                return Streamable.error(new TestException());
            }
            return Streamable.range(1, 5);
        })
        .retry((c, _) -> c < 1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void retryPredicateTrueOnce() {
        AtomicInteger counter = new AtomicInteger();
        Streamable.defer(() -> {
            if (counter.getAndIncrement() == 0) {
                return Streamable.error(new TestException());
            }
            return Streamable.range(1, 5);
        })
        .retry(_ -> true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }
}
