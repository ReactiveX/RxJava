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
import io.reactivex.rxjava4.exceptions.TestException;

public class StreamableDoOnXTest extends StreamableBaseTest {

    @Test
    public void passthrough() {
        Streamable.range(1, 5)
        .doOnNext(_ -> { })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void normal() {
        var onValueCounter = new AtomicInteger();

        Streamable.range(1, 5)
        .doOnNext(_ -> onValueCounter.incrementAndGet())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(5, onValueCounter.get(), "onValueCounter");
    }

    @Test
    public void onCurrentThrows() {
        var onValueCounter = new AtomicInteger();

        Streamable.range(1, 5)
        .doOnNext(_ -> { onValueCounter.incrementAndGet(); throw new TestException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);

        assertEquals(1, onValueCounter.get(), "onValueCounter");
    }
}
