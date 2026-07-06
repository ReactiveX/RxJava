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

import org.junit.jupiter.api.*;

import io.reactivex.rxjava4.core.Flowable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.processors.PublishProcessor;

public class StreamableFromPublisherTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        withVirtual(exec -> {
            Flowable.range(1, 5)
            .toStreamable(exec)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5);
        });
    }

    // @RepeatedTest(1000)
    @Test
    public void normalDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Flowable.range(1, 5)
            .toStreamable(exec)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5);
        });
    }

    @Test
    public void error() {
        Flowable.error(new TestException())
        .toStreamable()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void take() throws Throwable {
        withVirtual(exec -> {
            Flowable.range(1, 5)
            .toStreamable(exec)
            .take(3)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3);
        });
    }

    @Test
    public void cancel() throws Throwable {
        withVirtual(exec -> {
            var pp = PublishProcessor.create();

            IO.println("test()");

            var ts = pp.toStreamable(exec)
            .test(exec);

            IO.println("awaitOnSubscribe()");

            ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

            IO.println("hasSubscribers()");

            while (!pp.hasSubscribers()) {
                Thread.sleep(1);
            }

            IO.println("cancel()");

            Thread.sleep(100);

            ts.cancel();

            IO.println("!hasSubscribers()");

            while (pp.hasSubscribers()) {
                Thread.sleep(1);
            }
        });
    }

    @Test
    public void cancelDebug() throws Throwable {
        withCachedExecutor(exec -> {
            var pp = PublishProcessor.create();

            IO.println("test()");

            var ts = pp.toStreamable(exec)
            .test(exec);

            IO.println("awaitOnSubscribe()");

            ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

            IO.println("hasSubscribers()");

            while (!pp.hasSubscribers()) {
                Thread.sleep(1);
            }

            IO.println("cancel()");

            Thread.sleep(100);

            ts.cancel();

            IO.println("!hasSubscribers()");

            while (pp.hasSubscribers()) {
                Thread.sleep(1);
            }
        });
    }
}
