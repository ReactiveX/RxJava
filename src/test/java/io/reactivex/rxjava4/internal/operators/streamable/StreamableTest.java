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

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.internal.subscriptions.EmptySubscription;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class StreamableTest {

    @Test
    public void empty() throws Throwable {
        TestHelper.withVirtual(exec -> {

            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            ts.onSubscribe(EmptySubscription.INSTANCE);

            var comp = Streamable.empty().forEach(e -> { ts.onError(new TestException("Element produced? " + e)); }, exec);

            comp.stage().toCompletableFuture().thenAccept(_ -> ts.onComplete()).exceptionally(e -> { ts.onError(e); return null; }).join();

            ts
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult();
        });
    }

    @Test
    public void just() throws Throwable {
        TestHelper.withVirtual(exec -> {

            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            ts.onSubscribe(EmptySubscription.INSTANCE);

            var comp = Streamable.just(1).forEach(e -> { ts.onNext(e); }, exec);

            comp.stage().toCompletableFuture().thenAccept(_ -> ts.onComplete()).exceptionally(e -> { ts.onError(e); return null; }).join();

            ts
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1);
        });
    }

    @Test
    public void fromFlowable() throws Throwable {
        TestHelper.withVirtual(_ -> {
            Flowable.range(1, 10)
            .toStreamable()
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            ;
        });
    }

    @Test
    public void fromFlowableToStreamableToFlowable() throws Throwable {
        TestHelper.withVirtual(_ -> {
            Flowable.range(1, 10)
            .toStreamable()
            .toFlowable()
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            ;
        });
    }

}
