package org.rx.reactive;

public interface IDisposable {

    /**
     * Stop receiving notifications on the observer that was registered when this IDisposable was received.
     * <p>
     * This allows unregistering a watcher before it has finished receiving all events (ie. before onCompleted is called).
     */
    public void unsubscribe();

}
