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
import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.AtomicObservableSubscription;
import rx.util.Pair;
import rx.util.functions.Func1;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class OperatorTakeUntil {

    /**
     * Returns the values from the source observable sequence until the other observable sequence produces a value.
     *
     * @param source the source sequence to propagate elements for.
     * @param other  the observable sequence that terminates propagation of elements of the source sequence.
     * @param <T>    the type of source.
     * @param <E>    the other type.
     * @return An observable sequence containing the elements of the source sequence up to the point the other sequence interrupted further propagation.
     */
    public static <T, E> Func1<Observer<T>, Subscription> takeUntil(final Observable<T> source, final Observable<E> other) {
        return new TakeUntil<T, E>(source, other);
    }

    private static class TakeUntil<T, E> implements Func1<Observer<T>, Subscription> {

        private final Observable<T> source;
        private final Observable<E> other;
        private final AtomicObservableSubscription subscription = new AtomicObservableSubscription();

        private TakeUntil(Observable<T> source, Observable<E> other) {
            this.source = source;
            this.other = other;
        }

        @Override
        public Subscription call(final Observer<T> observer) {
            Observable<Pair<Type, Notification<T>>> result = mergeWithIdentifier(source, other);

            return subscription.wrap(result.subscribe(new Observer<Pair<Type, Notification<T>>>() {
                @Override
                public void onCompleted() {
                    // ignore
                }

                @Override
                public void onError(Exception e) {
                    // ignore
                }

                @Override
                public void onNext(Pair<Type, Notification<T>> pair) {
                    Type type = pair.getFirst();
                    Notification<T> notification = pair.getSecond();

                    if (notification.isOnError()) {
                        observer.onError(notification.getException());
                    } else if (type == Type.SOURCE && notification.isOnNext()) {
                        observer.onNext(notification.getValue());
                    } else if ((type == Type.OTHER && notification.isOnNext()) || (type == Type.SOURCE && notification.isOnCompleted())) {
                        observer.onCompleted();
                    }

                }
            }));
        }

        @SuppressWarnings("unchecked")
        private static <T, E> Observable<Pair<Type, Notification<T>>> mergeWithIdentifier(Observable<T> source, Observable<E> other) {
            Observable<Pair<Type, Notification<T>>> s = source.materialize().map(new Func1<Notification<T>, Pair<Type, Notification<T>>>() {
                @Override
                public Pair<Type, Notification<T>> call(Notification<T> arg) {
                    return Pair.create(Type.SOURCE, arg);
                }
            });

            Observable<Pair<Type, Notification<T>>> o = other.materialize().map(new Func1<Notification<E>, Pair<Type, Notification<T>>>() {
                @Override
                public Pair<Type, Notification<T>> call(Notification<E> arg) {
                    Notification<T> n = null;
                    if (arg.isOnNext()) {
                        n = new Notification<T>((T)null);
                    }
                    if (arg.isOnCompleted()) {
                        n = new Notification<T>();
                    }
                    if (arg.isOnError()) {
                        n = new Notification<T>(arg.getException());
                    }
                    return Pair.create(Type.OTHER, n);
                }
            });

            return Observable.merge(s, o);
        }

        private static enum Type {
            SOURCE, OTHER
        }

    }


    public static class UnitTest {


        @Test
        public void testTakeUntil() {
            Subscription sSource = mock(Subscription.class);
            Subscription sOther = mock(Subscription.class);
            TestObservable source = new TestObservable(sSource);
            TestObservable other = new TestObservable(sOther);

            Observer<String> result = mock(Observer.class);
            Observable<String> stringObservable = Observable.create(new TakeUntil<String, String>(source, other));
            stringObservable.subscribe(result);
            source.sendOnNext("one");
            source.sendOnNext("two");
            other.sendOnNext("three");
            source.sendOnNext("four");
            source.sendOnCompleted();
            other.sendOnCompleted();

            verify(result, times(1)).onNext("one");
            verify(result, times(1)).onNext("two");
            verify(result, times(0)).onNext("three");
            verify(result, times(0)).onNext("four");
            verify(sSource, times(1)).unsubscribe();
            verify(sOther, times(1)).unsubscribe();

        }

        @Test
        public void testTakeUntilSourceCompleted() {
            Subscription sSource = mock(Subscription.class);
            Subscription sOther = mock(Subscription.class);
            TestObservable source = new TestObservable(sSource);
            TestObservable other = new TestObservable(sOther);

            Observer<String> result = mock(Observer.class);
            Observable<String> stringObservable = Observable.create(new TakeUntil<String, String>(source, other));
            stringObservable.subscribe(result);
            source.sendOnNext("one");
            source.sendOnNext("two");
            source.sendOnCompleted();

            verify(result, times(1)).onNext("one");
            verify(result, times(1)).onNext("two");
            verify(sSource, times(1)).unsubscribe();
            verify(sOther, times(1)).unsubscribe();

        }

        @Test
        public void testTakeUntilSourceError() {
            Subscription sSource = mock(Subscription.class);
            Subscription sOther = mock(Subscription.class);
            TestObservable source = new TestObservable(sSource);
            TestObservable other = new TestObservable(sOther);

            Observer<String> result = mock(Observer.class);
            Observable<String> stringObservable = Observable.create(new TakeUntil<String, String>(source, other));
            stringObservable.subscribe(result);
            source.sendOnNext("one");
            source.sendOnNext("two");
            source.sendOnError(new TestException());

            verify(result, times(1)).onNext("one");
            verify(result, times(1)).onNext("two");
            verify(result, times(1)).onError(any(TestException.class));
            verify(sSource, times(1)).unsubscribe();
            verify(sOther, times(1)).unsubscribe();

        }

        @Test
        public void testTakeUntilOtherError() {
            Subscription sSource = mock(Subscription.class);
            Subscription sOther = mock(Subscription.class);
            TestObservable source = new TestObservable(sSource);
            TestObservable other = new TestObservable(sOther);

            Observer<String> result = mock(Observer.class);
            Observable<String> stringObservable = Observable.create(new TakeUntil<String, String>(source, other));
            stringObservable.subscribe(result);
            source.sendOnNext("one");
            source.sendOnNext("two");
            other.sendOnError(new TestException());

            verify(result, times(1)).onNext("one");
            verify(result, times(1)).onNext("two");
            // ignore other exception
            verify(result, times(1)).onError(any(TestException.class));
            verify(result, times(0)).onCompleted();
            verify(sSource, times(1)).unsubscribe();
            verify(sOther, times(1)).unsubscribe();

        }

        @Test
        public void testTakeUntilOtherCompleted() {
            Subscription sSource = mock(Subscription.class);
            Subscription sOther = mock(Subscription.class);
            TestObservable source = new TestObservable(sSource);
            TestObservable other = new TestObservable(sOther);

            Observer<String> result = mock(Observer.class);
            Observable<String> stringObservable = Observable.create(new TakeUntil<String, String>(source, other));
            stringObservable.subscribe(result);
            source.sendOnNext("one");
            source.sendOnNext("two");
            other.sendOnCompleted();

            verify(result, times(1)).onNext("one");
            verify(result, times(1)).onNext("two");
            verify(result, times(0)).onCompleted();
            verify(sSource, times(0)).unsubscribe();
            verify(sOther, times(0)).unsubscribe();

        }

        private static class TestObservable extends Observable<String> {

            Observer<String> observer = null;
            Subscription s;

            public TestObservable(Subscription s) {
                this.s = s;
            }

            /* used to simulate subscription */
            public void sendOnCompleted() {
                observer.onCompleted();
            }

            /* used to simulate subscription */
            public void sendOnNext(String value) {
                observer.onNext(value);
            }

            /* used to simulate subscription */
            @SuppressWarnings("unused")
            public void sendOnError(Exception e) {
                observer.onError(e);
            }

            @Override
            public Subscription subscribe(final Observer<String> observer) {
                this.observer = observer;
                return s;
            }
        }

        private static class TestException extends RuntimeException {

        }

    }
}
