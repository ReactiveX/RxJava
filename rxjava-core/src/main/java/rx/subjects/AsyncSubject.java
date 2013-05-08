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
package rx.subjects;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observer;
import rx.Subscription;
import rx.util.AtomicObservableSubscription;
import rx.util.functions.Action1;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

/**
 * Subject that publishes only the last event to each {@link Observer} that has subscribed when the completes. 
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code
 
  // observer will receive no onNext events.
  AsyncSubject<Object> subject = AsyncSubject.create();
  subject.subscribe(observer);
  subject.onNext("one");
  subject.onNext("two");
  subject.onNext("three");
 
  // observer will receive "three" as the only onNext event.
  AsyncSubject<Object> subject = AsyncSubject.create();
  subject.subscribe(observer);
  subject.onNext("one");
  subject.onNext("two");
  subject.onNext("three");
  subject.onCompleted();
 
  } </pre>
 * 
 * @param <T>
 */
public class AsyncSubject<T> extends Subject<T, T> {
	
    public static <T> AsyncSubject<T> create() {
        final ConcurrentHashMap<Subscription, Observer<T>> observers = new ConcurrentHashMap<Subscription, Observer<T>>();

        Func1<Observer<T>, Subscription> onSubscribe = new Func1<Observer<T>, Subscription>() {
            @Override
            public Subscription call(Observer<T> observer) {
                final AtomicObservableSubscription subscription = new AtomicObservableSubscription();

                subscription.wrap(new Subscription() {
                    @Override
                    public void unsubscribe() {
                        // on unsubscribe remove it from the map of outbound observers to notify
                        observers.remove(subscription);
                    }
                });

                // on subscribe add it to the map of outbound observers to notify
                observers.put(subscription, observer);
                return subscription;
            }
        };

        return new AsyncSubject<T>(onSubscribe, observers);
    }

    private final ConcurrentHashMap<Subscription, Observer<T>> observers;
    private final AtomicReference<T> currentValue;

    protected AsyncSubject(Func1<Observer<T>, Subscription> onSubscribe, ConcurrentHashMap<Subscription, Observer<T>> observers) {
        super(onSubscribe);
        this.observers = observers;
        this.currentValue = new AtomicReference<T>();
    }

    @Override
    public void onCompleted() {
    	T finalValue = currentValue.get();
    	for (Observer<T> observer : observers.values()) {
			observer.onNext(finalValue);
    	}
        for (Observer<T> observer : observers.values()) {
            observer.onCompleted();
        }
    }

    @Override
    public void onError(Exception e) {
        for (Observer<T> observer : observers.values()) {
            observer.onError(e);
        }
    }

    @Override
    public void onNext(T args) {
        currentValue.set(args);
    }

    public static class UnitTest {

        private final Exception testException = new Exception();

        @Test
        public void testNeverCompleted() {
        	AsyncSubject<Object> subject = AsyncSubject.create();

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");
            subject.onNext("three");

            assertNeverCompletedObserver(aObserver);
        }

        private void assertNeverCompletedObserver(Observer<String> aObserver)
        {
            verify(aObserver, Mockito.never()).onNext(anyString());
            verify(aObserver, Mockito.never()).onError(testException);
            verify(aObserver, Mockito.never()).onCompleted();
        }
        
        @Test
        public void testCompleted() {
        	AsyncSubject<Object> subject = AsyncSubject.create();

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");
            subject.onNext("three");
            subject.onCompleted();

            assertCompletedObserver(aObserver);
        }

        private void assertCompletedObserver(Observer<String> aObserver)
        {
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testError() {
        	AsyncSubject<Object> subject = AsyncSubject.create();

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");
            subject.onNext("three");
            subject.onError(testException);
            subject.onNext("four");
            subject.onError(new Exception());
            subject.onCompleted();

            assertErrorObserver(aObserver);
        }

        private void assertErrorObserver(Observer<String> aObserver)
        {
            verify(aObserver, Mockito.never()).onNext(anyString());
            verify(aObserver, times(1)).onError(testException);
            verify(aObserver, Mockito.never()).onCompleted();
        }

        @Test
        public void testUnsubscribeBeforeCompleted() {
        	AsyncSubject<Object> subject = AsyncSubject.create();

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            Subscription subscription = subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");

            subscription.unsubscribe();
            assertNoOnNextEventsReceived(aObserver);

            subject.onNext("three");
            subject.onCompleted();

            assertNoOnNextEventsReceived(aObserver);
        }

        private void assertNoOnNextEventsReceived(Observer<String> aObserver)
        {
            verify(aObserver, Mockito.never()).onNext(anyString());
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, Mockito.never()).onCompleted();
        }

        @Test
        public void testUnsubscribe()
        {
            UnsubscribeTester.test(new Func0<AsyncSubject<Object>>()
            {
                @Override
                public AsyncSubject<Object> call()
                {
                    return AsyncSubject.create();
                }
            }, new Action1<AsyncSubject<Object>>()
            {
                @Override
                public void call(AsyncSubject<Object> DefaultSubject)
                {
                    DefaultSubject.onCompleted();
                }
            }, new Action1<AsyncSubject<Object>>()
            {
                @Override
                public void call(AsyncSubject<Object> DefaultSubject)
                {
                    DefaultSubject.onError(new Exception());
                }
            }, 
            null);
        }
    }
}
