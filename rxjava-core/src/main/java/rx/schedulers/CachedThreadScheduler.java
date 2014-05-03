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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CachedThreadScheduler extends Scheduler {
    private static class EventLoopSchedulerPool {
        final ThreadFactory factory = new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "RxCachedThreadScheduler-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };

        private final long expirationTime;
        private final ConcurrentHashMap<EventLoopScheduler, Long> expiringQueue;

        EventLoopSchedulerPool(long expirationTime, TimeUnit unit) {
            this.expirationTime = unit.toNanos(expirationTime);
            this.expiringQueue = new ConcurrentHashMap<EventLoopScheduler, Long>();
        }

        private static EventLoopSchedulerPool INSTANCE = new EventLoopSchedulerPool(
            60L, TimeUnit.SECONDS
        );

        EventLoopScheduler get() {
            long now = now();
            if (!expiringQueue.isEmpty()) {
                for (Map.Entry<EventLoopScheduler, Long> eventLoopSchedulerExpirationTimeEntry : expiringQueue.entrySet()) {
                    expiringQueue.remove(eventLoopSchedulerExpirationTimeEntry.getKey());
                    if((now - eventLoopSchedulerExpirationTimeEntry.getValue()) <= expirationTime) {
                        return eventLoopSchedulerExpirationTimeEntry.getKey();
                    }
                }
            }

            return new EventLoopScheduler(factory);
        }

        void put(EventLoopScheduler eventLoopScheduler) {
            expiringQueue.put(eventLoopScheduler, now());
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
            pooledEventLoop = EventLoopSchedulerPool.INSTANCE.get();
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
            EventLoopSchedulerPool.INSTANCE.put(pooledEventLoop);
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
        EventLoopScheduler(ThreadFactory threadFactory) {
            super(threadFactory);
        }
    }
}
