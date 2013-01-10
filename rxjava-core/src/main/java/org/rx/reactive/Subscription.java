package org.rx.reactive;

public interface Subscription {

    /**
     * Stop receiving notifications on the observer that was registered when this IDisposable was received.
     * <p>
     * This allows unregistering a Observer before it has finished receiving all events (ie. before onCompleted is called).
     */
    public void unsubscribe();

}
