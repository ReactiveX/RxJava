/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.single;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.functions.Consumer;
import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.BiConsumer;
import io.reactivex.schedulers.Schedulers;

public class SingleDelayTest {
    @Test
    public void delay() throws Exception {
        final AtomicInteger value = new AtomicInteger();

        Single.just(1).delay(200, TimeUnit.MILLISECONDS)
        .subscribe(new BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(Integer v, Throwable e) throws Exception {
                value.set(v);
            }
        });

        Thread.sleep(100);

        assertEquals(0, value.get());

        Thread.sleep(200);

        assertEquals(1, value.get());
    }

    @Test
    public void delayError() {
        Single.error(new TestException()).delay(5, TimeUnit.SECONDS)
        .test()
        .awaitDone(1, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void delaySubscriptionCompletable() throws Exception {
        Single.just(1).delaySubscription(Completable.complete().delay(100, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delaySubscriptionObservable() throws Exception {
        Single.just(1).delaySubscription(Observable.timer(100, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delaySubscriptionFlowable() throws Exception {
        Single.just(1).delaySubscription(Flowable.timer(100, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delaySubscriptionSingle() throws Exception {
        Single.just(1).delaySubscription(Single.timer(100, TimeUnit.MILLISECONDS))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delaySubscriptionTime() throws Exception {
        Single.just(1).delaySubscription(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void delaySubscriptionTimeCustomScheduler() throws Exception {
        Single.just(1).delaySubscription(100, TimeUnit.MILLISECONDS, Schedulers.io())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void testOnErrorCalledOnScheduler() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Thread> thread = new AtomicReference<Thread>();

        Single.<String>error(new Exception())
                .delay(0, TimeUnit.MILLISECONDS, Schedulers.newThread())
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        thread.set(Thread.currentThread());
                        latch.countDown();
                    }
                })
                .onErrorResumeNext(Single.just(""))
                .subscribe();

        latch.await();

        assertNotEquals(Thread.currentThread(), thread.get());
    }

}
