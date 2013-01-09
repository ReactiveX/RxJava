package org.rx.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.rx.reactive.AbstractIObservable;
import org.rx.reactive.IObservable;
import org.rx.reactive.IDisposable;
import org.rx.reactive.IObserver;


/**
 * Skips a specified number of contiguous values from the start of a watchable sequence and then returns the remaining values.
 * 
 * @param <T>
 */
/* package */final class OperationSkip {

    /**
     * Skips a specified number of contiguous values from the start of a watchable sequence and then returns the remaining values.
     * 
     * @param items
     * @param num
     * @return
     * 
     * @see http://msdn.microsoft.com/en-us/library/hh229847(v=vs.103).aspx
     */
    public static <T> IObservable<T> skip(final IObservable<T> items, final int num) {
        // wrap in a Watchable so that if a chain is built up, then asynchronously subscribed to twice we will have 2 instances of Take<T> rather than 1 handing both, which is not thread-safe.
        return new AbstractIObservable<T>() {

            @Override
            public IDisposable subscribe(IObserver<T> actualWatcher) {
                final AtomicWatchableSubscription subscription = new AtomicWatchableSubscription();
                // wrap in AtomicWatcher so that onNext calls are not interleaved but received
                // in the order they are called
                subscription.setActual(new Skip<T>(items, num).subscribe(new AtomicWatcher<T>(actualWatcher, subscription)));
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
    private static class Skip<T> extends AbstractIObservable<T> {
        private final int num;
        private final IObservable<T> items;
        private AtomicWatcher<T> atomicWatcher;
        private AtomicWatchableSubscription subscription = new AtomicWatchableSubscription();;

        Skip(final IObservable<T> items, final int num) {
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
                // skip them until we reach the 'num' value
                if (counter.incrementAndGet() > num) {
                    atomicWatcher.onNext(args);
                }
            }

        }

    }

    public static class UnitTest {

        @Test
        public void testSkip1() {
            IObservable<String> w = WatchableExtensions.toWatchable("one", "two", "three");
            IObservable<String> skip = skip(w, 2);

            @SuppressWarnings("unchecked")
            IObserver<String> aWatcher = mock(IObserver.class);
            skip.subscribe(aWatcher);
            verify(aWatcher, never()).onNext("one");
            verify(aWatcher, never()).onNext("two");
            verify(aWatcher, times(1)).onNext("three");
            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
        }

        @Test
        public void testSkip2() {
            IObservable<String> w = WatchableExtensions.toWatchable("one", "two", "three");
            IObservable<String> skip = skip(w, 1);

            @SuppressWarnings("unchecked")
            IObserver<String> aWatcher = mock(IObserver.class);
            skip.subscribe(aWatcher);
            verify(aWatcher, never()).onNext("one");
            verify(aWatcher, times(1)).onNext("two");
            verify(aWatcher, times(1)).onNext("three");
            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
        }

    }

}