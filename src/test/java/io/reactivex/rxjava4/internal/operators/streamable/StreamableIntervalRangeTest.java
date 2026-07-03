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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class StreamableIntervalRangeTest extends StreamableBaseTest {

    @Test
    public void basic() {
        Streamable.intervalRange(1, 5, 20, 20, TimeUnit.MILLISECONDS, Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    public void basicVirtual() throws Throwable {
        withVirtual(exec -> {
            Streamable.intervalRange(1, 5, 20, 20, TimeUnit.MILLISECONDS, exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1L, 2L, 3L, 4L, 5L);
        });
    }

    @Test
    public void basicExecutor() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.intervalRange(1, 5, 20, 20, TimeUnit.MILLISECONDS, exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1L, 2L, 3L, 4L, 5L);
        });
    }

    @Test
    public void basicVirtualSchedule() throws Throwable {
        withVirtualScheduled(exec -> {
            Streamable.intervalRange(1, 5, 20, 20, TimeUnit.MILLISECONDS, exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1L, 2L, 3L, 4L, 5L);
        });
    }

    @Test
    public void singleStep() {
        var streamer = new StreamableIntervalRange.IntervalStreamer(1, 6);

        streamer.run();

        assertTrue(streamer.next().toCompletableFuture().join(), "next-join");

        assertEquals(1L, streamer.current());

        var cf = streamer.next().toCompletableFuture();

        streamer.run();

        assertTrue(cf.join(), "next-join-2");

        assertEquals(2L, streamer.current());

        streamer.run();
        streamer.run();

        cf = streamer.next().toCompletableFuture();
        assertTrue(cf.join(), "next-join-3");

        assertEquals(3L, streamer.current());

        cf = streamer.next().toCompletableFuture();
        assertTrue(cf.join(), "next-join-4");

        assertEquals(4L, streamer.current());

        streamer.run();

        cf = streamer.next().toCompletableFuture();
        assertTrue(cf.join(), "next-join-5");

        assertEquals(5, streamer.current());

        cf = streamer.next().toCompletableFuture();
        assertFalse(cf.join(), "next-join-6");

        streamer.finish().toCompletableFuture().join();

        assertTrue(streamer.isDisposed(), "Streamer is not diposed");
    }

    @Test
    public void singleStep2() {
        var streamer = new StreamableIntervalRange.IntervalStreamer(1, 6);

        streamer.run();

        assertTrue(streamer.next().toCompletableFuture().join(), "next-join");

        assertEquals(1L, streamer.current());

        var cf = streamer.next().toCompletableFuture();

        streamer.run();

        assertTrue(cf.join(), "next-join-2");

        assertEquals(2L, streamer.current());

        streamer.run();
        streamer.run();

        cf = streamer.next().toCompletableFuture();
        assertTrue(cf.join(), "next-join-3");

        assertEquals(3L, streamer.current());

        cf = streamer.next().toCompletableFuture();
        assertTrue(cf.join(), "next-join-4");

        assertEquals(4L, streamer.current());

        cf = streamer.next().toCompletableFuture();

        streamer.run();

        assertTrue(cf.join(), "next-join-5");

        assertEquals(5, streamer.current());

        cf = streamer.next().toCompletableFuture();
        assertFalse(cf.join(), "next-join-6");

        streamer.finish().toCompletableFuture().join();

        assertTrue(streamer.isDisposed(), "Streamer is not diposed");
    }

    @Test
    public void singleStep3() {
        var streamer = new StreamableIntervalRange.IntervalStreamer(1, 6);

        streamer.run();
        streamer.run();
        streamer.run();
        streamer.run();
        streamer.run();

        assertTrue(streamer.next().toCompletableFuture().join(), "next-join");

        assertEquals(1L, streamer.current());

        var cf = streamer.next().toCompletableFuture();

        assertTrue(cf.join(), "next-join-2");

        assertEquals(2L, streamer.current());

        cf = streamer.next().toCompletableFuture();
        assertTrue(cf.join(), "next-join-3");

        assertEquals(3L, streamer.current());

        cf = streamer.next().toCompletableFuture();
        assertTrue(cf.join(), "next-join-4");

        assertEquals(4L, streamer.current());

        cf = streamer.next().toCompletableFuture();
        assertTrue(cf.join(), "next-join-5");

        assertEquals(5, streamer.current());

        cf = streamer.next().toCompletableFuture();
        assertFalse(cf.join(), "next-join-6");

        streamer.finish().toCompletableFuture().join();

        assertTrue(streamer.isDisposed(), "Streamer is not diposed");
    }

    @Test
    public void disposable() {
        TestHelper.checkDisposed(new StreamableIntervalRange.IntervalStreamer(1, 6));
    }

    @Test
    public void singleStep4() {
        var streamer = new StreamableIntervalRange.IntervalStreamer(1, 6);

        streamer.run();
        streamer.run();
        streamer.run();

        assertTrue(streamer.next().toCompletableFuture().join(), "next-join");

        assertEquals(1L, streamer.current());

        var cf = streamer.next().toCompletableFuture();

        assertTrue(cf.join(), "next-join-2");

        assertEquals(2L, streamer.current());

        cf = streamer.next().toCompletableFuture();
        assertTrue(cf.join(), "next-join-3");

        assertEquals(3L, streamer.current());

        cf = streamer.next().toCompletableFuture();

        streamer.dispose();

        var cff = cf;
        var e = assertThrows(CompletionException.class, () -> cff.join());
        assertTrue(e.getCause() instanceof CancellationException, e.getCause().toString());

        streamer.finish().toCompletableFuture().join();

        assertTrue(streamer.isDisposed(), "Streamer is not diposed");
    }

    @Test
    public void singleStep5() {
        var streamer = new StreamableIntervalRange.IntervalStreamer(1, 6);

        var cf = streamer.next().toCompletableFuture();

        streamer.dispose();

        var cff = cf;
        var e = assertThrows(CompletionException.class, () -> cff.join());
        assertTrue(e.getCause() instanceof CancellationException, e.getCause().toString());

        streamer.finish().toCompletableFuture().join();

        assertTrue(streamer.isDisposed(), "Streamer is not diposed");
    }

    @Test
    public void singleStep6() {
        var streamer = new StreamableIntervalRange.IntervalStreamer(1, 6);

        streamer.dispose();

        var cf = streamer.next().toCompletableFuture();

        var cff = cf;
        assertThrows(CancellationException.class, () -> cff.join());

        streamer.finish().toCompletableFuture().join();

        assertTrue(streamer.isDisposed(), "Streamer is not diposed");
    }

    @Test
    public void singleStep7() {
        var streamer = new StreamableIntervalRange.IntervalStreamer(1, 6);

        streamer.run();

        assertTrue(streamer.next().toCompletableFuture().join(), "next-join");

        assertEquals(1L, streamer.current());

        var cf = streamer.next().toCompletableFuture();

        streamer.run();

        assertTrue(cf.join(), "next-join");

        assertEquals(2L, streamer.current());

        streamer.finish().toCompletableFuture().join();

        assertTrue(streamer.isDisposed(), "Streamer is not diposed");
    }

    @Test
    public void underflowLong() throws Throwable {
        assertThrows(IllegalArgumentException.class, () -> {
            Streamable.intervalRange(1, -1, 20, 20, TimeUnit.MILLISECONDS, Schedulers.single());
        });
    }

    @Test
    public void underOverflowLong() throws Throwable {
        assertThrows(IllegalArgumentException.class, () -> {
            Streamable.intervalRange(2, Long.MAX_VALUE, 20, 20, TimeUnit.MILLISECONDS, Schedulers.single());
        });
    }

    @Test
    public void underNoOverflowLong() throws Throwable {
        Streamable.intervalRange(-2, Long.MAX_VALUE, 20, 20, TimeUnit.MILLISECONDS, Schedulers.single());
    }

    @Test
    public void underflowLongExec() throws Throwable {
        assertThrows(IllegalArgumentException.class, () -> {
            withVirtual(exec -> {
                Streamable.intervalRange(1, -1, 20, 20, TimeUnit.MILLISECONDS, exec);
            });
        });
    }

    @Test
    public void underOverflowLongExecutor() throws Throwable {
        assertThrows(IllegalArgumentException.class, () -> {
            withVirtual(exec -> {
                Streamable.intervalRange(2, Long.MAX_VALUE, 20, 20, TimeUnit.MILLISECONDS, exec);
            });
        });
    }

    @Test
    public void underNoOverflowLongExecutor() throws Throwable {
        withVirtual(exec -> {
            Streamable.intervalRange(-2, Long.MAX_VALUE, 20, 20, TimeUnit.MILLISECONDS, exec);
        });
    }

    @Test
    public void timerRunNextRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            var streamer = new StreamableIntervalRange.IntervalStreamer(1, 6);

            AtomicReference<CompletionStage<Boolean>> result = new AtomicReference<>();
            Runnable r2 = () -> result.lazySet(streamer.next());

            TestHelper.race(streamer, r2);

            result.get().toCompletableFuture().join();

            assertEquals(1, streamer.current());
        }
    }

    @Test
    public void neitherSchedulerNoExecutor() {
        assertThrows(IllegalArgumentException.class, () -> {
            new StreamableIntervalRange(1, 1, 1, 1, TimeUnit.SECONDS, null, null);
        });
    }
}
