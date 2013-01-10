package org.rx.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Test;
import org.rx.reactive.AbstractIObservable;
import org.rx.reactive.IDisposable;
import org.rx.reactive.IObservable;
import org.rx.reactive.IObserver;

final class OperationToWatchableList<T> extends AbstractIObservable<List<T>> {
    private final IObservable<T> that;
    final ConcurrentLinkedQueue<T> list = new ConcurrentLinkedQueue<T>();

    OperationToWatchableList(IObservable<T> that) {
        this.that = that;
    }

    public IDisposable subscribe(IObserver<List<T>> listObserver) {
        final AtomicWatchableSubscription subscription = new AtomicWatchableSubscription();
        final IObserver<List<T>> watcher = new AtomicWatcher<List<T>>(listObserver, subscription);

        subscription.setActual(that.subscribe(new IObserver<T>() {
            public void onNext(T value) {
                // onNext can be concurrently executed so list must be thread-safe
                list.add(value);
            }

            public void onError(Exception ex) {
                watcher.onError(ex);
            }

            public void onCompleted() {
                try {
                    // copy from LinkedQueue to List since ConcurrentLinkedQueue does not implement the List interface
                    ArrayList<T> l = new ArrayList<T>(list.size());
                    for (T t : list) {
                        l.add(t);
                    }

                    // benjchristensen => I want to make this immutable but some clients are sorting this
                    // instead of using toSortedList() and this change breaks them until we migrate their code.
                    // watcher.onNext(Collections.unmodifiableList(l));
                    watcher.onNext(l);
                    watcher.onCompleted();
                } catch (Exception e) {
                    onError(e);
                }

            }
        }));
        return subscription;
    }

    public static class UnitTest {

        @Test
        public void testList() {
            IObservable<String> w = WatchableExtensions.toWatchable("one", "two", "three");
            IObservable<List<String>> watchable = new OperationToWatchableList<String>(w);

            @SuppressWarnings("unchecked")
            IObserver<List<String>> aWatcher = mock(IObserver.class);
            watchable.subscribe(aWatcher);
            verify(aWatcher, times(1)).onNext(Arrays.asList("one", "two", "three"));
            verify(aWatcher, never()).onError(any(Exception.class));
            verify(aWatcher, times(1)).onCompleted();
        }
    }
}