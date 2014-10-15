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
package rx.internal.operators;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observers.TestSubscriber;

public class OperatorDoOnUnsubscribeTest {
    
    @Test
    public void testDoOnUnsubscribe() throws Exception {
        int subCount = 3;
        final CountDownLatch upperLatch = new CountDownLatch(subCount);
        final CountDownLatch lowerLatch = new CountDownLatch(subCount);
        final CountDownLatch onNextLatch = new CountDownLatch(subCount);

        final AtomicInteger upperCount = new AtomicInteger();
        final AtomicInteger lowerCount = new AtomicInteger();
        Observable<Long> longs = Observable
                // The stream needs to be infinite to ensure the stream does not terminate
                // before it is unsubscribed
                .interval(50, TimeUnit.MILLISECONDS)
                .doOnUnsubscribe(new Action0() {
                    // Test that upper stream will be notified for un-subscription
                    // from a child subscriber
                    @Override
                    public void call() {
                        upperLatch.countDown();
                        upperCount.incrementAndGet();
                    }
                })
                .doOnNext(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        // Ensure there is at least some onNext events before un-subscription happens
                        onNextLatch.countDown();
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    // Test that lower stream will be notified for a direct un-subscription
                    @Override
                    public void call() {
                        lowerLatch.countDown();
                        lowerCount.incrementAndGet();
                    }
                });

        List<Subscription> subscriptions = new ArrayList<Subscription>();
        List<TestSubscriber<Long>> subscribers = new ArrayList<TestSubscriber<Long>>();

        for (int i = 0; i < subCount; ++i) {
            TestSubscriber<Long> subscriber = new TestSubscriber<Long>();
            subscriptions.add(longs.subscribe(subscriber));
            subscribers.add(subscriber);
        }

        onNextLatch.await();
        for (int i = 0; i < subCount; ++i) {
            subscriptions.get(i).unsubscribe();
            // Test that unsubscribe() method is not affected in any way
            subscribers.get(i).assertUnsubscribed();
        }

        upperLatch.await();
        lowerLatch.await();
        assertEquals(String.format("There should exactly %d un-subscription events for upper stream", subCount), subCount, upperCount.get());
        assertEquals(String.format("There should exactly %d un-subscription events for lower stream", subCount), subCount, lowerCount.get());
    }

    @Test
    public void testDoOnUnSubscribeWorksWithRefCount() throws Exception {
        int subCount = 3;
        final CountDownLatch upperLatch = new CountDownLatch(1);
        final CountDownLatch lowerLatch = new CountDownLatch(1);
        final CountDownLatch onNextLatch = new CountDownLatch(subCount);

        final AtomicInteger upperCount = new AtomicInteger();
        final AtomicInteger lowerCount = new AtomicInteger();
        Observable<Long> longs = Observable
                // The stream needs to be infinite to ensure the stream does not terminate
                // before it is unsubscribed
                .interval(50, TimeUnit.MILLISECONDS)
                .doOnUnsubscribe(new Action0() {
                    // Test that upper stream will be notified for un-subscription
                    @Override
                    public void call() {
                        upperLatch.countDown();
                        upperCount.incrementAndGet();
                    }
                })
                .doOnNext(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        // Ensure there is at least some onNext events before un-subscription happens
                        onNextLatch.countDown();
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    // Test that lower stream will be notified for un-subscription
                    @Override
                    public void call() {
                        lowerLatch.countDown();
                        lowerCount.incrementAndGet();
                    }
                })
                .publish()
                .refCount();

        List<Subscription> subscriptions = new ArrayList<Subscription>();
        List<TestSubscriber<Long>> subscribers = new ArrayList<TestSubscriber<Long>>();

        for (int i = 0; i < subCount; ++i) {
            TestSubscriber<Long> subscriber = new TestSubscriber<Long>();
            subscriptions.add(longs.subscribe(subscriber));
            subscribers.add(subscriber);
        }

        onNextLatch.await();
        for (int i = 0; i < subCount; ++i) {
            subscriptions.get(i).unsubscribe();
            // Test that unsubscribe() method is not affected in any way
            subscribers.get(i).assertUnsubscribed();
        }

        upperLatch.await();
        lowerLatch.await();
        assertEquals("There should exactly 1 un-subscription events for upper stream", 1, upperCount.get());
        assertEquals("There should exactly 1 un-subscription events for lower stream", 1, lowerCount.get());
    }
}
