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

import rx.Notification;
import rx.Observer;
import rx.Scheduler;
import rx.util.functions.Action0;

import java.util.concurrent.ConcurrentLinkedQueue;

/* package */class ScheduledObserver<T> implements Observer<T> {
    private final Observer<T> underlying;
    private final Scheduler scheduler;

    private final ConcurrentLinkedQueue<Notification<T>> queue = new ConcurrentLinkedQueue<Notification<T>>();
    private final Object lock = new Object();

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
        boolean schedule;
        synchronized (lock) {
            schedule = queue.isEmpty();

            queue.offer(notification);
        }

        if (schedule) {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    Notification<T> not = dequeue();

                    if (not == null) {
                        return;
                    }

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

                    scheduler.schedule(this);

                }
            });
        }
    }

    private Notification<T> dequeue() {
        synchronized (lock) {
            return queue.poll();
        }
    }
}
