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
import rx.operators.SafeObservableSubscription;
import rx.util.functions.Action1;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

/**
 * Subject that publishes the most recent and all subsequent events to each subscribed {@link Observer}.
 *
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

  // observer will receive all events.
  BehaviorSubject<Object> subject = BehaviorSubject.createWithDefaultValue("default");
  subject.subscribe(observer);
  subject.onNext("one");
  subject.onNext("two");
  subject.onNext("three");

  // observer will receive the "one", "two" and "three" events, but not "zero"
  BehaviorSubject<Object> subject = BehaviorSubject.createWithDefaultValue("default");
  subject.onNext("zero");
  subject.onNext("one");
  subject.subscribe(observer);
  subject.onNext("two");
  subject.onNext("three");

  } </pre>
 *
 * @param <T>
 */
public class BehaviorSubject<T> extends Subject<T, T> {

    /**
     * Creates a {@link BehaviorSubject} which publishes the last and all subsequent events to each
     * {@link Observer} that subscribes to it.
     *
     * @param defaultValue
     *            The value which will be published to any {@link Observer} as long as the
     *            {@link BehaviorSubject} has not yet received any events.
     * @return the constructed {@link BehaviorSubject}.
     */
    public static <T> BehaviorSubject<T> createWithDefaultValue(T defaultValue) {
        final ConcurrentHashMap<Subscription, Observer<? super T>> observers = new ConcurrentHashMap<Subscription, Observer<? super T>>();

        final AtomicReference<T> currentValue = new AtomicReference<T>(defaultValue);

        Func1<Observer<? super T>, Subscription> onSubscribe = new Func1<Observer<? super T>, Subscription>() {
            @Override
            public Subscription call(Observer<? super T> observer) {
                final SafeObservableSubscription subscription = new SafeObservableSubscription();

                subscription.wrap(new Subscription() {
                    @Override
                    public void unsubscribe() {
                        // on unsubscribe remove it from the map of outbound observers to notify
                        observers.remove(subscription);
                    }
                });

                observer.onNext(currentValue.get());

                // on subscribe add it to the map of outbound observers to notify
                observers.put(subscription, observer);
                return subscription;
            }
        };

        return new BehaviorSubject<T>(currentValue, onSubscribe, observers);
    }

    private final ConcurrentHashMap<Subscription, Observer<? super T>> observers;
    private final AtomicReference<T> currentValue;

    protected BehaviorSubject(AtomicReference<T> currentValue, Func1<? super Observer<? super T>, ? extends Subscription> onSubscribe, ConcurrentHashMap<Subscription, Observer<? super T>> observers) {
        super(onSubscribe);
        this.currentValue = currentValue;
        this.observers = observers;
    }

    @Override
    public void onCompleted() {
        for (Observer<? super T> observer : observers.values()) {
            observer.onCompleted();
        }
    }

    @Override
    public void onError(Throwable e) {
        for (Observer<? super T> observer : observers.values()) {
            observer.onError(e);
        }
    }

    @Override
    public void onNext(T args) {
        currentValue.set(args);
        for (Observer<? super T> observer : observers.values()) {
            observer.onNext(args);
        }
    }

    public static class UnitTest {

        private final Throwable testException = new Throwable();

        @Test
        public void testThatObserverReceivesDefaultValueIfNothingWasPublished() {
            BehaviorSubject<String> subject = BehaviorSubject.createWithDefaultValue("default");

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");
            subject.onNext("three");

            assertReceivedAllEvents(aObserver);
        }

        private void assertReceivedAllEvents(Observer<String> aObserver) {
            verify(aObserver, times(1)).onNext("default");
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, Mockito.never()).onError(testException);
            verify(aObserver, Mockito.never()).onCompleted();
        }

        @Test
        public void testThatObserverDoesNotReceiveDefaultValueIfSomethingWasPublished() {
            BehaviorSubject<String> subject = BehaviorSubject.createWithDefaultValue("default");

            subject.onNext("one");

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("two");
            subject.onNext("three");

            assertDidNotReceiveTheDefaultValue(aObserver);
        }

        private void assertDidNotReceiveTheDefaultValue(Observer<String> aObserver) {
            verify(aObserver, Mockito.never()).onNext("default");
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, Mockito.never()).onError(testException);
            verify(aObserver, Mockito.never()).onCompleted();
        }

        @Test
        public void testCompleted() {
            BehaviorSubject<String> subject = BehaviorSubject.createWithDefaultValue("default");

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onCompleted();

            assertCompletedObserver(aObserver);
        }

        private void assertCompletedObserver(Observer<String> aObserver)
        {
            verify(aObserver, times(1)).onNext("default");
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, Mockito.never()).onError(any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testCompletedAfterError() {
            BehaviorSubject<String> subject = BehaviorSubject.createWithDefaultValue("default");

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onError(testException);
            subject.onNext("two");
            subject.onCompleted();

            assertErrorObserver(aObserver);
        }

        private void assertErrorObserver(Observer<String> aObserver)
        {
            verify(aObserver, times(1)).onNext("default");
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onError(testException);
        }

        @Test
        public void testUnsubscribe()
        {
            UnsubscribeTester.test(new Func0<BehaviorSubject<String>>()
            {
                @Override
                public BehaviorSubject<String> call()
                {
                    return BehaviorSubject.createWithDefaultValue("default");
                }
            }, new Action1<BehaviorSubject<String>>()
            {
                @Override
                public void call(BehaviorSubject<String> DefaultSubject)
                {
                    DefaultSubject.onCompleted();
                }
            }, new Action1<BehaviorSubject<String>>()
            {
                @Override
                public void call(BehaviorSubject<String> DefaultSubject)
                {
                    DefaultSubject.onError(new Throwable());
                }
            }, new Action1<BehaviorSubject<String>>()
            {
                @Override
                public void call(BehaviorSubject<String> DefaultSubject)
                {
                    DefaultSubject.onNext("one");
                }
            });
        }
    }
}
