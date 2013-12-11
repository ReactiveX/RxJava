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
import rx.schedulers.CurrentThreadScheduler;
import rx.schedulers.ImmediateScheduler;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.MultipleAssignmentSubscription;
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
                return new Observation(observer).subscribe();
            }
        }

        /** Observe through individual queue per observer. */
        private class Observation {
            final Observer<? super T> observer;
            final CompositeSubscription compositeSubscription = new CompositeSubscription();
            final MultipleAssignmentSubscription recursiveSubscription = new MultipleAssignmentSubscription();
            final ConcurrentLinkedQueue<Notification<? extends T>> queue = new ConcurrentLinkedQueue<Notification<? extends T>>();
            final AtomicInteger counter = new AtomicInteger(0);

            public Observation(Observer<? super T> observer) {
                this.observer = observer;
                // we want the recursiveSubscription unsubscribed if the parent compositiveSubscription is unsubscribed
                compositeSubscription.add(recursiveSubscription);
            }

            public Subscription subscribe() {
                // schedule on the outer scheduler first to perform the subscription
                compositeSubscription.add(scheduler.schedule(null, new Func2<Scheduler, T, Subscription>() {
                    @Override
                    public Subscription call(final Scheduler innerScheduler, T state) {
                        // we're now on the scheduler so let's subscribe
                        return source.materialize().subscribe(new Action1<Notification<? extends T>>() {

                            @Override
                            public void call(Notification<? extends T> notification) {
                                // we are receiving events so put them in the queue
                                queue.offer(notification);
                                /*
                                 * We use a counter to track whether the inner scheduler is processing or not
                                 * 
                                 * If 0 then we schedule on the inner scheduler, otherwise there is a task already running
                                 * and it will iterate to pick up what was put in the queue.
                                 */
                                if (counter.getAndIncrement() == 0) {
                                    // no task running so schedule the work
                                    recursiveSubscription.setSubscription(innerScheduler.schedule(new Action1<Action0>() {
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
                        });
                    }
                }));
                return compositeSubscription;

            }

        }
    }

}