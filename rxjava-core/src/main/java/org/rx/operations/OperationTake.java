package org.rx.operations;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.rx.reactive.AbstractIObservable;
import org.rx.reactive.IObservable;
import org.rx.reactive.IDisposable;
import org.rx.reactive.IObserver;


/**
 * Returns a specified number of contiguous values from the start of an observable sequence.
 * 
 * @param <T>
 */
/* package */final class OperationTake {

    /**
     * Returns a specified number of contiguous values from the start of an observable sequence.
     * 
     * @param items
     * @param num
     * @return
     */
    public static <T> IObservable<T> take(final IObservable<T> items, final int num) {
        // wrap in a Watchbable so that if a chain is built up, then asynchronously subscribed to twice we will have 2 instances of Take<T> rather than 1 handing both, which is not thread-safe.
        return new AbstractIObservable<T>() {

            @Override
            public IDisposable subscribe(IObserver<T> actualWatcher) {
                final AtomicWatchableSubscription subscription = new AtomicWatchableSubscription();
                // wrap in AtomicWatcherSingleThreaded so that onNext calls are not interleaved but received
                // in the order they are called
                subscription.setActual(new Take<T>(items, num).subscribe(new AtomicWatcher<T>(actualWatcher, subscription)));
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
     * Note how the take() factory method above protects us from a single instance being exposed with the Watchable wrapper handling the subscribe flow.
     * 
     * @param <T>
     */
    private static class Take<T> extends AbstractIObservable<T> {
        private final int num;
        private final IObservable<T> items;
        private AtomicWatcher<T> atomicWatcher;
        private AtomicWatchableSubscription subscription = new AtomicWatchableSubscription();;

        Take(final IObservable<T> items, final int num) {
            this.num = num;
            this.items = items;
        }

        public IDisposable subscribe(IObserver<T> actualWatcher) {
            atomicWatcher = new AtomicWatcher<T>(actualWatcher, subscription);
            subscription.setActual(items.subscribe(new ItemWatcher()));
            return subscription;
        }

        /**
         * Used to subscribe to the 'items' Watchable sequence and forward to the actualWatcher up to 'num' count.
         */
        private class ItemWatcher implements IObserver<T> {

            private AtomicInteger counter = new AtomicInteger();

            public ItemWatcher() {
            }

            @Override
            public void onCompleted() {
                atomicWatcher.onCompleted();
            }

            @Override
            public void onError(Exception e) {
                atomicWatcher.onError(e);
            }

            @Override
            public void onNext(T args) {
                if (counter.getAndIncrement() < num) {
                    atomicWatcher.onNext(args);
                } else {
                    // we are done so let's unsubscribe
                    atomicWatcher.onCompleted();
                    subscription.unsubscribe();
                }
            }

        }

    }

    public static class UnitTest {

        @Test
        public void testTake1() {
            IObservable<String> w = WatchableExtensions.toWatchable("one", "two", "three");
            IObservable<String> take = take(w, 2);

            @SuppressWarnings("unchecked")
            IObserver<String> aWatcher = mock(IObserver.class);
            take.subscribe(aWatcher);
            verify(aWatcher, times(1)).onNext("one");
            verify(aWatcher, times(1)).onNext("two");
            verify(aWatcher, never()).onNext("three");
            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
        }

        @Test
        public void testTake2() {
            IObservable<String> w = WatchableExtensions.toWatchable("one", "two", "three");
            IObservable<String> take = take(w, 1);

            @SuppressWarnings("unchecked")
            IObserver<String> aWatcher = mock(IObserver.class);
            take.subscribe(aWatcher);
            verify(aWatcher, times(1)).onNext("one");
            verify(aWatcher, never()).onNext("two");
            verify(aWatcher, never()).onNext("three");
            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
        }

        @Test
        public void testUnsubscribeAfterTake() {
            IDisposable s = mock(IDisposable.class);
            TestWatchable w = new TestWatchable(s, "one", "two", "three");

            @SuppressWarnings("unchecked")
            IObserver<String> aWatcher = mock(IObserver.class);
            IObservable<String> take = take(w, 1);
            take.subscribe(aWatcher);

            // wait for the watchable to complete
            try {
                w.t.join();
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }

            System.out.println("TestWatchable thread finished");
            verify(aWatcher, times(1)).onNext("one");
            verify(aWatcher, never()).onNext("two");
            verify(aWatcher, never()).onNext("three");
            verify(s, times(1)).unsubscribe();
        }

        private static class TestWatchable extends AbstractIObservable<String> {

            final IDisposable s;
            final String[] values;
            Thread t = null;

            public TestWatchable(IDisposable s, String... values) {
                this.s = s;
                this.values = values;
            }

            @Override
            public IDisposable subscribe(final IObserver<String> observer) {
                System.out.println("TestWatchable subscribed to ...");
                t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            System.out.println("running TestWatchable thread");
                            for (String s : values) {
                                System.out.println("TestWatchable onNext: " + s);
                                observer.onNext(s);
                            }
                            observer.onCompleted();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                });
                System.out.println("starting TestWatchable thread");
                t.start();
                System.out.println("done starting TestWatchable thread");
                return s;
            }

        }
    }

}