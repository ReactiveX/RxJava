/**
 * Copyright 2013 Netflix, Inc.
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
package rx.operators;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Notification;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.util.functions.Func2;

/* package */class ScheduledObserver<T> implements Observer<T> {
    private final Observer<? super T> underlying;
    private final Scheduler scheduler;
    private final CompositeSubscription parentSubscription;
    private final EventLoop eventLoop = new EventLoop();
    final AtomicInteger counter = new AtomicInteger();
    private final AtomicBoolean started = new AtomicBoolean();

    private final ConcurrentLinkedQueue<Notification<? extends T>> queue = new ConcurrentLinkedQueue<Notification<? extends T>>();
    
    
    public ScheduledObserver(CompositeSubscription s, Observer<? super T> underlying, Scheduler scheduler) {
        this.parentSubscription = s;
        this.underlying = underlying;
        this.scheduler = scheduler;
    }

    @Override
    public void onCompleted() {
        enqueue(new Notification<T>());
    }

    @Override
    public void onError(final Throwable e) {
        enqueue(new Notification<T>(e));
    }

    @Override
    public void onNext(final T args) {
        enqueue(new Notification<T>(args));
    }

    private void enqueue(Notification<? extends T> notification) {
        // this must happen before synchronization between threads
        queue.offer(notification);

        /**
         * If the counter is currently at 0 (before incrementing with this addition)
         * we will schedule the work.
         */
        if (counter.getAndIncrement() <= 0) {
            if (!started.get() && started.compareAndSet(false, true)) {
                // first time we use the parent scheduler to start the event loop
                MultipleAssignmentSubscription recursiveSubscription = new MultipleAssignmentSubscription();
                parentSubscription.add(scheduler.schedule(recursiveSubscription, eventLoop));
                parentSubscription.add(recursiveSubscription);
            } else {
                // subsequent times we reschedule existing one
                eventLoop.reschedule();
            }
        }
    }

    private class EventLoop implements Func2<Scheduler, MultipleAssignmentSubscription, Subscription> {

        volatile Scheduler _recursiveScheduler;
        volatile MultipleAssignmentSubscription _recursiveSubscription;

        public void reschedule() {
            _recursiveSubscription.setSubscription(_recursiveScheduler.schedule(_recursiveSubscription, this));
        }

        @Override
        public Subscription call(Scheduler s, MultipleAssignmentSubscription recursiveSubscription) {
            /*
             * --------------------------------------------------------------------------------------
             * Set these the first time through so we can externally trigger recursive execution again
             */
            if (_recursiveScheduler == null) {
                _recursiveScheduler = s;
            }
            if (_recursiveSubscription == null) {
                _recursiveSubscription = recursiveSubscription;
            }
            /*
             * Back to regular flow
             * --------------------------------------------------------------------------------------
             */

            do {
                Notification<? extends T> notification = queue.poll();
                // if we got a notification, send it
                if (notification != null) {

                    // if unsubscribed stop working
                    if (parentSubscription.isUnsubscribed()) {
                        return parentSubscription;
                    }
                    // process notification

                    switch (notification.getKind()) {
                    case OnNext:
                        underlying.onNext(notification.getValue());
                        break;
                    case OnError:
                        underlying.onError(notification.getThrowable());
                        break;
                    case OnCompleted:
                        underlying.onCompleted();
                        break;
                    default:
                        throw new IllegalStateException("Unknown kind of notification " + notification);
                    }
                }
            } while (counter.decrementAndGet() > 0);

            return parentSubscription;
        }
    }
}
