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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.core.config.StreamableInterceptConfig;
import io.reactivex.rxjava4.exceptions.TestException;

public class StreamableInterceptTest extends StreamableBaseTest {

    @Test
    public void passthrough() {
        Streamable.range(1, 5)
        .intercept(new StreamableInterceptConfig<>((_, v) -> v))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normal() {
        var onStreamCounter = new AtomicInteger();
        var onNextCounter = new AtomicInteger();
        var onValueCounter = new AtomicInteger();
        var onFinishCounter = new AtomicInteger();

        Streamable.range(1, 5)
        .intercept(new StreamableInterceptConfig<>(
                (_, s) -> { onStreamCounter.incrementAndGet(); return s; },
                (_, b) -> { onNextCounter.incrementAndGet(); return b; },
                v -> { onValueCounter.incrementAndGet(); return v * 10; },
                (_, f) -> { onFinishCounter.incrementAndGet(); return f; }
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(10, 20, 30, 40, 50);

        assertEquals(1, onStreamCounter.get(), "onStreamCounter");
        assertEquals(6, onNextCounter.get(), "onNextCounter");
        assertEquals(5, onValueCounter.get(), "onValueCounter");
        assertEquals(1, onFinishCounter.get(), "onFinishCounter");
    }

    @Test
    public void onStreamNull() {
        var onStreamCounter = new AtomicInteger();
        var onNextCounter = new AtomicInteger();
        var onValueCounter = new AtomicInteger();
        var onFinishCounter = new AtomicInteger();

        Streamable.range(1, 5)
        .intercept(new StreamableInterceptConfig<>(
                (_, _) -> { onStreamCounter.incrementAndGet(); return null; },
                (_, b) -> { onNextCounter.incrementAndGet(); return b; },
                v -> { onValueCounter.incrementAndGet(); return v * 10; },
                (_, f) -> { onFinishCounter.incrementAndGet(); return f; }
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);

        assertEquals(1, onStreamCounter.get(), "onStreamCounter");
        assertEquals(0, onNextCounter.get(), "onNextCounter");
        assertEquals(0, onValueCounter.get(), "onValueCounter");
        assertEquals(0, onFinishCounter.get(), "onFinishCounter");
    }

    @Test
    public void onStreamCrash() {
        var onStreamCounter = new AtomicInteger();
        var onNextCounter = new AtomicInteger();
        var onValueCounter = new AtomicInteger();
        var onFinishCounter = new AtomicInteger();

        Streamable.range(1, 5)
        .intercept(new StreamableInterceptConfig<>(
                (_, _) -> { onStreamCounter.incrementAndGet(); throw new TestException(); },
                (_, b) -> { onNextCounter.incrementAndGet(); return b; },
                v -> { onValueCounter.incrementAndGet(); return v * 10; },
                (_, f) -> { onFinishCounter.incrementAndGet(); return f; }
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);

        assertEquals(1, onStreamCounter.get(), "onStreamCounter");
        assertEquals(0, onNextCounter.get(), "onNextCounter");
        assertEquals(0, onValueCounter.get(), "onValueCounter");
        assertEquals(0, onFinishCounter.get(), "onFinishCounter");
    }

    @Test
    public void onNextNull() {
        var onStreamCounter = new AtomicInteger();
        var onNextCounter = new AtomicInteger();
        var onValueCounter = new AtomicInteger();
        var onFinishCounter = new AtomicInteger();

        Streamable.range(1, 5)
        .intercept(new StreamableInterceptConfig<>(
                (_, s) -> { onStreamCounter.incrementAndGet(); return s; },
                (_, _) -> { onNextCounter.incrementAndGet(); return null; },
                v -> { onValueCounter.incrementAndGet(); return v * 10; },
                (_, f) -> { onFinishCounter.incrementAndGet(); return f; }
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);

        assertEquals(1, onStreamCounter.get(), "onStreamCounter");
        assertEquals(1, onNextCounter.get(), "onNextCounter");
        assertEquals(0, onValueCounter.get(), "onValueCounter");
        assertEquals(1, onFinishCounter.get(), "onFinishCounter");
    }

    @Test
    public void onNextCrash() {
        var onStreamCounter = new AtomicInteger();
        var onNextCounter = new AtomicInteger();
        var onValueCounter = new AtomicInteger();
        var onFinishCounter = new AtomicInteger();

        Streamable.range(1, 5)
        .intercept(new StreamableInterceptConfig<>(
                (_, s) -> { onStreamCounter.incrementAndGet(); return s; },
                (_, _) -> { onNextCounter.incrementAndGet(); throw new TestException(); },
                v -> { onValueCounter.incrementAndGet(); return v * 10; },
                (_, f) -> { onFinishCounter.incrementAndGet(); return f; }
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);

        assertEquals(1, onStreamCounter.get(), "onStreamCounter");
        assertEquals(1, onNextCounter.get(), "onNextCounter");
        assertEquals(0, onValueCounter.get(), "onValueCounter");
        assertEquals(1, onFinishCounter.get(), "onFinishCounter");
    }

    @Test
    public void onCurrentNull() {
        var onStreamCounter = new AtomicInteger();
        var onNextCounter = new AtomicInteger();
        var onValueCounter = new AtomicInteger();
        var onFinishCounter = new AtomicInteger();

        Streamable.range(1, 5)
        .intercept(new StreamableInterceptConfig<>(
                (_, s) -> { onStreamCounter.incrementAndGet(); return s; },
                (_, b) -> { onNextCounter.incrementAndGet(); return b; },
                _ -> { onValueCounter.incrementAndGet(); return null; },
                (_, f) -> { onFinishCounter.incrementAndGet(); return f; }
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);

        assertEquals(1, onStreamCounter.get(), "onStreamCounter");
        assertEquals(1, onNextCounter.get(), "onNextCounter");
        assertEquals(1, onValueCounter.get(), "onValueCounter");
        assertEquals(1, onFinishCounter.get(), "onFinishCounter");
    }

    @Test
    public void onCurrentThrows() {
        var onStreamCounter = new AtomicInteger();
        var onNextCounter = new AtomicInteger();
        var onValueCounter = new AtomicInteger();
        var onFinishCounter = new AtomicInteger();

        Streamable.range(1, 5)
        .intercept(new StreamableInterceptConfig<>(
                (_, s) -> { onStreamCounter.incrementAndGet(); return s; },
                (_, b) -> { onNextCounter.incrementAndGet(); return b; },
                _ -> { onValueCounter.incrementAndGet(); throw new TestException(); },
                (_, f) -> { onFinishCounter.incrementAndGet(); return f; }
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);

        assertEquals(1, onStreamCounter.get(), "onStreamCounter");
        assertEquals(1, onNextCounter.get(), "onNextCounter");
        assertEquals(1, onValueCounter.get(), "onValueCounter");
        assertEquals(1, onFinishCounter.get(), "onFinishCounter");
    }

    @Test
    public void onFinishNull() {
        var onStreamCounter = new AtomicInteger();
        var onNextCounter = new AtomicInteger();
        var onValueCounter = new AtomicInteger();
        var onFinishCounter = new AtomicInteger();

        Streamable.range(1, 5)
        .intercept(new StreamableInterceptConfig<>(
                (_, s) -> { onStreamCounter.incrementAndGet(); return s; },
                (_, b) -> { onNextCounter.incrementAndGet(); return b; },
                v -> { onValueCounter.incrementAndGet(); return v * 10; },
                (_, _) -> { onFinishCounter.incrementAndGet(); return null; }
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class, 10, 20, 30, 40, 50);

        assertEquals(1, onStreamCounter.get(), "onStreamCounter");
        assertEquals(6, onNextCounter.get(), "onNextCounter");
        assertEquals(5, onValueCounter.get(), "onValueCounter");
        assertEquals(1, onFinishCounter.get(), "onFinishCounter");
    }

    @Test
    public void onFinishCrash() {
        var onStreamCounter = new AtomicInteger();
        var onNextCounter = new AtomicInteger();
        var onValueCounter = new AtomicInteger();
        var onFinishCounter = new AtomicInteger();

        Streamable.range(1, 5)
        .intercept(new StreamableInterceptConfig<>(
                (_, s) -> { onStreamCounter.incrementAndGet(); return s; },
                (_, b) -> { onNextCounter.incrementAndGet(); return b; },
                v -> { onValueCounter.incrementAndGet(); return v * 10; },
                (_, _) -> { onFinishCounter.incrementAndGet(); throw new TestException(); }
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class, 10, 20, 30, 40, 50);

        assertEquals(1, onStreamCounter.get(), "onStreamCounter");
        assertEquals(6, onNextCounter.get(), "onNextCounter");
        assertEquals(5, onValueCounter.get(), "onValueCounter");
        assertEquals(1, onFinishCounter.get(), "onFinishCounter");
    }
}
