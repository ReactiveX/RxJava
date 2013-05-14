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

import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

/**
 * Returns a specified number of contiguous elements from the end of an observable sequence.
 */
public final class OperationTakeLast {

    public static <T> Func1<Observer<T>, Subscription> takeLast(final Observable<T> items, final int count) {
        return new Func1<Observer<T>, Subscription>() {

            @Override
            public Subscription call(Observer<T> observer) {
                return new TakeLast<T>(items, count).call(observer);
            }

        };
    }

    private static class TakeLast<T> implements Func1<Observer<T>, Subscription> {
        private final int count;
        private final Observable<T> items;
        private final AtomicObservableSubscription subscription = new AtomicObservableSubscription();

        TakeLast(final Observable<T> items, final int count) {
            this.count = count;
            this.items = items;
        }

        public Subscription call(Observer<T> observer) {
            return subscription.wrap(items.subscribe(new ItemObserver(observer)));
        }

        private class ItemObserver implements Observer<T> {

            private LinkedBlockingDeque<T> deque = new LinkedBlockingDeque<T>(count);
            private final Observer<T> observer;

            public ItemObserver(Observer<T> observer) {
                this.observer = observer;
            }

            @Override
            public void onCompleted() {
                Iterator<T> reverse = deque.descendingIterator();
                while (reverse.hasNext()) {
                    observer.onNext(reverse.next());
                }
                observer.onCompleted();
            }

            @Override
            public void onError(Exception e) {
                observer.onError(e);
            }

            @Override
            public void onNext(T args) {
                while (!deque.offerFirst(args)) {
                    deque.removeLast();
                }
            }

        }

    }

    public static class UnitTest {

        @Test
        public void testTakeLastEmpty() {
            Observable<String> w = Observable.from();
            Observable<String> take = Observable.create(takeLast(w, 2));

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            take.subscribe(aObserver);
            verify(aObserver, never()).onNext(any(String.class));
            verify(aObserver, never()).onError(any(Exception.class));
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
            verify(aObserver, never()).onError(any(Exception.class));
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
            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

    }

}
