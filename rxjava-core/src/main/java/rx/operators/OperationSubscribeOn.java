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

import org.junit.Test;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.util.functions.Action0;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class OperationSubscribeOn {

    public static <T> Func1<Observer<T>, Subscription> subscribeOn(Observable<T> source, Scheduler scheduler) {
        return new SubscribeOn<T>(source, scheduler);
    }

    private static class SubscribeOn<T> implements Func1<Observer<T>, Subscription> {
        private final Observable<T> source;
        private final Scheduler scheduler;

        public SubscribeOn(Observable<T> source, Scheduler scheduler) {
            this.source = source;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription call(final Observer<T> observer) {
            return scheduler.schedule(new Func0<Subscription>() {
                @Override
                public Subscription call() {
                    return new ScheduledSubscription(source.subscribe(observer), scheduler);
                }
            });
        }
    }

    private static class ScheduledSubscription implements Subscription {
        private final Subscription underlying;
        private final Scheduler scheduler;

        private ScheduledSubscription(Subscription underlying, Scheduler scheduler) {
            this.underlying = underlying;
            this.scheduler = scheduler;
        }

        @Override
        public void unsubscribe() {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    underlying.unsubscribe();
                }
            });
        }
    }

    public static class UnitTest {

        @Test
        @SuppressWarnings("unchecked")
        public void testSubscribeOn() {
            Observable<Integer> w = Observable.toObservable(1, 2, 3);

            Scheduler scheduler = spy(OperatorTester.UnitTest.forwardingScheduler(Schedulers.immediate()));

            Observer<Integer> observer = mock(Observer.class);
            Subscription subscription = Observable.create(subscribeOn(w, scheduler)).subscribe(observer);

            verify(scheduler, times(1)).schedule(any(Func0.class));
            subscription.unsubscribe();
            verify(scheduler, times(1)).schedule(any(Action0.class));
            verifyNoMoreInteractions(scheduler);

            verify(observer, times(1)).onNext(1);
            verify(observer, times(1)).onNext(2);
            verify(observer, times(1)).onNext(3);
            verify(observer, times(1)).onCompleted();
        }

    }

}