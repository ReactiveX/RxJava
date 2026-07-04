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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.schedulers.Schedulers;

public class StreamableTakeUntilTest extends StreamableBaseTest {

    @Test
    public void passthrough() throws Throwable {
        Streamable.range(1, 5)
        .takeUntil(Streamable.never())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void passthroughError() throws Throwable {
        Streamable.error(new TestException())
        .takeUntil(Streamable.never())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void timer() throws Throwable {
        Streamable.never()
        .takeUntil(Streamable.timer(10, TimeUnit.MILLISECONDS, Schedulers.single()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void empty() throws Throwable {
        Streamable.never()
        .takeUntil(Streamable.empty())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void otherError() throws Throwable {
        Streamable.never()
        .takeUntil(Streamable.error(new TestException()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void realtime() {
        withRetry(3, () -> {
            Streamable.intervalRange(1, 5, 0, 100, TimeUnit.MILLISECONDS, Schedulers.single())
            .takeUntil(Streamable.timer(250, TimeUnit.MILLISECONDS, Schedulers.virtual()))
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1L, 2L, 3L);
        });
    }

    @Test
    public void realtimeDebug() throws Throwable {
        withCachedExecutor(exec -> {
            withRetry(3, () -> {
                Streamable.intervalRange(1, 5, 0, 100, TimeUnit.MILLISECONDS, Schedulers.single())
                .takeUntil(Streamable.timer(250, TimeUnit.MILLISECONDS, Schedulers.virtual()))
                .test(exec)
                .awaitDone(5, TimeUnit.SECONDS)
                .assertResult(1L, 2L, 3L);
            });
        });
    }

    @Test
    public void neverNeverEMpty() throws Throwable {
        Streamable.never()
        .takeUntil(Streamable.never())
        .takeUntil(Streamable.empty())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void longTimer() throws Throwable {
        Streamable.range(1, 5)
        .takeUntil(Streamable.timer(1, TimeUnit.MINUTES, Schedulers.single()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }
}
