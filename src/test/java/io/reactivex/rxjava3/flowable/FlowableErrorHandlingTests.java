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

package io.reactivex.rxjava3.flowable;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.DefaultSubscriber;

public class FlowableErrorHandlingTests extends RxJavaTest {

    /**
     * Test that an error from a user provided Observer.onNext
     * is handled and emitted to the onError.
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void onNextError() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> caughtError = new AtomicReference<>();
        Flowable<Long> f = Flowable.interval(50, TimeUnit.MILLISECONDS);
        Subscriber<Long> subscriber = new DefaultSubscriber<Long>() {

            @Override
            public void onComplete() {
                System.out.println("completed");
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("error: " + e);
                caughtError.set(e);
                latch.countDown();
            }

            @Override
            public void onNext(Long args) {
                throw new RuntimeException("forced failure");
            }
        };
        f.safeSubscribe(subscriber);

        latch.await(2000, TimeUnit.MILLISECONDS);
        assertNotNull(caughtError.get());
    }

    /**
     * Test that an error from a user provided Observer.onNext
     * is handled and emitted to the onError.
     * even when done across thread boundaries with observeOn
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void onNextErrorAcrossThread() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> caughtError = new AtomicReference<>();
        Flowable<Long> f = Flowable.interval(50, TimeUnit.MILLISECONDS);
        Subscriber<Long> subscriber = new DefaultSubscriber<Long>() {

            @Override
            public void onComplete() {
                System.out.println("completed");
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("error: " + e);
                caughtError.set(e);
                latch.countDown();
            }

            @Override
            public void onNext(Long args) {
                throw new RuntimeException("forced failure");
            }
        };
        f.observeOn(Schedulers.newThread())
        .safeSubscribe(subscriber);

        latch.await(2000, TimeUnit.MILLISECONDS);
        assertNotNull(caughtError.get());
    }
}
