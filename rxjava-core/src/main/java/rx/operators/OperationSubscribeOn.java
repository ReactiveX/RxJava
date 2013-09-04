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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.util.functions.Action0;
import rx.util.functions.Func2;

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
        public Subscription call(final Observer<? super T> observer) {
            return scheduler.schedule(null, new Func2<Scheduler, T, Subscription>() {
                @Override
                public Subscription call(Scheduler s, T t) {
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
            Observable<Integer> w = Observable.from(1, 2, 3);

            Scheduler scheduler = spy(OperatorTester.UnitTest.forwardingScheduler(Schedulers.immediate()));

            Observer<Integer> observer = mock(Observer.class);
            Subscription subscription = Observable.create(subscribeOn(w, scheduler)).subscribe(observer);

            verify(scheduler, times(1)).schedule(isNull(), any(Func2.class));
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
