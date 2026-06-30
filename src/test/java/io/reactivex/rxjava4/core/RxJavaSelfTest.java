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

package io.reactivex.rxjava4.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.exceptions.TestException;

public final class RxJavaSelfTest extends RxJavaTest {

    @Test
    public void withRetrySuccess() {
        var counter = new AtomicInteger();
        withRetry(3, () -> {
            counter.incrementAndGet();
        });

        assertEquals(1, counter.get());
    }

    @Test
    public void withRetryFailOnce() {
        var counter = new AtomicInteger();
        withRetry(3, () -> {
            if (counter.getAndIncrement() == 0) {
                throw new TestException("Failed index " + counter.get());
            }
        });

        assertEquals(2, counter.get());
    }

    @Test
    public void withRetryFailTwice() {
        var counter = new AtomicInteger();
        withRetry(3, () -> {
            if (counter.getAndIncrement() < 2) {
                throw new TestException("Failed index " + counter.get());
            }
        });

        assertEquals(3, counter.get());
    }

    @Test
    public void withRetryTruce() {
        var counter = new AtomicInteger();

        assertThrows(AssertionError.class, () -> {
            withRetry(3, () -> {
                counter.getAndIncrement();
                throw new TestException("Failed index " + counter.get());
            });
        });

        assertEquals(3, counter.get());
    }
}
