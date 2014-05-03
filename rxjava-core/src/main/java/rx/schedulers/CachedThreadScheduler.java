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
package rx.schedulers;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/* package */public class CachedThreadScheduler extends Scheduler {
    private static class CachedEventLoopPool {
        final ThreadFactory factory = new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "RxCachedThreadScheduler-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };

        private final long keepAliveTime;
        private final LinkedBlockingDeque<EventLoopScheduler> expiringQueue;

        CachedEventLoopPool(long keepAliveTime, TimeUnit unit) {
            this.keepAliveTime = unit.toNanos(keepAliveTime);
            this.expiringQueue = new LinkedBlockingDeque<EventLoopScheduler>();
        }

        private static CachedEventLoopPool INSTANCE = new CachedEventLoopPool(
            60L, TimeUnit.SECONDS
        );

        EventLoopScheduler takeEventLoop() {
            long startTimestamp = now();
            while (!expiringQueue.isEmpty()) {
                EventLoopScheduler eventLoopScheduler = null;
                try {
                    eventLoopScheduler = expiringQueue.pollFirst(keepAliveTime, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    // If we were interrupted, try again next loop
                }

                if (eventLoopScheduler != null &&
                    (startTimestamp - eventLoopScheduler.getExpirationTime()) <= keepAliveTime) {
                    return eventLoopScheduler;
                }

                // Don't spin too long trying to find a cached event loop
                if ((startTimestamp + keepAliveTime) > now()) {
                    break;
                }
            }

            // No suitable cached event loop found, or we timed out looking for one. Create a new one.
            return new EventLoopScheduler(factory);
        }

        void returnEventLoop(EventLoopScheduler eventLoopScheduler) {
            // Refresh expire time before putting event loop back in pool
            eventLoopScheduler.setExpirationTime(now());

            expiringQueue.addLast(eventLoopScheduler);
        }

        long now() {
            return System.nanoTime();
        }
    }

    @Override
    public Worker createWorker() {
        return new EventLoop();
    }

    private static class EventLoop extends Scheduler.Worker {
        private final CompositeSubscription innerSubscription = new CompositeSubscription();
        private final EventLoopScheduler pooledEventLoop;
        private final NewThreadScheduler.OnActionComplete onComplete;

        EventLoop() {
            pooledEventLoop = CachedEventLoopPool.INSTANCE.takeEventLoop();
            onComplete = new NewThreadScheduler.OnActionComplete() {
                @Override
                public void complete(Subscription s) {
                    innerSubscription.remove(s);
                }
            };
        }

        @Override
        public void unsubscribe() {
            innerSubscription.unsubscribe();
            CachedEventLoopPool.INSTANCE.returnEventLoop(pooledEventLoop);
        }

        @Override
        public boolean isUnsubscribed() {
            return innerSubscription.isUnsubscribed();
        }

        @Override
        public Subscription schedule(Action0 action) {
            if (innerSubscription.isUnsubscribed()) {
                // don't schedule, we are unsubscribed
                return Subscriptions.empty();
            }
            return pooledEventLoop.schedule(action, onComplete);
        }

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            if (innerSubscription.isUnsubscribed()) {
                // don't schedule, we are unsubscribed
                return Subscriptions.empty();
            }

            return pooledEventLoop.schedule(action, delayTime, unit, onComplete);
        }
    }

    private static class EventLoopScheduler extends NewThreadScheduler.EventLoopScheduler {
        private long expirationTime;

        EventLoopScheduler(ThreadFactory threadFactory) {
            super(threadFactory);
            this.expirationTime = 0L;
        }

        public long getExpirationTime() {
            return expirationTime;
        }

        public void setExpirationTime(long expirationTime) {
            this.expirationTime = expirationTime;
        }
    }
}
