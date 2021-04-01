/*
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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.processors.BehaviorProcessor;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class FlowableDoOnUnsubscribeTest extends RxJavaTest {

    @Test
    public void doOnUnsubscribe() throws Exception {
        int subCount = 3;
        final CountDownLatch upperLatch = new CountDownLatch(subCount);
        final CountDownLatch lowerLatch = new CountDownLatch(subCount);
        final CountDownLatch onNextLatch = new CountDownLatch(subCount);

        final AtomicInteger upperCount = new AtomicInteger();
        final AtomicInteger lowerCount = new AtomicInteger();
        Flowable<Long> longs = Flowable
                // The stream needs to be infinite to ensure the stream does not terminate
                // before it is unsubscribed
                .interval(50, TimeUnit.MILLISECONDS)
                .doOnCancel(new Action() {
                    @Override
                    public void run() {
                        // Test that upper stream will be notified for un-subscription
                        // from a child subscriber
                            upperLatch.countDown();
                            upperCount.incrementAndGet();
                    }
                })
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) {
                            // Ensure there is at least some onNext events before un-subscription happens
                            onNextLatch.countDown();
                    }
                })
                .doOnCancel(new Action() {
                    @Override
                    public void run() {
                        // Test that lower stream will be notified for a direct un-subscription
                            lowerLatch.countDown();
                            lowerCount.incrementAndGet();
                    }
                });

        List<Disposable> subscriptions = new ArrayList<>();
        List<TestSubscriber<Long>> subscribers = new ArrayList<>();

        for (int i = 0; i < subCount; ++i) {
            TestSubscriber<Long> subscriber = new TestSubscriber<>();
            subscriptions.add(Disposable.fromSubscription(subscriber));
            longs.subscribe(subscriber);
            subscribers.add(subscriber);
        }

        onNextLatch.await();
        for (int i = 0; i < subCount; ++i) {
            subscriptions.get(i).dispose();
            // Test that unsubscribe() method is not affected in any way
        }

        upperLatch.await();
        lowerLatch.await();
        assertEquals(String.format("There should exactly %d un-subscription events for upper stream", subCount), subCount, upperCount.get());
        assertEquals(String.format("There should exactly %d un-subscription events for lower stream", subCount), subCount, lowerCount.get());
    }

    @Test
    public void doOnUnSubscribeWorksWithRefCount() throws Exception {
        int subCount = 3;
        final CountDownLatch upperLatch = new CountDownLatch(1);
        final CountDownLatch lowerLatch = new CountDownLatch(1);
        final CountDownLatch onNextLatch = new CountDownLatch(subCount);

        final AtomicInteger upperCount = new AtomicInteger();
        final AtomicInteger lowerCount = new AtomicInteger();
        Flowable<Long> longs = Flowable
                // The stream needs to be infinite to ensure the stream does not terminate
                // before it is unsubscribed
                .interval(50, TimeUnit.MILLISECONDS)
                .doOnCancel(new Action() {
                    @Override
                    public void run() {
                        // Test that upper stream will be notified for un-subscription
                            upperLatch.countDown();
                            upperCount.incrementAndGet();
                    }
                })
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) {
                            // Ensure there is at least some onNext events before un-subscription happens
                            onNextLatch.countDown();
                    }
                })
                .doOnCancel(new Action() {
                    @Override
                    public void run() {
                        // Test that lower stream will be notified for un-subscription
                            lowerLatch.countDown();
                            lowerCount.incrementAndGet();
                    }
                })
                .publish()
                .refCount();

        List<Disposable> subscriptions = new ArrayList<>();
        List<TestSubscriber<Long>> subscribers = new ArrayList<>();

        for (int i = 0; i < subCount; ++i) {
            TestSubscriber<Long> subscriber = new TestSubscriber<>();
            longs.subscribe(subscriber);
            subscriptions.add(Disposable.fromSubscription(subscriber));
            subscribers.add(subscriber);
        }

        onNextLatch.await();
        for (int i = 0; i < subCount; ++i) {
            subscriptions.get(i).dispose();
            // Test that unsubscribe() method is not affected in any way
        }

        upperLatch.await();
        lowerLatch.await();
        assertEquals("There should exactly 1 un-subscription events for upper stream", 1, upperCount.get());
        assertEquals("There should exactly 1 un-subscription events for lower stream", 1, lowerCount.get());
    }

    @Test
    public void noReentrantDispose() {

        final AtomicInteger cancelCalled = new AtomicInteger();

        final BehaviorProcessor<Integer> p = BehaviorProcessor.create();
        p.doOnCancel(new Action() {
            @Override
            public void run() throws Exception {
                cancelCalled.incrementAndGet();
                p.onNext(2);
            }
        })
        .firstOrError()
        .subscribe()
        .dispose();

        assertEquals(1, cancelCalled.get());
    }
}
