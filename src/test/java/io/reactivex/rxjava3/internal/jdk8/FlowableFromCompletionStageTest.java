/**
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

package io.reactivex.rxjava3.internal.jdk8;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class FlowableFromCompletionStageTest extends RxJavaTest {

    @Test
    public void syncSuccess() {
        Flowable.fromCompletionStage(CompletableFuture.completedFuture(1))
        .test()
        .assertResult(1);
    }

    @Test
    public void syncFailure() {
        CompletableFuture<Integer> cf = new CompletableFuture<>();
        cf.completeExceptionally(new TestException());

        Flowable.fromCompletionStage(cf)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void syncNull() {
        Flowable.fromCompletionStage(CompletableFuture.<Integer>completedFuture(null))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void cancel() {
        CompletableFuture<Integer> cf = new CompletableFuture<>();

        TestSubscriber<Integer> ts = Flowable.fromCompletionStage(cf)
        .test();

        ts.assertEmpty();

        ts.cancel();

        cf.complete(1);

        ts.assertEmpty();
    }
}
