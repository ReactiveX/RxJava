/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.schedulers.Schedulers;

public class ErrorHandlingTests {

    /**
     * Test that an error from a user provided Observer.onNext is handled and emitted to the onError
     */
    @Test
    public void testOnNextError() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> caughtError = new AtomicReference<Throwable>();
        Observable<Long> o = Observable.interval(50, TimeUnit.MILLISECONDS);
        Subscriber<Long> observer = new Subscriber<Long>() {

            @Override
            public void onCompleted() {
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
        o.subscribe(observer);

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
        final AtomicReference<Throwable> caughtError = new AtomicReference<Throwable>();
        Observable<Long> o = Observable.interval(50, TimeUnit.MILLISECONDS);
        Subscriber<Long> observer = new Subscriber<Long>() {

            @Override
            public void onCompleted() {
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
        o.observeOn(Schedulers.newThread()).subscribe(observer);

        latch.await(2000, TimeUnit.MILLISECONDS);
        assertNotNull(caughtError.get());
    }
}
