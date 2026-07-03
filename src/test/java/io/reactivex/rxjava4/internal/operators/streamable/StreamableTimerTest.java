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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class StreamableTimerTest extends StreamableBaseTest {

    @Test
    public void basic() {
        Streamable.timer(100, TimeUnit.MILLISECONDS, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0L);
    }

    @Test
    public void basicExecutor() throws Throwable {
        withCachedExecutor(exec -> {;
            Streamable.timer(100, TimeUnit.MILLISECONDS, exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(0L);
        });
    }

    @Test
    public void basicVirtual() throws Throwable {
        withVirtual(exec -> {;
            Streamable.timer(100, TimeUnit.MILLISECONDS, exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(0L);
        });
    }

    @Test
    public void basicScheduled() throws Throwable {
        try (var exec = Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory())) {
            Streamable.timer(100, TimeUnit.MILLISECONDS, exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(0L);
        }
    }

    @Test
    public void neitherSchedulerNoExecutor() {
        assertThrows(IllegalArgumentException.class, () -> {
            new StreamableTimer(1, TimeUnit.SECONDS, null, null);
        });
    }

    @Test
    public void cancelBeforeTimerHit() {
        assertThrows(CancellationException.class, () -> {
            var awaitable = Streamable.timer(10, TimeUnit.MINUTES, Schedulers.single())
            .forEach(_ -> { });

            Thread.sleep(100);

            awaitable.close();

            awaitable.await();
        });
    }

    @Test
    public void cancelBeforeTimerHitExecutorService() {
        assertThrows(CancellationException.class, () -> {
            withCachedExecutor(exec -> {
                var awaitable = Streamable.timer(10, TimeUnit.MINUTES, exec)
                        .forEach(_ -> { });

                        Thread.sleep(100);

                        awaitable.close();

                        awaitable.await();
            });
        });
    }

    @Test
    public void cancelBeforeTimerHitScheduledExecutorService() {
        assertThrows(CancellationException.class, () -> {
            try (var exec = Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory())) {
                var awaitable = Streamable.timer(10, TimeUnit.MINUTES, exec)
                        .forEach(_ -> { });

                        Thread.sleep(100);

                        awaitable.close();

                        awaitable.await();
            };
        });
    }

    @Test
    public void disposable() {
        TestHelper.checkDisposed(new StreamableTimer.TimerStreamer());
    }

    @Test
    public void basicZeroDelay() {
        Streamable.timer(0, TimeUnit.MILLISECONDS, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0L);
    }

    @Test
    public void basicExecutoreroDelay() throws Throwable {
        withCachedExecutor(exec -> {;
            Streamable.timer(0, TimeUnit.MILLISECONDS, exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(0L);
        });
    }

    @Test
    public void basicVirtualeroDelay() throws Throwable {
        withVirtual(exec -> {;
            Streamable.timer(0, TimeUnit.MILLISECONDS, exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(0L);
        });
    }

    @Test
    public void basicSchedulederoDelay() throws Throwable {
        try (var exec = Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory())) {
            Streamable.timer(0, TimeUnit.MILLISECONDS, exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(0L);
        }
    }

}
