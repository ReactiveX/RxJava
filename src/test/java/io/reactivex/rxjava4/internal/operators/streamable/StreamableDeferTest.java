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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;

@Isolated
public class StreamableDeferTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        var counter = new AtomicInteger();

        var source = Streamable.defer(() -> {

            return Streamable.just(counter.getAndIncrement());
        });

        for (int i = 0; i < 5; i++) {
            source.test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(i);
        }
    }

    @Test
    public void crash() throws Throwable {
        Streamable.defer(() -> { throw new TestException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }
}
