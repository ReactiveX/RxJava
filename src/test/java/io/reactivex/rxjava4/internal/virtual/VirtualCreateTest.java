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

/*
 * Copyright 2019-Present David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.rxjava4.internal.virtual;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.core.config.StandardBufferedConfig;
import io.reactivex.rxjava4.internal.operators.streamable.StreamableBaseTest;
import io.reactivex.rxjava4.schedulers.Schedulers;

public class VirtualCreateTest extends StreamableBaseTest {

    @Test
    public void checkIsInsideVirtualThread() {
        try (var scope = Executors.newVirtualThreadPerTaskExecutor()) {
            Flowable.virtualCreate(emitter -> emitter.emit(Thread.currentThread().isVirtual()), scope)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(true);
        }
    }

    @Test
    public void checkIsInsideVirtualThreadExec() throws Throwable {
        try (var exec = Executors.newSingleThreadExecutor()) {
            Flowable.virtualCreate(emitter -> emitter.emit(Thread.currentThread().isVirtual()), exec)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(false);

            exec.shutdown();
        }
    }

    @Test
    public void plainVirtual() {
        var result = new AtomicReference<Boolean>();
        try (var scope = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) {
            scope.submit(() -> result.set(Thread.currentThread().isVirtual()));
        }

        assertTrue(result.get());
    }

    @Test
    public void takeUntil() throws Throwable {
        withVirtual(exec -> Flowable.<Integer>virtualCreate(e -> {
            for (int i = 1; i < 6; i++) {
                e.emit(i);
            }
        }, exec)
        .take(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2));
    }

    @Test
    public void backpressure() throws Throwable {
        withVirtual(exec -> Flowable.<Integer>virtualCreate(e -> {
            for (int i = 0; i < 10000; i++) {
                e.emit(i);
            }
        }, exec)
        .observeOn(Schedulers.single(), new StandardBufferedConfig(false, 2))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(10000));
    }

    @Test
    public void error() throws Throwable {
        withVirtual(exec -> Flowable.<Integer>virtualCreate(_ -> {
            throw new IOException();
        }, exec)
        .observeOn(Schedulers.single(), new StandardBufferedConfig(false, 2))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertError(IOException.class));
    }
}
