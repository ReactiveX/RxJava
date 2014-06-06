package rx.ios.schedulers;
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


import org.robovm.apple.foundation.NSBlockOperation;
import org.robovm.apple.foundation.NSOperationQueue;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            final NSBlockOperation runOperation = new NSBlockOperation();

            executor.schedule(new Runnable() {
                @Override
                public void run() {
                    if (isUnsubscribed()) {
                        return;
                    }
                    /* Runnable for action */
                    final Runnable actionRunner = new Runnable() {
                        @Override
                        public void run() {
                            action.call();
                        }
                    };

                    runOperation.addExecutionBlock$(actionRunner);

                    /* Add operation to operation queue*/
                    operationQueue.addOperation(runOperation);
                }
            }, delayTime, unit);

            return Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    runOperation.cancel();
                }
            });
        }
    }
}
