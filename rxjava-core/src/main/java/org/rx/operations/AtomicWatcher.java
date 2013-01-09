package org.rx.operations;

import javax.annotation.concurrent.ThreadSafe;

import org.rx.reactive.IObserver;

/**
 * A thread-safe Watcher for transitioning states in operators.
 * <p>
 * Allows both single-threaded and multi-threaded execution controlled by the following FastProperty:
 * <li>reactive.watcher.multithreaded.enabled [Default: false]</li>
 * <p>
 * Single-threaded Execution rules are:
 * <ul>
 * <li>Allow only single-threaded, synchronous, ordered execution of onNext, onCompleted, onError</li>
 * <li>Once an onComplete or onError are performed, no further calls can be executed</li>
 * <li>If unsubscribe is called, this means we call completed() and don't allow any further onNext calls.</li>
 * </ul>
 * <p>
 * Multi-threaded Execution rules are:
 * <ul>
 * <li>Allows multiple threads to perform onNext concurrently</li>
 * <li>When an onComplete, onError or unsubscribe request is received, block until all current onNext calls are completed</li>
 * <li>When an unsubscribe is received, block until all current onNext are completed</li>
 * <li>Once an onComplete or onError are performed, no further calls can be executed</li>
 * <li>If unsubscribe is called, this means we call completed() and don't allow any further onNext calls.</li>
 * </ul>
 * 
 * @param <T>
 */
@ThreadSafe
/* package */class AtomicWatcher<T> implements IObserver<T> {

    /** Allow changing between forcing single or allowing multi-threaded execution of onNext */
    private static boolean allowMultiThreaded = true;
    static {
        String v = System.getProperty("rx.onNext.multithreaded.enabled");
        if (v != null) {
            // if we have a property set then we'll use it
            allowMultiThreaded = Boolean.parseBoolean(v);
        }
    }

    private final IObserver<T> watcher;

    public AtomicWatcher(IObserver<T> watcher, AtomicWatchableSubscription subscription) {
        if (allowMultiThreaded) {
            this.watcher = new AtomicWatcherMultiThreaded<T>(watcher, subscription);
        } else {
            this.watcher = new AtomicWatcherSingleThreaded<T>(watcher, subscription);
        }
    }

    @Override
    public void onCompleted() {
        watcher.onCompleted();
    }

    @Override
    public void onError(Exception e) {
        watcher.onError(e);
    }

    @Override
    public void onNext(T args) {
        watcher.onNext(args);
    }

}
