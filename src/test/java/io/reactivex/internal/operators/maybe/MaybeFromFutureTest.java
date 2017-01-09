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

package io.reactivex.internal.operators.maybe;

import java.util.concurrent.*;

import org.junit.Test;

import io.reactivex.Maybe;
import io.reactivex.internal.functions.Functions;

public class MaybeFromFutureTest {

    @Test
    public void cancelImmediately() {
        FutureTask<Integer> ft = new FutureTask<Integer>(Functions.justCallable(1));

        Maybe.fromFuture(ft).test(true)
        .assertEmpty();
    }

    @Test
    public void timeout() {
        FutureTask<Integer> ft = new FutureTask<Integer>(Functions.justCallable(1));

        Maybe.fromFuture(ft, 1, TimeUnit.MILLISECONDS).test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);
    }

    @Test
    public void timedWait() {
        FutureTask<Integer> ft = new FutureTask<Integer>(Functions.justCallable(1));
        ft.run();

        Maybe.fromFuture(ft, 1, TimeUnit.MILLISECONDS).test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void interrupt() {
        FutureTask<Integer> ft = new FutureTask<Integer>(Functions.justCallable(1));

        Thread.currentThread().interrupt();

        Maybe.fromFuture(ft, 1, TimeUnit.MILLISECONDS).test()
        .assertFailure(InterruptedException.class);
    }
}
