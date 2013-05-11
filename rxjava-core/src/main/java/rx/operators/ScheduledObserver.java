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
import java.util.concurrent.atomic.AtomicInteger;

import rx.Notification;
import rx.Observer;
import rx.Scheduler;
import rx.util.functions.Action0;

/* package */class ScheduledObserver<T> implements Observer<T> {
    private final Observer<T> underlying;
    private final Scheduler scheduler;

    private final ConcurrentLinkedQueue<Notification<T>> queue = new ConcurrentLinkedQueue<Notification<T>>();
    private final AtomicInteger counter = new AtomicInteger(0);

    public ScheduledObserver(Observer<T> underlying, Scheduler scheduler) {
        this.underlying = underlying;
        this.scheduler = scheduler;
    }

    @Override
    public void onCompleted() {
        enqueue(new Notification<T>());
    }

    @Override
    public void onError(final Exception e) {
        enqueue(new Notification<T>(e));
    }

    @Override
    public void onNext(final T args) {
        enqueue(new Notification<T>(args));
    }

    private void enqueue(Notification<T> notification) {
        // this must happen before 'counter' is used to provide synchronization between threads
        queue.offer(notification);

        // we now use counter to atomically determine if we need to start processing or not
        // it will be 0 if it's the first notification or the scheduler has finished processing work
        // and we need to start doing it again
        if (counter.getAndIncrement() == 0) {
            processQueue();
        }
    }

    private void processQueue() {
        scheduler.schedule(new Action0() {
            @Override
            public void call() {
                Notification<T> not = queue.poll();

                switch (not.getKind()) {
                case OnNext:
                    underlying.onNext(not.getValue());
                    break;
                case OnError:
                    underlying.onError(not.getException());
                    break;
                case OnCompleted:
                    underlying.onCompleted();
                    break;
                default:
                    throw new IllegalStateException("Unknown kind of notification " + not);

                }

                // decrement count and if we still have work to do
                // recursively schedule ourselves to process again
                if (counter.decrementAndGet() > 0) {
                    scheduler.schedule(this);
                }

            }
        });
    }
}
