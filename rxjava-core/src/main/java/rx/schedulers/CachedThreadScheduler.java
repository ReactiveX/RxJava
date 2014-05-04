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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
        private final ConcurrentLinkedQueue<EventLoopScheduler> expiringQueue;
        private final ScheduledExecutorService evictExpiredEventLoopExecutor;

        CachedEventLoopPool(long keepAliveTime, TimeUnit unit) {
            this.keepAliveTime = unit.toNanos(keepAliveTime);
            this.expiringQueue = new ConcurrentLinkedQueue<EventLoopScheduler>();

            evictExpiredEventLoopExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                final AtomicInteger counter = new AtomicInteger();

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "RxCachedEventLoopEvictor-" + counter.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            });
            evictExpiredEventLoopExecutor.scheduleWithFixedDelay(
                new Runnable() {
                    @Override
                    public void run() {
                        evictExpiredEventLoops();
                    }
                }, this.keepAliveTime, this.keepAliveTime, TimeUnit.NANOSECONDS
            );
        }

        private static CachedEventLoopPool INSTANCE = new CachedEventLoopPool(
            60L, TimeUnit.SECONDS
        );

        EventLoopScheduler takeEventLoop() {
            while (!expiringQueue.isEmpty()) {
                EventLoopScheduler eventLoopScheduler = expiringQueue.poll();
                if (eventLoopScheduler != null) {
                    return eventLoopScheduler;
                }
            }

            // No non-expired cached event loop found, so create a new one.
            return new EventLoopScheduler(factory);
        }

        void returnEventLoop(EventLoopScheduler eventLoopScheduler) {
            // Refresh expire time before putting event loop back in pool
            eventLoopScheduler.setExpirationTime(now() + keepAliveTime);

            expiringQueue.add(eventLoopScheduler);
        }

        void evictExpiredEventLoops() {
            long currentTimestamp = now();
            while (!expiringQueue.isEmpty()) {
                EventLoopScheduler eventLoopScheduler = expiringQueue.poll();
                if (eventLoopScheduler != null && eventLoopScheduler.getExpirationTime() > currentTimestamp) {
                    // Queue is ordered with the event loops that will expire first in the beginning,
                    // so when we find a non-expired event loop we can stop evicting.
                    // TODO: Since we use a concurrent queue, we can only put it back as tail.
                    // When we can start requiring 1.7, use ConcurrentLinkedDeque and put it back as head.
                    expiringQueue.add(eventLoopScheduler);
                    break;
                }
            }
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
        private final AtomicBoolean returnEventLoopOnce = new AtomicBoolean(false);

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
            if (returnEventLoopOnce.compareAndSet(false, true)) {
                // unsubscribe should be idempotent, so only do this once
                CachedEventLoopPool.INSTANCE.returnEventLoop(pooledEventLoop);
            }
            innerSubscription.unsubscribe();
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

            Subscription subscription = pooledEventLoop.schedule(action, onComplete);
            innerSubscription.add(subscription);
            return subscription;
        }

        @Override
        public Subscription schedule(Action0 action, long delayTime, TimeUnit unit) {
            if (innerSubscription.isUnsubscribed()) {
                // don't schedule, we are unsubscribed
                return Subscriptions.empty();
            }

            Subscription subscription = pooledEventLoop.schedule(action, delayTime, unit, onComplete);
            innerSubscription.add(subscription);
            return subscription;
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
