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
package rx.operators;

import rx.Notification;
import rx.Scheduler;
import rx.Subscriber;
import rx.subscriptions.CompositeSubscription;
import rx.util.functions.Action0;

/**
 * Move the observation of events to another thread via Scheduler.
 * @param <T> the item type
 */
public class OperatorObserveOn<T> implements Operator<T, T> {
    final Scheduler scheduler;
    public OperatorObserveOn(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> t1) {
        final QueueDrain qd = new QueueDrain(t1);
        final CompositeSubscription csub = new CompositeSubscription();
        return new Subscriber<T>(t1) {
            /** Dispatch the notification value. */
            void run(final Notification<T> nt) {
                qd.enqueue(new Action0() {
                    @Override
                    public void call() {
                        nt.accept(t1);
                    }
                });
                qd.tryDrainAsync(scheduler, csub);
            }
            @Override
            public void onNext(final T args) {
                run(Notification.createOnNext(args));
            }

            @Override
            public void onError(final Throwable e) {
                run(Notification.<T>createOnError(e));
            }

            @Override
            public void onCompleted() {
                run(Notification.<T>createOnCompleted());
            }
        };
    }
    
}
