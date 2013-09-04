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

import static org.mockito.Mockito.*;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

/**
 * Returns an Observable that emits the items from the source Observable until another Observable
 * emits an item.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/takeUntil.png">
 */
public class OperationTakeUntil {

    /**
     * Returns the values from the source observable sequence until the other observable sequence produces a value.
     * 
     * @param source
     *            the source sequence to propagate elements for.
     * @param other
     *            the observable sequence that terminates propagation of elements of the source sequence.
     * @param <T>
     *            the type of source.
     * @param <E>
     *            the other type.
     * @return An observable sequence containing the elements of the source sequence up to the point the other sequence interrupted further propagation.
     */
    public static <T, E> Observable<T> takeUntil(final Observable<? extends T> source, final Observable<? extends E> other) {
        Observable<Notification<T>> s = Observable.create(new SourceObservable<T>(source));
        Observable<Notification<T>> o = Observable.create(new OtherObservable<T, E>(other));

        @SuppressWarnings("unchecked")
        /**
         * In JDK 7 we could use 'varargs' instead of 'unchecked'.
         * See http://stackoverflow.com/questions/1445233/is-it-possible-to-solve-the-a-generic-array-of-t-is-created-for-a-varargs-param
         * and http://hg.openjdk.java.net/jdk7/tl/langtools/rev/46cf751559ae 
         */
        Observable<Notification<T>> result = Observable.merge(s, o);

        return result.takeWhile(new Func1<Notification<T>, Boolean>() {
            @Override
            public Boolean call(Notification<T> notification) {
                return !notification.halt;
            }
        }).map(new Func1<Notification<T>, T>() {
            @Override
            public T call(Notification<T> notification) {
                return notification.value;
            }
        });
    }

    private static class Notification<T> {
        private final boolean halt;
        private final T value;

        public static <T> Notification<T> value(T value) {
            return new Notification<T>(false, value);
        }

        public static <T> Notification<T> halt() {
            return new Notification<T>(true, null);
        }

        private Notification(boolean halt, T value) {
            this.halt = halt;
            this.value = value;
        }

    }

    private static class SourceObservable<T> implements OnSubscribeFunc<Notification<T>> {
        private final Observable<? extends T> sequence;

        private SourceObservable(Observable<? extends T> sequence) {
            this.sequence = sequence;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super Notification<T>> notificationObserver) {
            return sequence.subscribe(new Observer<T>() {
                @Override
                public void onCompleted() {
                    notificationObserver.onNext(Notification.<T> halt());
                }

                @Override
                public void onError(Throwable e) {
                    notificationObserver.onError(e);
                }

                @Override
                public void onNext(T args) {
                    notificationObserver.onNext(Notification.value(args));
                }
            });
        }
    }

    private static class OtherObservable<T, E> implements OnSubscribeFunc<Notification<T>> {
        private final Observable<? extends E> sequence;

        private OtherObservable(Observable<? extends E> sequence) {
            this.sequence = sequence;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super Notification<T>> notificationObserver) {
            return sequence.subscribe(new Observer<E>() {
                @Override
                public void onCompleted() {
                    // Ignore
                }

                @Override
                public void onError(Throwable e) {
                    notificationObserver.onError(e);
                }

                @Override
                public void onNext(E args) {
                    notificationObserver.onNext(Notification.<T> halt());
                }
            });
        }
    }

    public static class UnitTest {

        @Test
        @SuppressWarnings("unchecked")
        public void testTakeUntil() {
            Subscription sSource = mock(Subscription.class);
            Subscription sOther = mock(Subscription.class);
            TestObservable source = new TestObservable(sSource);
            TestObservable other = new TestObservable(sOther);

            Observer<String> result = mock(Observer.class);
            Observable<String> stringObservable = takeUntil(source, other);
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
        @SuppressWarnings("unchecked")
        public void testTakeUntilSourceCompleted() {
            Subscription sSource = mock(Subscription.class);
            Subscription sOther = mock(Subscription.class);
            TestObservable source = new TestObservable(sSource);
            TestObservable other = new TestObservable(sOther);

            Observer<String> result = mock(Observer.class);
            Observable<String> stringObservable = takeUntil(source, other);
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
        @SuppressWarnings("unchecked")
        public void testTakeUntilSourceError() {
            Subscription sSource = mock(Subscription.class);
            Subscription sOther = mock(Subscription.class);
            TestObservable source = new TestObservable(sSource);
            TestObservable other = new TestObservable(sOther);
            Throwable error = new Throwable();

            Observer<String> result = mock(Observer.class);
            Observable<String> stringObservable = takeUntil(source, other);
            stringObservable.subscribe(result);
            source.sendOnNext("one");
            source.sendOnNext("two");
            source.sendOnError(error);

            verify(result, times(1)).onNext("one");
            verify(result, times(1)).onNext("two");
            verify(result, times(1)).onError(error);
            verify(sSource, times(1)).unsubscribe();
            verify(sOther, times(1)).unsubscribe();

        }

        @Test
        @SuppressWarnings("unchecked")
        public void testTakeUntilOtherError() {
            Subscription sSource = mock(Subscription.class);
            Subscription sOther = mock(Subscription.class);
            TestObservable source = new TestObservable(sSource);
            TestObservable other = new TestObservable(sOther);
            Throwable error = new Throwable();

            Observer<String> result = mock(Observer.class);
            Observable<String> stringObservable = takeUntil(source, other);
            stringObservable.subscribe(result);
            source.sendOnNext("one");
            source.sendOnNext("two");
            other.sendOnError(error);

            verify(result, times(1)).onNext("one");
            verify(result, times(1)).onNext("two");
            verify(result, times(1)).onError(error);
            verify(result, times(0)).onCompleted();
            verify(sSource, times(1)).unsubscribe();
            verify(sOther, times(1)).unsubscribe();

        }

        @Test
        @SuppressWarnings("unchecked")
        public void testTakeUntilOtherCompleted() {
            Subscription sSource = mock(Subscription.class);
            Subscription sOther = mock(Subscription.class);
            TestObservable source = new TestObservable(sSource);
            TestObservable other = new TestObservable(sOther);

            Observer<String> result = mock(Observer.class);
            Observable<String> stringObservable = takeUntil(source, other);
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

            Observer<? super String> observer = null;
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
            public void sendOnError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public Subscription subscribe(final Observer<? super String> observer) {
                this.observer = observer;
                return s;
            }
        }

    }
}
