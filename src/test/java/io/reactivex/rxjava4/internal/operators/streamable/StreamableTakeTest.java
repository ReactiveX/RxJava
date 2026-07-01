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
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.Flowable;

@Isolated
public class StreamableTakeTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        var isCancelled = new AtomicBoolean();

        Flowable.range(1, 10)
        .doOnCancel(() -> isCancelled.set(true))
        .toStreamable()
        .take(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);

        assertTrue(isCancelled.get(), "Cancel was not propagated");
    }

    @Test
    public void fewer() throws Throwable {
        var isCancelled = new AtomicBoolean();

        Flowable.range(1, 4)
        .doOnCancel(() -> isCancelled.set(true))
        .toStreamable()
        .take(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4);

        assertFalse(isCancelled.get(), "Cancel was propagated!");
    }
}
