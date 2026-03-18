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

import static org.testng.Assert.assertTrue;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.rxjava4.core.Flowable;

public class FlowableVirtualTransformExecutorTest {

    @Test
    public void checkIsInsideVirtualThread() {
        try (var scope = Executors.newVirtualThreadPerTaskExecutor()) {
            var cancelled = new AtomicBoolean();
            Flowable.range(1, 5)
            .doOnCancel(() -> cancelled.set(true))
            .virtualTransform((v, emitter) -> emitter.emit(v), scope)
            .take(1)
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1);

            assertTrue(cancelled.get());
        }
    }
}
