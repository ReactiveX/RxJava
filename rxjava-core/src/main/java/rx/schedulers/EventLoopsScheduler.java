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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.NewThreadScheduler.OnActionComplete;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/* package */class EventLoopsScheduler extends Scheduler {

    private static class ComputationSchedulerPool {
        final int cores = Runtime.getRuntime().availableProcessors();
        final ThreadFactory factory = new ThreadFactory() {
            final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "RxComputationThreadPool-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };

        final EventLoopScheduler[] eventLoops;

        ComputationSchedulerPool() {
            // initialize event loops
            eventLoops = new EventLoopScheduler[cores];
            for (int i = 0; i < cores; i++) {
                eventLoops[i] = new EventLoopScheduler(factory);
            }
        }

        private static ComputationSchedulerPool INSTANCE = new ComputationSchedulerPool();

        long n = 0;

        public EventLoopScheduler getEventLoop() {
            // round-robin selection (improvements to come)
            return eventLoops[(int) (n++ % cores)];
        }

    }

    @Override
    public Worker createWorker() {
        return new EventLoop();
    }

    private static class EventLoop extends Scheduler.Worker {
        private final CompositeSubscription innerSubscription = new CompositeSubscription();
        private final EventLoopScheduler pooledEventLoop;
        private final OnActionComplete onComplete;

        EventLoop() {
            pooledEventLoop = ComputationSchedulerPool.INSTANCE.getEventLoop();
            onComplete = new OnActionComplete() {

                @Override
                public void complete(Subscription s) {
                    innerSubscription.remove(s);
                }

            };
        }

        @Override
        public void unsubscribe() {
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
