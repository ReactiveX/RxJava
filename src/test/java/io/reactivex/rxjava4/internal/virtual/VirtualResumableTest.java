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

package io.reactivex.rxjava4.internal.virtual;

import static org.testng.Assert.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.*;

public class VirtualResumableTest {

    @Test
    public void parkNormalThread() {
        var ready1 = new VirtualResumable();
        var ready2 = new VirtualResumable();

        var t = new Thread(() -> {
            ready1.await();
            ready2.resume();
        });
        t.start();

        ready1.resume();
        ready2.await();
    }

    @Test
    public void concurrentAwait() {
        var ready1 = new VirtualResumable();

        var stateEx = 0;

        try {
            var t = new Thread(ready1::await);
            t.start();

            while (ready1.get() == null) { }
            try {
                ready1.await();
            } catch (IllegalStateException ex) {
                stateEx++;
            }
        } finally {
            ready1.resume();
        }

        assertEquals(1, stateEx++);
    }

    @Test @Timeout(value = 30000, unit = TimeUnit.MILLISECONDS)
    public void pingPong() throws Exception {
        try (var scope = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) {

            var producerReady = new VirtualResumable();
            var consumerReady = new VirtualResumable();

            var n = 100_000;

            var exchange = new AtomicReference<Integer>();

            var job1 = scope.submit(() -> {
                assertTrue(Thread.currentThread().isVirtual());
                for (int i = 1; i <= n; i++) {

                    consumerReady.await();

                    assertNull(exchange.get());

                    exchange.lazySet(i);

                    producerReady.resume();
                }
                return null;
            });

            var job2 = scope.submit(() -> {
                assertTrue(Thread.currentThread().isVirtual());
                var value = -1;
                while (value != n) {
                    consumerReady.resume();

                    producerReady.await();

                    value = exchange.get();
                    exchange.lazySet(null);
                }
                return value;
            });

            job1.get();

            assertEquals(n, job2.get().intValue());
        }
    }
}
