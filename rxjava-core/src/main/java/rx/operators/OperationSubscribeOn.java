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

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Action1;

/**
 * Asynchronously subscribes and unsubscribes Observers on the specified Scheduler.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/subscribeOn.png">
 */
public class OperationSubscribeOn {

    public static <T> OnSubscribeFunc<T> subscribeOn(Observable<? extends T> source, Scheduler scheduler) {
        return new SubscribeOn<T>(source, scheduler);
    }

    private static class SubscribeOn<T> implements OnSubscribeFunc<T> {
        private final Observable<? extends T> source;
        private final Scheduler scheduler;

        public SubscribeOn(Observable<? extends T> source, Scheduler scheduler) {
            this.source = source;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            final CompositeSubscription s = new CompositeSubscription();
            scheduler.schedule(new Action1<Inner>() {

                @Override
                public void call(final Inner inner) {
                    s.add(new ScheduledSubscription(source.subscribe(observer), inner));
                }

            });
            // only include the ScheduledSubscription
            // but not the actual Subscription from the Scheduler as we need to schedule the unsubscribe action
            // and therefore can't unsubscribe the scheduler until after the unsubscribe happens
            return s;
        }
    }

    private static class ScheduledSubscription implements Subscription {
        private final Subscription underlying;
        private volatile boolean unsubscribed = false;
        private final Scheduler.Inner scheduler;

        private ScheduledSubscription(Subscription underlying, Inner scheduler) {
            this.underlying = underlying;
            this.scheduler = scheduler;
        }

        @Override
        public void unsubscribe() {
            unsubscribed = true;
            scheduler.schedule(new Action1<Inner>() {
                @Override
                public void call(Inner inner) {
                    underlying.unsubscribe();
                    // tear down this subscription as well now that we're done
                    inner.unsubscribe();
                }
            });
        }

        @Override
        public boolean isUnsubscribed() {
            return unsubscribed;
        }
    }
}
