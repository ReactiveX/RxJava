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

package io.reactivex.internal.operators.completable;

import static org.junit.Assert.assertNotEquals;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.*;

public class CompletableDelayTest {

    @Test
    public void delayCustomScheduler() {

        Completable.complete()
        .delay(100, TimeUnit.MILLISECONDS, Schedulers.trampoline())
        .test()
        .assertResult();
    }

    @Test
    public void testOnErrorCalledOnScheduler() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Thread> thread = new AtomicReference<Thread>();

        Completable.error(new Exception())
                .delay(0, TimeUnit.MILLISECONDS, Schedulers.newThread())
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        thread.set(Thread.currentThread());
                        latch.countDown();
                    }
                })
                .onErrorComplete()
                .subscribe();

        latch.await();

        assertNotEquals(Thread.currentThread(), thread.get());
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Completable.never().delay(1, TimeUnit.MINUTES));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeCompletable(new Function<Completable, CompletableSource>() {
            @Override
            public CompletableSource apply(Completable c) throws Exception {
                return c.delay(1, TimeUnit.MINUTES);
            }
        });
    }

    @Test
    public void normal() {
        Completable.complete()
        .delay(1, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void errorNotDelayed() {
        TestScheduler scheduler = new TestScheduler();

        TestObserver<Void> to = Completable.error(new TestException())
        .delay(100, TimeUnit.MILLISECONDS, scheduler, false)
        .test();

        to.assertEmpty();

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        to.assertFailure(TestException.class);

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        to.assertFailure(TestException.class);
    }

    @Test
    public void errorDelayed() {
        TestScheduler scheduler = new TestScheduler();

        TestObserver<Void> to = Completable.error(new TestException())
        .delay(100, TimeUnit.MILLISECONDS, scheduler, true)
        .test();

        to.assertEmpty();

        scheduler.advanceTimeBy(1, TimeUnit.MILLISECONDS);

        to.assertEmpty();

        scheduler.advanceTimeBy(99, TimeUnit.MILLISECONDS);

        to.assertFailure(TestException.class);
    }
}
