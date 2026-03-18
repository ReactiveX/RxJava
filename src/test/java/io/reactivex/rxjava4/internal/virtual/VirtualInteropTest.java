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

import static org.testng.Assert.assertTrue;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.functions.Consumer;

public class VirtualInteropTest {

    @Test
    public void checkIsInsideVirtualThread() {
        try (var scope = Executors.newVirtualThreadPerTaskExecutor()) {
            Flowable.virtualCreate(emitter -> {
                emitter.emit(Thread.currentThread().isVirtual());
            }, scope)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(true);
        }
    }

    @Test
    public void checkIsInsideVirtualThreadExec() throws Throwable {
        try (var exec = Executors.newSingleThreadExecutor()) {
            Flowable.virtualCreate(emitter -> {
                emitter.emit(Thread.currentThread().isVirtual());
            }, exec)
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

    static void withVirtual(Consumer<ExecutorService> call) throws Throwable {
        try (var exec = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) {
            call.accept(exec);
        }
    }
}
