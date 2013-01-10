package org.rx.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.junit.Test;
import org.rx.reactive.AbstractIObservable;
import org.rx.reactive.IDisposable;
import org.rx.reactive.IObservable;
import org.rx.reactive.IObserver;

/**
 * Accepts an Iterable object and exposes it as an Observable.
 * 
 * @param <T>
 *            The type of the Iterable sequence.
 */
/* package */class OperationToWatchableIterable<T> extends AbstractIObservable<T> {
    public OperationToWatchableIterable(Iterable<T> list) {
        this.iterable = list;
    }

    public Iterable<T> iterable;

    public IDisposable subscribe(IObserver<T> watcher) {
        final AtomicWatchableSubscription subscription = new AtomicWatchableSubscription(WatchableExtensions.noOpSubscription());
        final IObserver<T> observer = new AtomicWatcher<T>(watcher, subscription);

        for (T item : iterable) {
            observer.onNext(item);
        }
        observer.onCompleted();

        return subscription;
    }

    public static class UnitTest {

        @Test
        public void testIterable() {
            IObservable<String> watchable = new OperationToWatchableIterable<String>(Arrays.<String> asList("one", "two", "three"));

            @SuppressWarnings("unchecked")
            IObserver<String> aWatcher = mock(IObserver.class);
            watchable.subscribe(aWatcher);
            verify(aWatcher, times(1)).onNext("one");
            verify(aWatcher, times(1)).onNext("two");
            verify(aWatcher, times(1)).onNext("three");
            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
        }
    }
}