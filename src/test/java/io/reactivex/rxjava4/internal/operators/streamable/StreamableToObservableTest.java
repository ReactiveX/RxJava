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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.core.config.StreamableInterceptConfig;
import io.reactivex.rxjava4.disposables.CompositeDisposable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;
import io.reactivex.rxjava4.internal.operators.streamable.StreamableToObservable.StreamToObserver;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.processors.PublishProcessor;

public class StreamableToObservableTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        var onFinishCounter = new AtomicInteger();

        Streamable.range(1, 5)
        .intercept(new StreamableInterceptConfig<>(
                (_, v) -> v, (_, v) -> v, v -> v, (_, v) -> { onFinishCounter.incrementAndGet(); return v; }))
        .toObservable()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, onFinishCounter.get(), "onFinishCounter");
    }

    @Test
    public void crash() throws Throwable {
        AtomicInteger onFinishCounter = new AtomicInteger();

        Streamable.error(new TestException())
        .intercept(new StreamableInterceptConfig<>(
                (_, v) -> v, (_, v) -> v, v -> v, (_, v) -> { onFinishCounter.incrementAndGet(); return v; }))
        .toObservable()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);

        assertEquals(1, onFinishCounter.get(), "onFinishCounter");
    }

    @Test
    public void take() throws Throwable {
        var onFinishCounter = new AtomicInteger();

        Streamable.range(1, 5)
        .intercept(new StreamableInterceptConfig<>(
                (_, v) -> v, (_, v) -> v, v -> v, (_, v) -> { onFinishCounter.incrementAndGet(); return v; }))
        .toObservable()
        .take(3)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);

        assertEquals(1, onFinishCounter.get(), "onFinishCounter");
    }

    @Test
    public void subject() throws Throwable {
        var pp = PublishProcessor.create();
        var to = pp.toStreamable()
        .toObservable()
        .test();

        while (!pp.hasSubscribers()) {
            Thread.sleep(1);
        }

        to.awaitOnSubscribe(1, TimeUnit.SECONDS);

        to.dispose();

        while (pp.hasSubscribers()) {
            Thread.sleep(1);
        }
    }

    @Test
    public void upstream() {
        var s = Streamable.range(1, 5);

        var o = s.toObservable();

        if (o instanceof HasUpstreamStreamableSource<?> uso) {
            assertSame(s, uso.source());
        } else {
            fail(o.getClass() + " doesn't implement HasUpstreamStreamable or it is hidden.");
        }
    }

    @Test
    public void disposable() {
        var observer = new TestObserver<>();
        var cs = new CompositeDisposable();
        observer.onSubscribe(cs);
        var sto = new StreamToObserver<>(Streamable.never().stream(cs), observer, cs,
                new AtomicInteger(), new AtomicBoolean(), new AtomicReference<>(), new AtomicBoolean());
        cs.add(sto);

        assertFalse(sto.isDisposed(), "sto disposed?");

        observer.dispose();

        assertTrue(cs.isDisposed(), "cs not disposed?");
        assertTrue(sto.isDisposed(), "sto not disposed?");
    }
}
