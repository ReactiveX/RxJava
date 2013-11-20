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
import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.CurrentThreadScheduler;
import rx.concurrency.ImmediateScheduler;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.Func2;

/**
 * Asynchronously notify Observers on the specified Scheduler.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/observeOn.png">
 */
public class OperationObserveOn {

    public static <T> OnSubscribeFunc<T> observeOn(Observable<? extends T> source, Scheduler scheduler) {
        return new ObserveOn<T>(source, scheduler);
    }

    private static class ObserveOn<T> implements OnSubscribeFunc<T> {
        private final Observable<? extends T> source;
        private final Scheduler scheduler;
        private volatile Scheduler recursiveScheduler;

        final ConcurrentLinkedQueue<Notification<? extends T>> queue = new ConcurrentLinkedQueue<Notification<? extends T>>();
        final AtomicInteger counter = new AtomicInteger(0);

        public ObserveOn(Observable<? extends T> source, Scheduler scheduler) {
            this.source = source;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            if (scheduler instanceof ImmediateScheduler) {
                // do nothing if we request ImmediateScheduler so we don't invoke overhead
                return source.subscribe(observer);
            } else if (scheduler instanceof CurrentThreadScheduler) {
                // do nothing if we request CurrentThreadScheduler so we don't invoke overhead
                return source.subscribe(observer);
            } else {
                return observeOn(observer, scheduler);
            }
        }

        public Subscription observeOn(final Observer<? super T> observer, final Scheduler scheduler) {
            final CompositeSubscription s = new CompositeSubscription();

            s.add(source.materialize().subscribe(new Action1<Notification<? extends T>>() {

                @Override
                public void call(Notification<? extends T> e) {
                    // this must happen before 'counter' is used to provide synchronization between threads
                    queue.offer(e);

                    // we now use counter to atomically determine if we need to start processing or not
                    // it will be 0 if it's the first notification or the scheduler has finished processing work
                    // and we need to start doing it again
                    if (counter.getAndIncrement() == 0) {
                        if (recursiveScheduler == null) {
                            s.add(scheduler.schedule(null, new Func2<Scheduler, T, Subscription>() {

                                @Override
                                public Subscription call(Scheduler innerScheduler, T state) {
                                    // record innerScheduler so 'processQueue' can use it for all subsequent executions
                                    recursiveScheduler = innerScheduler;

                                    processQueue(s, observer);

                                    return Subscriptions.empty();
                                }
                            }));
                        } else {
                            processQueue(s, observer);
                        }
                    }

                }
            }));

            return s;
        }

        /**
         * This uses 'recursiveScheduler' NOT 'scheduler' as it should reuse the same scheduler each time it processes.
         * This means it must first get the recursiveScheduler when it first executes.
         */
        private void processQueue(final CompositeSubscription s, final Observer<? super T> observer) {

            s.add(recursiveScheduler.schedule(new Action1<Action0>() {
                @Override
                public void call(Action0 self) {
                    Notification<? extends T> not = queue.poll();
                    if (not != null) {
                        not.accept(observer);
                    }

                    // decrement count and if we still have work to do
                    // recursively schedule ourselves to process again
                    if (counter.decrementAndGet() > 0) {
                        self.call();
                    }

                }
            }));
        }
    }

}
