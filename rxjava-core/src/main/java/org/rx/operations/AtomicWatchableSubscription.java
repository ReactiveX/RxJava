package org.rx.operations;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.concurrent.ThreadSafe;

import org.rx.reactive.IDisposable;


/**
 * Thread-safe wrapper around WatchableSubscription that ensures unsubscribe can be called only once.
 */
@ThreadSafe
/* package */class AtomicWatchableSubscription implements IDisposable {

    private AtomicReference<IDisposable> actualSubscription = new AtomicReference<IDisposable>();
    private AtomicBoolean unsubscribed = new AtomicBoolean(false);

    public AtomicWatchableSubscription() {

    }

    public AtomicWatchableSubscription(IDisposable actualSubscription) {
        this.actualSubscription.set(actualSubscription);
    }

    /**
     * Set the actual subscription once it exists (if it wasn't available when constructed)
     * 
     * @param actualSubscription
     * @throws IllegalStateException
     *             if trying to set more than once (or use this method after setting via constructor)
     */
    public AtomicWatchableSubscription setActual(IDisposable actualSubscription) {
        if (!this.actualSubscription.compareAndSet(null, actualSubscription)) {
            throw new IllegalStateException("Can not set subscription more than once.");
        }
        return this;
    }

    @Override
    public void unsubscribe() {
        // get the real thing and set to null in an atomic operation so we will only ever call unsubscribe once
        IDisposable actual = actualSubscription.getAndSet(null);
        // if it's not null we will unsubscribe
        if (actual != null) {
            actual.unsubscribe();
            unsubscribed.set(true);
        }
    }

    public boolean isUnsubscribed() {
        return unsubscribed.get();
    }
}
