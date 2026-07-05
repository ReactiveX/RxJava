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

import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.schedulers.Schedulers;

public class StreamableTimeoutTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.range(1, 5)
        .timeout(1, TimeUnit.MINUTES, Schedulers.single(), Streamable.empty())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void crash() throws Throwable {
        Streamable.error(new TestException())
        .timeout(1, TimeUnit.MINUTES, Schedulers.single(), Streamable.empty())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void timesOut() throws Throwable {
        Streamable.<Integer>never()
        .timeout(100, TimeUnit.MILLISECONDS, Schedulers.single(), Streamable.range(6, 5))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(6, 7, 8, 9, 10);
    }

    @Test
    public void timesOutError() throws Throwable {
        Streamable.<Integer>never()
        .timeout(100, TimeUnit.MILLISECONDS, Schedulers.single(), Streamable.error(new TimeoutException()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);
    }

    @Test
    public void timesOutDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.<Integer>never()
            .timeout(100, TimeUnit.MILLISECONDS, Schedulers.single(), Streamable.range(6, 5))
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(6, 7, 8, 9, 10);
        });
    }

    @Test
    public void finishFails() {
        StreamableFailingFinish.MAIN_COMPLETES
        .timeout(100, TimeUnit.MILLISECONDS, Schedulers.single(), Streamable.empty())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }
}
