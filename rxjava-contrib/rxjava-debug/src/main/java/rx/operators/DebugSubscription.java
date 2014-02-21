package rx.operators;

import rx.Subscription;
import rx.plugins.DebugNotification;

final class DebugSubscription<T> implements Subscription {
    private final DebugSubscriber<T> debugObserver;

    DebugSubscription(DebugSubscriber<T> debugObserver) {
        this.debugObserver = debugObserver;
    }

    @Override
    public void unsubscribe() {
        debugObserver.events.call(DebugNotification.<T> createUnsubscribe(debugObserver.o, debugObserver.from, debugObserver.to));
        debugObserver.unsubscribe();
    }

    @Override
    public boolean isUnsubscribed() {
        return debugObserver.isUnsubscribed();
    }
}