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

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.processors.DispatchStreamProcessor;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class DispatchStreamProcessorTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        var dsp = new DispatchStreamProcessor<Integer>();

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertFalse(dsp.hasComplete(), "dsp has completed?");
        assertFalse(dsp.hasThrowable(), "dsp has throwable?");
        assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");

        var ts = dsp.test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000);

        for (int i = 1; i < 6; i++) {
            dsp.next(i).toCompletableFuture().join();
        }
        dsp.finish(null).toCompletableFuture().join();

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertTrue(dsp.hasComplete(), "dsp has completed?");
        assertFalse(dsp.hasThrowable(), "dsp has throwable?");
        assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");
    }

    @Test
    public void endsInError() throws Throwable {
        var dsp = new DispatchStreamProcessor<Integer>();

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertFalse(dsp.hasComplete(), "dsp has completed?");
        assertFalse(dsp.hasThrowable(), "dsp has throwable?");
        assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");

        var ts = dsp.test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000);

        for (int i = 1; i < 6; i++) {
            dsp.next(i).toCompletableFuture().join();
        }
        var te = new TestException();
        dsp.finish(te).toCompletableFuture().join();

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class, 1, 2, 3, 4, 5);

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertFalse(dsp.hasComplete(), "dsp has completed?");
        assertTrue(dsp.hasThrowable(), "dsp has no throwable?");
        assertSame(te, dsp.getThrowable(), "dsp has the wrong throwable?");
    }

    @Test
    public void normalDebug() throws Throwable {
        withCachedExecutor(exec -> {
            var dsp = new DispatchStreamProcessor<Integer>();

            assertFalse(dsp.hasStreamers(), "dsp has streamers?");
            assertFalse(dsp.hasComplete(), "dsp has completed?");
            assertFalse(dsp.hasThrowable(), "dsp has throwable?");
            assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");

            var ts = dsp.intercept(debugIntercept()).test(exec);

            ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

            awaitStreamers(dsp, 1000);

            assertTrue(dsp.hasStreamers(), "dsp has no streamers?");

            for (int i = 1; i < 6; i++) {
                dsp.next(i).toCompletableFuture().join();
            }
            dsp.finish(null).toCompletableFuture().join();

            ts.awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5);

            assertFalse(dsp.hasStreamers(), "dsp has streamers?");
            assertTrue(dsp.hasComplete(), "dsp has completed?");
            assertFalse(dsp.hasThrowable(), "dsp has throwable?");
            assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");
        });
    }

    @Test
    public void alreadyCompleted() throws Throwable {
        var dsp = new DispatchStreamProcessor<Integer>();

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertFalse(dsp.hasComplete(), "dsp has completed?");
        assertFalse(dsp.hasThrowable(), "dsp has throwable?");
        assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");

        dsp.finish(null);

        dsp.test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertTrue(dsp.hasComplete(), "dsp has not completed?");
        assertFalse(dsp.hasThrowable(), "dsp has throwable?");
        assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");
    }

    @Test
    public void alreadyFailed() throws Throwable {
        var dsp = new DispatchStreamProcessor<Integer>();

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertFalse(dsp.hasComplete(), "dsp has completed?");
        assertFalse(dsp.hasThrowable(), "dsp has throwable?");
        assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");

        var te = new TestException();
        dsp.finish(te);

        dsp.test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertFalse(dsp.hasComplete(), "dsp has completed?");
        assertTrue(dsp.hasThrowable(), "dsp has no throwable?");
        assertSame(te, dsp.getThrowable(), "dsp has a different throwable?");
    }

    @Test
    public void normalTake3AltDebug() throws Throwable {
        withCachedExecutor(exec -> {
            var dsp = new DispatchStreamProcessor<Integer>();

            assertFalse(dsp.hasStreamers(), "dsp has streamers?");
            assertFalse(dsp.hasComplete(), "dsp has completed?");
            assertFalse(dsp.hasThrowable(), "dsp has throwable?");
            assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");

            var ts = dsp.take(3).intercept(debugIntercept()).test(exec);

            ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

            ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

            awaitStreamers(dsp, 1000);

            for (int i = 1; i < 4; i++) {
                IO.println(i + " -> next");
                dsp.next(i).toCompletableFuture().join();
            }

            awaitNoStreamers(dsp, 1000);

            assertFalse(dsp.hasStreamers(), "dsp has streamers?");

            for (int i = 4; i < 6; i++) {
                IO.println(i + " -> next");
                dsp.next(i).toCompletableFuture().join();
            }

            IO.println("() -> finish");
            dsp.finish(null).toCompletableFuture().join();

            IO.println("awaitDone");
            ts.awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3);

            assertFalse(dsp.hasStreamers(), "dsp has streamers?");
            assertTrue(dsp.hasComplete(), "dsp has completed?");
            assertFalse(dsp.hasThrowable(), "dsp has throwable?");
            assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");
        });
    }

    @Test
    public void normalTake3Debug() throws Throwable {
        withCachedExecutor(exec -> {
            var dsp = new DispatchStreamProcessor<Integer>();

            assertFalse(dsp.hasStreamers(), "dsp has streamers?");
            assertFalse(dsp.hasComplete(), "dsp has completed?");
            assertFalse(dsp.hasThrowable(), "dsp has throwable?");
            assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");

            var ts = dsp.take(3).test(exec);

            ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

            awaitStreamers(dsp, 1000);

            for (int i = 1; i < 6; i++) {
                dsp.next(i).toCompletableFuture().join();
            }

            IO.println("() -> finish");
            dsp.finish(null).toCompletableFuture().join();

            IO.println("awaitDone");
            ts.awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3);

            assertFalse(dsp.hasStreamers(), "dsp has streamers?");
            assertTrue(dsp.hasComplete(), "dsp has completed?");
            assertFalse(dsp.hasThrowable(), "dsp has throwable?");
            assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");
        });
    }

    @Test
    public void normalMulti() throws Throwable {
        var dsp = new DispatchStreamProcessor<Integer>();

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertFalse(dsp.hasComplete(), "dsp has completed?");
        assertFalse(dsp.hasThrowable(), "dsp has throwable?");
        assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");

        var ts = dsp.test().withTag("ts");
        var ts2 = dsp.test().withTag("ts2");

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);
        ts2.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000, 2);

        for (int i = 1; i < 6; i++) {
            dsp.next(i).toCompletableFuture().join();
        }
        dsp.finish(null).toCompletableFuture().join();

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);

        ts2.awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertTrue(dsp.hasComplete(), "dsp has completed?");
        assertFalse(dsp.hasThrowable(), "dsp has throwable?");
        assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");
    }

    @Test
    public void normalMultiOtherCancels() throws Throwable {
        var dsp = new DispatchStreamProcessor<Integer>();

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertFalse(dsp.hasComplete(), "dsp has completed?");
        assertFalse(dsp.hasThrowable(), "dsp has throwable?");
        assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");

        var ts = dsp.test();
        var ts2 = dsp.test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);
        ts2.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000, 2);

        ts2.cancel();

        for (int i = 1; i < 6; i++) {
            dsp.next(i).toCompletableFuture().join();
        }
        dsp.finish(null).toCompletableFuture().join();

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);

        ts2.assertEmpty();

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertTrue(dsp.hasComplete(), "dsp has completed?");
        assertFalse(dsp.hasThrowable(), "dsp has throwable?");
        assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");
    }

    @Test
    public void normalMultiFirstCancels() throws Throwable {
        var dsp = new DispatchStreamProcessor<Integer>();

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertFalse(dsp.hasComplete(), "dsp has completed?");
        assertFalse(dsp.hasThrowable(), "dsp has throwable?");
        assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");

        var ts = dsp.test();
        var ts2 = dsp.test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);
        ts2.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000, 2);

        ts.cancel();

        for (int i = 1; i < 6; i++) {
            dsp.next(i).toCompletableFuture().join();
        }
        dsp.finish(null).toCompletableFuture().join();

        ts2.awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);

        ts.assertEmpty();

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertTrue(dsp.hasComplete(), "dsp has completed?");
        assertFalse(dsp.hasThrowable(), "dsp has throwable?");
        assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");
    }

    @Test
    public void raceToStream() throws Throwable {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            var dsp = new DispatchStreamProcessor<Integer>();

            assertFalse(dsp.hasStreamers(), "dsp has streamers?");
            assertFalse(dsp.hasComplete(), "dsp has completed?");
            assertFalse(dsp.hasThrowable(), "dsp has throwable?");
            assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");

            var ts = new TestSubscriber<>();
            var ts2 = new TestSubscriber<>();

            Runnable r1 = () -> dsp.subscribe(ts);
            Runnable r2 = () -> dsp.subscribe(ts2);

            TestHelper.race(r1, r2);

            ts.awaitOnSubscribe(1, TimeUnit.SECONDS);
            ts2.awaitOnSubscribe(1, TimeUnit.SECONDS);

            awaitStreamers(dsp, 1000);

            ts.cancel();
            ts2.cancel();

            awaitNoStreamers(dsp, 1000);

            assertFalse(dsp.hasStreamers(), "dsp has streamers?");
            assertFalse(dsp.hasComplete(), "dsp has completed?");
            assertFalse(dsp.hasThrowable(), "dsp has throwable?");
            assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");
        }
    }

    @Test
    public void comeAndGo() throws Throwable {
        var dsp = new DispatchStreamProcessor<Integer>();

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertFalse(dsp.hasComplete(), "dsp has completed?");
        assertFalse(dsp.hasThrowable(), "dsp has throwable?");
        assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");

        var ts = dsp.test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000);

        ts.cancel();

        awaitNoStreamers(dsp, 1000);

        assertFalse(dsp.hasStreamers(), "dsp has streamers?");
        assertFalse(dsp.hasComplete(), "dsp has completed?");
        assertFalse(dsp.hasThrowable(), "dsp has throwable?");
        assertNull(dsp.getThrowable(), "dsp has a non-null throwable?");
    }

    @Test
    public void isDisposed() {
        var dsp = new DispatchStreamProcessor<Integer>();

        var str = dsp.stream(new CompositeDisposable());

        assertTrue(str instanceof Disposable, "Does not implement disposable?");

        var d = (Disposable)str;

        assertFalse(d.isDisposed());
        assertTrue(dsp.hasStreamers());

        d.dispose();

        assertTrue(d.isDisposed());
        assertFalse(dsp.hasStreamers());

        d.dispose();

        assertTrue(d.isDisposed());
        assertFalse(dsp.hasStreamers());
    }
}
