/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.nbp;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.schedulers.Schedulers;

public class NbpErrorHandlingTests {

    /**
     * Test that an error from a user provided Observer.onNext is handled and emitted to the onError
     */
    @Test
    public void testOnNextError() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> caughtError = new AtomicReference<>();
        NbpObservable<Long> o = NbpObservable.interval(50, TimeUnit.MILLISECONDS);
        NbpSubscriber<Long> observer = new NbpObserver<Long>() {

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
        o.safeSubscribe(observer);

        latch.await(2000, TimeUnit.MILLISECONDS);
        assertNotNull(caughtError.get());
    }

    /**
     * Test that an error from a user provided Observer.onNext is handled and emitted to the onError
     * even when done across thread boundaries with observeOn
     */
    @Test
    public void testOnNextErrorAcrossThread() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> caughtError = new AtomicReference<>();
        NbpObservable<Long> o = NbpObservable.interval(50, TimeUnit.MILLISECONDS);
        NbpSubscriber<Long> observer = new NbpObserver<Long>() {

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
        o.observeOn(Schedulers.newThread())
        .safeSubscribe(observer);

        latch.await(2000, TimeUnit.MILLISECONDS);
        assertNotNull(caughtError.get());
    }
}