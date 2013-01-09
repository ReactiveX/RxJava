package org.rx.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.rx.functions.Func1;
import org.rx.reactive.AbstractIObservable;
import org.rx.reactive.IObservable;
import org.rx.reactive.IDisposable;
import org.rx.reactive.IObserver;


/**
 * Accepts a Function and makes it into a Watchable.
 * <p>
 * This is equivalent to Rx Observable.Create
 * 
 * @see http://msdn.microsoft.com/en-us/library/hh229114(v=vs.103).aspx
 * @see WatchableExtensions.toWatchable
 * @see WatchableExtensions.create
 */
/* package */ class OperationToWatchableFunction<T> extends AbstractIObservable<T> {
    private final Func1<IDisposable, IObserver<T>> func;

    OperationToWatchableFunction(Func1<IDisposable, IObserver<T>> func) {
        this.func = func;
    }

    @Override
    public IDisposable subscribe(IObserver<T> watcher) {
        final AtomicWatchableSubscription subscription = new AtomicWatchableSubscription();
        // We specifically use the SingleThreaded AtomicWatcher since we can't ensure the implementation is thread-safe
        // so will not allow it to use the MultiThreaded version even when other operators are doing so
        final IObserver<T> atomicWatcher = new AtomicWatcherSingleThreaded<T>(watcher, subscription);
        // if func.call is synchronous, then the subscription won't matter as it can't ever be called
        // if func.call is asynchronous, then the subscription will get set and can be unsubscribed from
        subscription.setActual(func.call(atomicWatcher));

        return subscription;
    }

    public static class UnitTest {

        @Test
        public void testCreate() {

            IObservable<String> watchable = new OperationToWatchableFunction<String>(new Func1<IDisposable, IObserver<String>>() {

                @Override
                public IDisposable call(IObserver<String> watcher) {
                    watcher.onNext("one");
                    watcher.onNext("two");
                    watcher.onNext("three");
                    watcher.onCompleted();
                    return WatchableExtensions.noOpSubscription();
                }

            });

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