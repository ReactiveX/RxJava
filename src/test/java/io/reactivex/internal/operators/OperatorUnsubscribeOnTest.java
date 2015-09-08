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

package io.reactivex.internal.operators;

import static org.junit.Assert.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorUnsubscribeOnTest {

    @Test
    public void testUnsubscribeWhenSubscribeOnAndUnsubscribeOnAreOnSameThread() throws InterruptedException {
        UIEventLoopScheduler UI_EVENT_LOOP = new UIEventLoopScheduler();
        try {
            final ThreadSubscription subscription = new ThreadSubscription();
            final AtomicReference<Thread> subscribeThread = new AtomicReference<>();
            Observable<Integer> w = Observable.create(new Publisher<Integer>() {

                @Override
                public void subscribe(Subscriber<? super Integer> t1) {
                    subscribeThread.set(Thread.currentThread());
                    t1.onSubscribe(subscription);
                    t1.onNext(1);
                    t1.onNext(2);
                    t1.onComplete();
                }
            });

            TestSubscriber<Integer> observer = new TestSubscriber<>();
            w.subscribeOn(UI_EVENT_LOOP).observeOn(Schedulers.computation()).unsubscribeOn(UI_EVENT_LOOP).subscribe(observer);

            observer.awaitTerminalEvent(1, TimeUnit.SECONDS);
            observer.dispose();
            
            Thread unsubscribeThread = subscription.getThread();

            assertNotNull(unsubscribeThread);
            assertNotSame(Thread.currentThread(), unsubscribeThread);

            assertNotNull(subscribeThread.get());
            assertNotSame(Thread.currentThread(), subscribeThread.get());
            // True for Schedulers.newThread()

            System.out.println("unsubscribeThread: " + unsubscribeThread);
            System.out.println("subscribeThread.get(): " + subscribeThread.get());
            assertTrue(unsubscribeThread == UI_EVENT_LOOP.getThread());

            observer.assertValues(1, 2);
            observer.assertTerminated();
        } finally {
            UI_EVENT_LOOP.shutdown();
        }
    }

    @Test
    public void testUnsubscribeWhenSubscribeOnAndUnsubscribeOnAreOnDifferentThreads() throws InterruptedException {
        UIEventLoopScheduler UI_EVENT_LOOP = new UIEventLoopScheduler();
        try {
            final ThreadSubscription subscription = new ThreadSubscription();
            final AtomicReference<Thread> subscribeThread = new AtomicReference<>();
            Observable<Integer> w = Observable.create(new Publisher<Integer>() {

                @Override
                public void subscribe(Subscriber<? super Integer> t1) {
                    subscribeThread.set(Thread.currentThread());
                    t1.onSubscribe(subscription);
                    t1.onNext(1);
                    t1.onNext(2);
                    t1.onComplete();
                }
            });

            TestSubscriber<Integer> observer = new TestSubscriber<>();
            w.subscribeOn(Schedulers.newThread()).observeOn(Schedulers.computation()).unsubscribeOn(UI_EVENT_LOOP).subscribe(observer);

            observer.awaitTerminalEvent(1, TimeUnit.SECONDS);
            observer.dispose();
            
            Thread unsubscribeThread = subscription.getThread();

            assertNotNull(unsubscribeThread);
            assertNotSame(Thread.currentThread(), unsubscribeThread);

            assertNotNull(subscribeThread.get());
            assertNotSame(Thread.currentThread(), subscribeThread.get());
            // True for Schedulers.newThread()

            System.out.println("UI Thread: " + UI_EVENT_LOOP.getThread());
            System.out.println("unsubscribeThread: " + unsubscribeThread);
            System.out.println("subscribeThread.get(): " + subscribeThread.get());
            assertSame(unsubscribeThread, UI_EVENT_LOOP.getThread());

            observer.assertValues(1, 2);
            observer.assertTerminated();
        } finally {
            UI_EVENT_LOOP.shutdown();
        }
    }

    private static class ThreadSubscription implements Subscription {
        private volatile Thread thread;

        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void cancel() {
            System.out.println("unsubscribe invoked: " + Thread.currentThread());
            thread = Thread.currentThread();
            latch.countDown();
        }

        public Thread getThread() throws InterruptedException {
            latch.await();
            return thread;
        }
        
        @Override
        public void request(long n) {
            
        }
    }

    public static class UIEventLoopScheduler extends Scheduler {

        private final Scheduler eventLoop;
        private volatile Thread t;

        public UIEventLoopScheduler() {

            eventLoop = Schedulers.single();

            /*
             * DON'T DO THIS IN PRODUCTION CODE
             */
            final CountDownLatch latch = new CountDownLatch(1);
            eventLoop.scheduleDirect(new Runnable() {

                @Override
                public void run() {
                    t = Thread.currentThread();
                    latch.countDown();
                }

            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException("failed to initialize and get inner thread");
            }
        }
        
        @Override
        public Worker createWorker() {
            return eventLoop.createWorker();
        }

        public Thread getThread() {
            return t;
        }

    }
}