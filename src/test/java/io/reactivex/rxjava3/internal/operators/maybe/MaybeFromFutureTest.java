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

package io.reactivex.rxjava3.internal.operators.maybe;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MaybeFromFutureTest extends RxJavaTest {

    @Test
    public void cancelImmediately() {
        FutureTask<Integer> ft = new FutureTask<>(Functions.justCallable(1));

        Maybe.fromFuture(ft).test(true)
        .assertEmpty();
    }

    @Test
    public void timeout() {
        FutureTask<Integer> ft = new FutureTask<>(Functions.justCallable(1));

        Maybe.fromFuture(ft, 1, TimeUnit.MILLISECONDS).test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class);
    }

    @Test
    public void timedWait() {
        FutureTask<Integer> ft = new FutureTask<>(Functions.justCallable(1));
        ft.run();

        Maybe.fromFuture(ft, 1, TimeUnit.MILLISECONDS).test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void interrupt() {
        FutureTask<Integer> ft = new FutureTask<>(Functions.justCallable(1));

        Thread.currentThread().interrupt();

        Maybe.fromFuture(ft, 1, TimeUnit.MILLISECONDS).test()
        .assertFailure(InterruptedException.class);
    }

    @Test
    public void cancelWhileRunning() {
        final TestObserver<Object> to = new TestObserver<>();

        FutureTask<Object> ft = new FutureTask<>(new Runnable() {
            @Override
            public void run() {
                to.dispose();
            }
        }, null);

        Schedulers.single().scheduleDirect(ft, 100, TimeUnit.MILLISECONDS);

        Maybe.fromFuture(ft)
        .subscribeWith(to)
        .assertEmpty();

        assertTrue(to.isDisposed());
    }

    @Test
    public void cancelAndCrashWhileRunning() {
        final TestObserver<Object> to = new TestObserver<>();

        FutureTask<Object> ft = new FutureTask<>(new Runnable() {
            @Override
            public void run() {
                to.dispose();
                throw new TestException();
            }
        }, null);

        Schedulers.single().scheduleDirect(ft, 100, TimeUnit.MILLISECONDS);

        Maybe.fromFuture(ft)
        .subscribeWith(to)
        .assertEmpty();

        assertTrue(to.isDisposed());
    }

    @Test
    public void futureNull() {
        FutureTask<Object> ft = new FutureTask<>(new Runnable() {
            @Override
            public void run() {
            }
        }, null);

        Schedulers.single().scheduleDirect(ft, 100, TimeUnit.MILLISECONDS);

        Maybe.fromFuture(ft)
        .test()
        .assertResult();
    }
}
