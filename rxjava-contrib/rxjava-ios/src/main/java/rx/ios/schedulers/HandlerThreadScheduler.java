/**
 * Copyright 2013 Netflix, Inc.
 * Copyright 2014 Ashley Williams
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

package rx.ios.schedulers;

import org.robovm.apple.foundation.NSOperationQueue;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.internal.util.RxThreadFactory;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Schedules actions to run on an iOS Handler thread.
 */
public class HandlerThreadScheduler extends Scheduler {

    private final NSOperationQueue operationQueue;
    private static final String THREAD_PREFIX = "RxiOSScheduledExecutorPool-";


    public HandlerThreadScheduler(NSOperationQueue operationQueue) {
        this.operationQueue = operationQueue;
    }

    @Override
    public Worker createWorker() {
        return new InnerHandlerThreadScheduler(operationQueue);
    }


    private static class InnerHandlerThreadScheduler extends Worker {

        private final NSOperationQueue operationQueue;
        private CompositeSubscription innerSubscription = new CompositeSubscription();


        public InnerHandlerThreadScheduler(NSOperationQueue operationQueue) {
            this.operationQueue = operationQueue;
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
        public Subscription schedule(final Action0 action) {
            return schedule(action, 0, null);
        }

        @Override
        public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
            return scheduledAction(action, delayTime, unit);
        }

        public Subscription scheduledAction(final Action0 action, long delay, TimeUnit unit) {

            if (innerSubscription.isUnsubscribed()) {
                return Subscriptions.empty();
            }

            final ScheduledIOSAction scheduledAction = new ScheduledIOSAction(action, operationQueue);
            final ScheduledExecutorService executor = IOSScheduledExecutorPool.getInstance();

            Future<?> future;
            if (delay <= 0) {
                future = executor.submit(scheduledAction);
            } else {
                future = executor.schedule(scheduledAction, delay, unit);
            }

            scheduledAction.add(Subscriptions.from(future));
            scheduledAction.addParent(innerSubscription);

            return scheduledAction;
        }
    }


    private static final class IOSScheduledExecutorPool {

        private static final RxThreadFactory THREAD_FACTORY = new RxThreadFactory(THREAD_PREFIX);

        private static IOSScheduledExecutorPool INSTANCE = new IOSScheduledExecutorPool();
        private final ScheduledExecutorService executorService;

        private IOSScheduledExecutorPool() {
            executorService = Executors.newScheduledThreadPool(1, THREAD_FACTORY);
        }

        public static ScheduledExecutorService getInstance() {
            return INSTANCE.executorService;
        }
    }

}
