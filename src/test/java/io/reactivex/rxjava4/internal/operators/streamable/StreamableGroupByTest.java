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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.*;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.config.StandardConcurrentConfig;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.processors.PublishProcessor;

public class StreamableGroupByTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.range(1, 5)
        .groupBy(v -> v)
        .flatMap(v -> v, StandardConcurrentConfig.DEFAULT)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void empty() throws Throwable {
        Streamable.empty()
        .groupBy(v -> v)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void oneGroup() throws Throwable {
        Streamable.range(1, 5)
        .groupBy(_ -> 1)
        .flatMap(v -> v, StandardConcurrentConfig.DEFAULT)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void crash() throws Throwable {
        Streamable.error(new TestException())
        .groupBy(v -> v)
        .flatMap(v -> v, StandardConcurrentConfig.DEFAULT)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void oneGroupDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.range(1, 5)
            .groupBy(_ -> 1)
            .flatMap(v -> v, StandardConcurrentConfig.DEFAULT)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5);
        });
    }

    @Test
    public void crashDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.error(new TestException())
            .groupBy(v -> v)
            .flatMap(v -> v, StandardConcurrentConfig.DEFAULT)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(TestException.class);
        });
    }

    @Test
    public void mod2() throws Throwable {
        Streamable.range(1, 5)
        .groupBy(v -> v % 2)
        .flatMap(v -> v, StandardConcurrentConfig.DEFAULT)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void mapperCrash() throws Throwable {
        Streamable.error(new TestException())
        .groupBy(_ -> { throw new TestException(); })
        .flatMap(v -> v, StandardConcurrentConfig.DEFAULT)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperCrash2() throws Throwable {
        Streamable.range(1, 5)
        .groupBy(v -> {
            if (v == 1) {
                return 1;
            }
            throw new TestException();
        })
        .flatMap(v -> v, StandardConcurrentConfig.DEFAULT)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(CompositeException.class, 1);
    }

    @Test
    public void tombstone() {
        StreamableGroupBy.TombstoneGroup.INSTANCE
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(CancellationException.class);
    }

    @Test
    public void tombstone2() {
        assertSame(Streamer.NEXT_TRUE, StreamableGroupBy.TombstoneGroup.INSTANCE.next(true), "send");
        assertSame(Streamer.FINISHED, StreamableGroupBy.TombstoneGroup.INSTANCE.finish(null), "terminate");
    }

    @Test
    public void innerMultiTest() {
        var ts = Streamable.just(1)
        .groupBy(v -> v)
        .map(g -> List.of(g.test(), g.test()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS);

        var ts1 = ts.values().get(0).get(0)
        .withTag("1")
        .awaitDone(5, TimeUnit.SECONDS);

        var ts2 = ts.values().get(0).get(1)
        .withTag("2")
        .awaitDone(5, TimeUnit.SECONDS);

        if (ts1.errors().isEmpty() && !ts2.errors().isEmpty()) {
            ts1.assertResult(1);
            ts2.assertFailure(IllegalStateException.class);
        } else
        if (!ts1.errors().isEmpty() && ts2.errors().isEmpty()) {
            ts2.assertResult(1);
            ts1.assertFailure(IllegalStateException.class);
        } else {
            fail("Wrong failure state. " + ts1.toString() + " | " + ts2.toString());
        }
    }

    @Test
    public void innerMultiTestDebug() throws Throwable {
        withCachedExecutor(exec -> {
            var ts = Streamable.just(1)
            .groupBy(v -> v)
            .map(g -> List.of(g.test(exec), g.test(exec)))
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS);

            var ts1 = ts.values().get(0).get(0)
            .withTag("1")
            .awaitDone(5, TimeUnit.SECONDS);

            var ts2 = ts.values().get(0).get(1)
            .withTag("2")
            .awaitDone(5, TimeUnit.SECONDS);

            if (ts1.errors().isEmpty() && !ts2.errors().isEmpty()) {
                ts1.assertResult(1);
                ts2.assertFailure(IllegalStateException.class);
            } else
            if (!ts1.errors().isEmpty() && ts2.errors().isEmpty()) {
                ts2.assertResult(1);
                ts1.assertFailure(IllegalStateException.class);
            } else {
                fail("Wrong failure state. " + ts1.toString() + " | " + ts2.toString());
            }
        });
    }

    @Test
    public void upstreamErrorWithOneGroup() {
        Streamable.range(1, 2)
        .map(v -> {
            if (v == 2) {
                throw new TestException();
            }
            return v;
        })
        .groupBy(v -> v)
        .flatMap(v -> v, StandardConcurrentConfig.DEFAULT)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(CompositeException.class, 1)
        ;
    }

    @Test
    @Disabled("TimeoutException(\"gr.get\"), not sure why sometimes it doesn't emit that first group")
    public void groupDisposeTest() throws Throwable {
        withCachedExecutor(exec -> {
            var pp = PublishProcessor.<Integer>create();
            var gr = new AtomicReference<GroupedStreamable<Integer, Integer>>();

            var ts = pp.toStreamable(exec)
            .groupBy(v -> v)
            .map(g -> {
                gr.lazySet(g);
                return g.test(exec);
            })
            .test(exec);

            ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

            int n = 1000;
            while (!pp.hasSubscribers()) {
                Thread.sleep(1);
                if (n-- < 0) {
                    throw new TimeoutException("hasSubscribers");
                }
            }

            pp.onNext(1);

            n = 1000;
            while (gr.get() == null) {
                Thread.sleep(1);
                if (n-- < 0) {
                    throw new TimeoutException("gr.get"); // FIXME why sometimes we get here bc gr.set never runs?
                }
            }

            assertFalse(((Disposable)gr.get()).isDisposed(), "Group already disposed?");

            pp.onComplete();

            ts.awaitDone(5, TimeUnit.SECONDS)
            .assertValueCount(1);

            assertTrue(((Disposable)gr.get()).isDisposed(), "Group not disposed");
        });
    }

    @Test
    public void finishFails() {
        StreamableFailingFinish.MAIN_COMPLETES
        .groupBy(v -> v)
        .flatMap(v -> v, StandardConcurrentConfig.DEFAULT)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void finishFailsDebug() throws Throwable {
        withCachedExecutor(exec -> {
            StreamableFailingFinish.MAIN_COMPLETES
            .groupBy(v -> v)
            .flatMap(v -> v, StandardConcurrentConfig.DEFAULT)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(TestException.class);
        });
    }

    @Test
    public void finishFails2Debug() throws Throwable {
        withCachedExecutor(exec -> {
            StreamableFailingFinish.MAIN_COMPLETES
            .groupBy(v -> v)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertFailure(TestException.class);
        });
    }
}
