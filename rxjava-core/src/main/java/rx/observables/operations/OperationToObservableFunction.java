/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.observables.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.Mockito;

import rx.observables.Observable;
import rx.observables.Observer;
import rx.observables.Subscription;
import rx.util.Func1;

/**
 * Accepts a Function and makes it into a Observable.
 * <p>
 * This is equivalent to Rx Observable.Create
 * 
 * @see http://msdn.microsoft.com/en-us/library/hh229114(v=vs.103).aspx
 * @see Observable.toObservable
 * @see Observable.create
 */
public final class OperationToObservableFunction<T> {

    public static <T> Observable<T> toObservableFunction(Func1<Subscription, Observer<T>> func) {
        return new ToObservableFunction<T>(func);
    }

    private static class ToObservableFunction<T> extends Observable<T> {
        private final Func1<Subscription, Observer<T>> func;

        public ToObservableFunction(Func1<Subscription, Observer<T>> func) {
            this.func = func;
        }

        @Override
        public Subscription subscribe(Observer<T> Observer) {
            final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
            // We specifically use the SingleThreaded AtomicObserver since we can't ensure the implementation is thread-safe
            // so will not allow it to use the MultiThreaded version even when other operators are doing so
            final Observer<T> atomicObserver = new AtomicObserverSingleThreaded<T>(Observer, subscription);
            // if func.call is synchronous, then the subscription won't matter as it can't ever be called
            // if func.call is asynchronous, then the subscription will get set and can be unsubscribed from
            subscription.setActual(func.call(atomicObserver));

            return subscription;
        }
    }

    public static class UnitTest {

        @Test
        public void testCreate() {

            Observable<String> observable = toObservableFunction(new Func1<Subscription, Observer<String>>() {

                @Override
                public Subscription call(Observer<String> Observer) {
                    Observer.onNext("one");
                    Observer.onNext("two");
                    Observer.onNext("three");
                    Observer.onCompleted();
                    return Observable.noOpSubscription();
                }

            });

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }
    }
}