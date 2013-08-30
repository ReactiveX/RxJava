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

import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

/**
 * Reverses the effect of {@link OperationMaterialize} by transforming the Notification objects
 * emitted by a source Observable into the items or notifications they represent.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/dematerialize.png">
 * <p>
 * See <a href="http://msdn.microsoft.com/en-us/library/hh229047(v=vs.103).aspx">here</a> for the
 * Microsoft Rx equivalent.
 */
public final class OperationDematerialize {

    /**
     * Dematerializes the explicit notification values of an observable sequence as implicit notifications.
     * 
     * @param sequence
     *            An observable sequence containing explicit notification values which have to be turned into implicit notifications.
     * @return An observable sequence exhibiting the behavior corresponding to the source sequence's notification values.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229047(v=vs.103).aspx">Observable.Dematerialize(TSource) Method </a>
     */
    public static <T> Func1<Observer<T>, Subscription> dematerialize(final Observable<Notification<T>> sequence) {
        return new DematerializeObservable<T>(sequence);
    }

    private static class DematerializeObservable<T> implements Func1<Observer<T>, Subscription> {

        private final Observable<Notification<T>> sequence;

        public DematerializeObservable(Observable<Notification<T>> sequence) {
            this.sequence = sequence;
        }

        @Override
        public Subscription call(final Observer<T> observer) {
            return sequence.subscribe(new Observer<Notification<T>>() {
                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                }

                @Override
                public void onNext(Notification<T> value) {
                    switch (value.getKind()) {
                    case OnNext:
                        observer.onNext(value.getValue());
                        break;
                    case OnError:
                        observer.onError(value.getThrowable());
                        break;
                    case OnCompleted:
                        observer.onCompleted();
                        break;
                    }
                }
            });
        }
    }

    public static class UnitTest {

        @Test
        @SuppressWarnings("unchecked")
        public void testDematerialize1() {
            Observable<Notification<Integer>> notifications = Observable.from(1, 2).materialize();
            Observable<Integer> dematerialize = notifications.dematerialize();

            Observer<Integer> aObserver = mock(Observer.class);
            dematerialize.subscribe(aObserver);

            verify(aObserver, times(1)).onNext(1);
            verify(aObserver, times(1)).onNext(2);
            verify(aObserver, times(1)).onCompleted();
            verify(aObserver, never()).onError(any(Throwable.class));
        }

        @Test
        @SuppressWarnings("unchecked")
        public void testDematerialize2() {
            Throwable exception = new Throwable("test");
            Observable<Integer> observable = Observable.error(exception);
            Observable<Integer> dematerialize = Observable.create(dematerialize(observable.materialize()));

            Observer<Integer> aObserver = mock(Observer.class);
            dematerialize.subscribe(aObserver);

            verify(aObserver, times(1)).onError(exception);
            verify(aObserver, times(0)).onCompleted();
            verify(aObserver, times(0)).onNext(any(Integer.class));
        }
        
        @Test
        @SuppressWarnings("unchecked")
        public void testDematerialize3() {
            Exception exception = new Exception("test");
            Observable<Integer> observable = Observable.error(exception);
            Observable<Integer> dematerialize = Observable.create(dematerialize(observable.materialize()));

            Observer<Integer> aObserver = mock(Observer.class);
            dematerialize.subscribe(aObserver);

            verify(aObserver, times(1)).onError(exception);
            verify(aObserver, times(0)).onCompleted();
            verify(aObserver, times(0)).onNext(any(Integer.class));
        }
    }
}
