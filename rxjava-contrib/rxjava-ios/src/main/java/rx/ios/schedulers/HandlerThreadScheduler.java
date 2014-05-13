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

import java.util.concurrent.TimeUnit;


import org.robovm.apple.foundation.NSBlockOperation;
import org.robovm.apple.foundation.NSOperationQueue;
import rx.Scheduler;

import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Schedules actions to run on an iOS Handler thread.
 */
public class HandlerThreadScheduler extends Scheduler {

    private final NSOperationQueue operationQueue;

    public HandlerThreadScheduler(NSOperationQueue operationQueue) {
        this.operationQueue = operationQueue;
    }

    @Override
    public Worker createWorker() {
        return new InnerHandlerThreadScheduler(operationQueue);
    }


    private static class InnerHandlerThreadScheduler extends Worker {

        private final NSOperationQueue operationQueue;
        private BooleanSubscription innerSubscription = new BooleanSubscription();

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
        public Subscription schedule(Action0 action0) {
            return schedule(action0, 0, TimeUnit.MILLISECONDS);
        }

        @Override
        public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
            final long delay = unit.toMillis(delayTime);

            final Runnable delayLaunch = new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        System.err.println("iOS post-delay thread interrupted");
                    }
                }
            };

            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (isUnsubscribed()) {
                        return;
                    }
                    action.call();
                }
            };

            /* create the two operations */
            NSBlockOperation delayOperation = new NSBlockOperation();
            NSBlockOperation runOperation = new NSBlockOperation();
            delayOperation.addExecutionBlock$(delayLaunch);
            runOperation.addExecutionBlock$(runnable);

            /* add the delay operation as a dependency to the run operation*/
            runOperation.addDependency$(delayOperation);

            /* add both operations to the queue */
            operationQueue.addOperation$(delayOperation);
            operationQueue.addOperation$(runOperation);

            return Subscriptions.empty();
        }
    }
}
