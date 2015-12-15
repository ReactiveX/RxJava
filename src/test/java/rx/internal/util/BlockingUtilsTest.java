/**
 * Copyright 2015 Netflix, Inc.
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

package rx.internal.util;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;

import rx.*;
import rx.schedulers.Schedulers;

/**
 * Test suite for {@link BlockingUtils}.
 */
public class BlockingUtilsTest {
    
    @Before
    @After
    public void before() {
        // make sure the interrupted flag is cleared
        Thread.interrupted();
    }
    
    @Test
    public void awaitCompleteShouldReturnIfCountIsZero() {
        Subscription subscription = mock(Subscription.class);
        CountDownLatch latch = new CountDownLatch(0);
        BlockingUtils.awaitForComplete(latch, subscription);
        verifyZeroInteractions(subscription);
    }

    @Test
    public void awaitCompleteShouldReturnOnEmpty() {
        final CountDownLatch latch = new CountDownLatch(1);
        Subscriber<Object> subscription = createSubscription(latch);
        Observable<Object> observable = Observable.empty().subscribeOn(Schedulers.newThread());
        observable.subscribe(subscription);
        BlockingUtils.awaitForComplete(latch, subscription);
    }

    @Test
    public void awaitCompleteShouldReturnOnError() {
        final CountDownLatch latch = new CountDownLatch(1);
        Subscriber<Object> subscription = createSubscription(latch);
        Observable<Object> observable = Observable.error(new RuntimeException()).subscribeOn(Schedulers.newThread());
        observable.subscribe(subscription);
        BlockingUtils.awaitForComplete(latch, subscription);
    }

    @Test
    public void shouldThrowRuntimeExceptionOnThreadInterrupted() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final Subscription subscription = mock(Subscription.class);
        final AtomicReference<Exception> caught = new AtomicReference<Exception>();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().interrupt();
                try {
                    BlockingUtils.awaitForComplete(latch, subscription);
                } catch (RuntimeException e) {
                    caught.set(e);
                }
            }
        });
        thread.run();
        verify(subscription).unsubscribe();
        Exception actual = caught.get();
        assertNotNull(actual);
        assertNotNull(actual.getCause());
        assertTrue(actual.getCause() instanceof InterruptedException);
    }


    private static <T> Subscriber<T> createSubscription(final CountDownLatch latch) {
        return new Subscriber<T>() {
            @Override
            public void onNext(T t) {
                //no-oop
            }

            @Override
            public void onError(Throwable e) {
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };
    }
}
