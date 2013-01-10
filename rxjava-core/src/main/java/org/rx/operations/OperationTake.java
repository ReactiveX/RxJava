package org.rx.operations;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.rx.reactive.Observable;
import org.rx.reactive.Observer;
import org.rx.reactive.Subscription;

/**
 * Returns a specified number of contiguous values from the start of an observable sequence.
 * 
 * @param <T>
 */
public final class OperationTake {

    /**
     * Returns a specified number of contiguous values from the start of an observable sequence.
     * 
     * @param items
     * @param num
     * @return
     */
    public static <T> Observable<T> take(final Observable<T> items, final int num) {
        // wrap in a Watchbable so that if a chain is built up, then asynchronously subscribed to twice we will have 2 instances of Take<T> rather than 1 handing both, which is not thread-safe.
        return new Observable<T>() {

            @Override
            public Subscription subscribe(Observer<T> actualObserver) {
                final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
                // wrap in AtomicObserverSingleThreaded so that onNext calls are not interleaved but received
                // in the order they are called
                subscription.setActual(new Take<T>(items, num).subscribe(new AtomicObserver<T>(actualObserver, subscription)));
                return subscription;
            }

        };
    }

    /**
     * This class is NOT thread-safe if invoked and referenced multiple times. In other words, don't subscribe to it multiple times from different threads.
     * <p>
     * It IS thread-safe from within it while receiving onNext events from multiple threads.
     * <p>
     * This should all be fine as long as it's kept as a private class and a new instance created from static factory method above.
     * <p>
     * Note how the take() factory method above protects us from a single instance being exposed with the Observable wrapper handling the subscribe flow.
     * 
     * @param <T>
     */
    private static class Take<T> extends Observable<T> {
        private final int num;
        private final Observable<T> items;
        private AtomicObserver<T> atomicObserver;
        private AtomicObservableSubscription subscription = new AtomicObservableSubscription();;

        Take(final Observable<T> items, final int num) {
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
                if (counter.getAndIncrement() < num) {
                    atomicObserver.onNext(args);
                } else {
                    // we are done so let's unsubscribe
                    atomicObserver.onCompleted();
                    subscription.unsubscribe();
                }
            }

        }

    }

    public static class UnitTest {

        @Test
        public void testTake1() {
            Observable<String> w = Observable.toObservable("one", "two", "three");
            Observable<String> take = take(w, 2);

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            take.subscribe(aObserver);
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, never()).onNext("three");
            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testTake2() {
            Observable<String> w = Observable.toObservable("one", "two", "three");
            Observable<String> take = take(w, 1);

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            take.subscribe(aObserver);
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, never()).onNext("two");
            verify(aObserver, never()).onNext("three");
            verify(aObserver, never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testUnsubscribeAfterTake() {
            Subscription s = mock(Subscription.class);
            TestObservable w = new TestObservable(s, "one", "two", "three");

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            Observable<String> take = take(w, 1);
            take.subscribe(aObserver);

            // wait for the Observable to complete
            try {
                w.t.join();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }

            System.out.println("TestObservable thread finished");
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, never()).onNext("two");
            verify(aObserver, never()).onNext("three");
            verify(s, times(1)).unsubscribe();
        }

        private static class TestObservable extends Observable<String> {

            final Subscription s;
            final String[] values;
            Thread t = null;

            public TestObservable(Subscription s, String... values) {
                this.s = s;
                this.values = values;
            }

            @Override
            public Subscription subscribe(final Observer<String> observer) {
                System.out.println("TestObservable subscribed to ...");
                t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            System.out.println("running TestObservable thread");
                            for (String s : values) {
                                System.out.println("TestObservable onNext: " + s);
                                observer.onNext(s);
                            }
                            observer.onCompleted();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                });
                System.out.println("starting TestObservable thread");
                t.start();
                System.out.println("done starting TestObservable thread");
                return s;
            }

        }
    }

}