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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;

/**
 * Returns an Observable that emits the last <code>count</code> items emitted by the source
 * Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/last.png">
 */
public final class OperationTakeLast {

    public static <T> OnSubscribeFunc<T> takeLast(final Observable<? extends T> items, final int count) {
        return new OnSubscribeFunc<T>() {

            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return new TakeLast<T>(items, count).onSubscribe(observer);
            }

        };
    }

    private static class TakeLast<T> implements OnSubscribeFunc<T> {
        private final int count;
        private final Observable<? extends T> items;
        private final SafeObservableSubscription subscription = new SafeObservableSubscription();

        TakeLast(final Observable<? extends T> items, final int count) {
            this.count = count;
            this.items = items;
        }

        public Subscription onSubscribe(Observer<? super T> observer) {
            if (count < 0) {
                throw new IndexOutOfBoundsException(
                        "count could not be negative");
            }
            return subscription.wrap(items.subscribe(new ItemObserver(observer)));
        }

        private class ItemObserver implements Observer<T> {

            /**
             * Store the last count elements until now.
             */
            private Deque<T> deque = new LinkedList<T>();
            private final Observer<? super T> observer;
            private final ReentrantLock lock = new ReentrantLock();

            public ItemObserver(Observer<? super T> observer) {
                this.observer = observer;
            }

            @Override
            public void onCompleted() {
                try {
                    for (T value : deque) {
                        observer.onNext(value);
                    }
                    observer.onCompleted();
                } catch (Throwable e) {
                    observer.onError(e);
                }
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public void onNext(T value) {
                if (count == 0) {
                    // If count == 0, we do not need to put value into deque and
                    // remove it at once. We can ignore the value directly.
                    return;
                }
                lock.lock();
                try {
                    deque.offerLast(value);
                    if (deque.size() > count) {
                        // Now deque has count + 1 elements, so the first
                        // element in the deque definitely does not belong
                        // to the last count elements of the source
                        // sequence. We can drop it now.
                        deque.removeFirst();
                    }
                } catch (Throwable e) {
                    observer.onError(e);
                    subscription.unsubscribe();
                } finally {
                    lock.unlock();
                }
            }

        }

    }

    public static class UnitTest {

        @Test
        public void testTakeLastEmpty() {
            Observable<String> w = Observable.empty();
            Observable<String> take = Observable.create(takeLast(w, 2));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            take.subscribe(aObserver);
            verify(aObserver, never()).onNext(any(String.class));
            verify(aObserver, never()).onError(any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testTakeLast1() {
            Observable<String> w = Observable.from("one", "two", "three");
            Observable<String> take = Observable.create(takeLast(w, 2));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            InOrder inOrder = inOrder(aObserver);
            take.subscribe(aObserver);
            inOrder.verify(aObserver, times(1)).onNext("two");
            inOrder.verify(aObserver, times(1)).onNext("three");
            verify(aObserver, never()).onNext("one");
            verify(aObserver, never()).onError(any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testTakeLast2() {
            Observable<String> w = Observable.from("one");
            Observable<String> take = Observable.create(takeLast(w, 10));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            take.subscribe(aObserver);
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, never()).onError(any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testTakeLastWithZeroCount() {
            Observable<String> w = Observable.from("one");
            Observable<String> take = Observable.create(takeLast(w, 0));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            take.subscribe(aObserver);
            verify(aObserver, never()).onNext("one");
            verify(aObserver, never()).onError(any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testTakeLastWithNull() {
            Observable<String> w = Observable.from("one", null, "three");
            Observable<String> take = Observable.create(takeLast(w, 2));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            take.subscribe(aObserver);
            verify(aObserver, never()).onNext("one");
            verify(aObserver, times(1)).onNext(null);
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, never()).onError(any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testTakeLastWithNegativeCount() {
            Observable<String> w = Observable.from("one");
            Observable<String> take = Observable.create(takeLast(w, -1));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            take.subscribe(aObserver);
            verify(aObserver, never()).onNext("one");
            verify(aObserver, times(1)).onError(
                    any(IndexOutOfBoundsException.class));
            verify(aObserver, never()).onCompleted();
        }

    }

}
