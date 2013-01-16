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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.observables.Observable;
import rx.observables.Observer;
import rx.observables.Subscription;

/**
 * Skips a specified number of contiguous values from the start of a Observable sequence and then returns the remaining values.
 * 
 * @param <T>
 */
public final class OperationSkip {

    /**
     * Skips a specified number of contiguous values from the start of a Observable sequence and then returns the remaining values.
     * 
     * @param items
     * @param num
     * @return
     * 
     * @see http://msdn.microsoft.com/en-us/library/hh229847(v=vs.103).aspx
     */
    public static <T> Observable<T> skip(final Observable<T> items, final int num) {
        // wrap in a Observable so that if a chain is built up, then asynchronously subscribed to twice we will have 2 instances of Take<T> rather than 1 handing both, which is not thread-safe.
        return new Observable<T>() {

            @Override
            public Subscription subscribe(Observer<T> actualObserver) {
                final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
                // wrap in AtomicObserver so that onNext calls are not interleaved but received
                // in the order they are called
                subscription.setActual(new Skip<T>(items, num).subscribe(new AtomicObserver<T>(actualObserver, subscription)));
                return subscription;
            }

        };
    }

    /**
     * This class is NOT thread-safe if invoked and referenced multiple times. In other words, don't subscribe to it multiple times from different threads.
     * <p>
     * It IS thread-safe from within it while receiving onNext events from multiple threads.
     * 
     * @param <T>
     */
    private static class Skip<T> extends Observable<T> {
        private final int num;
        private final Observable<T> items;
        private AtomicObserver<T> atomicObserver;
        private AtomicObservableSubscription subscription = new AtomicObservableSubscription();;

        Skip(final Observable<T> items, final int num) {
            this.num = num;
            this.items = items;
        }

        public Subscription subscribe(Observer<T> actualObserver) {
            atomicObserver = new AtomicObserver<T>(actualObserver, subscription);
            subscription.setActual(items.subscribe(new ItemObserver()));
            return subscription;
        }

        /**
         * Used to subscribe to the 'items' Observable sequence and forward to the actualObserver up to 'num' count.
         */
        private class ItemObserver implements Observer<T> {

            private AtomicInteger counter = new AtomicInteger();

            public ItemObserver() {
            }

            @Override
            public void onCompleted() {
                atomicObserver.onCompleted();
            }

            @Override
            public void onError(Exception e) {
                atomicObserver.onError(e);
            }

            @Override
            public void onNext(T args) {
                // skip them until we reach the 'num' value
                if (counter.incrementAndGet() > num) {
                    atomicObserver.onNext(args);
                }
            }

        }

    }

    public static class UnitTest {

        @Test
        public void testSkip1() {
            Observable<String> w = Observable.toObservable("one", "two", "three");
            Observable<String> skip = skip(w, 2);

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            skip.subscribe(aObserver);
            verify(aObserver, never()).onNext("one");
            verify(aObserver, never()).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testSkip2() {
            Observable<String> w = Observable.toObservable("one", "two", "three");
            Observable<String> skip = skip(w, 1);

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            skip.subscribe(aObserver);
            verify(aObserver, never()).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

    }

}