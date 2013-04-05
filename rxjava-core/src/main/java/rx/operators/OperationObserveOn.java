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
import rx.util.functions.Func1;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OperationObserveOn {

    public static <T> Func1<Observer<T>, Subscription> observeOn(Observable<T> source, Scheduler scheduler) {
        return new ObserveOn<T>(source, scheduler);
    }

    private static class ObserveOn<T> implements Func1<Observer<T>, Subscription> {
        private final Observable<T> source;
        private final Scheduler scheduler;

        public ObserveOn(Observable<T> source, Scheduler scheduler) {
            this.source = source;
            this.scheduler = scheduler;
        }

        @Override
        public Subscription call(final Observer<T> observer) {
            return source.subscribe(new ScheduledObserver<T>(observer, scheduler));
        }
    }

    public static class UnitTest {

        @Test
        @SuppressWarnings("unchecked")
        public void testObserveOn() {

            Scheduler scheduler = spy(Schedulers.forwardingScheduler(Schedulers.immediate()));

            Observer<Integer> observer = mock(Observer.class);
            Observable.create(observeOn(Observable.toObservable(1, 2, 3), scheduler)).subscribe(observer);

            verify(scheduler, times(4)).schedule(any(Action0.class));
            verifyNoMoreInteractions(scheduler);

            verify(observer, times(1)).onNext(1);
            verify(observer, times(1)).onNext(2);
            verify(observer, times(1)).onNext(3);
            verify(observer, times(1)).onCompleted();
        }

    }

}
