package rx.operators;

import rx.Subscription;
import rx.plugins.DebugNotification;
import rx.plugins.DebugNotificationListener;

final class DebugSubscription<T, C> implements Subscription {
    private final DebugSubscriber<T, C> debugObserver;
    private DebugNotificationListener<C> listener;

    DebugSubscription(DebugSubscriber<T, C> debugObserver, DebugNotificationListener<C> listener) {
        this.debugObserver = debugObserver;
        this.listener = listener;
    }

    @Override
    public void unsubscribe() {
        final DebugNotification<T> n = DebugNotification.<T> createUnsubscribe(debugObserver.getActual(), debugObserver.getFrom(), debugObserver.getTo());
        C context = listener.start(n);
        try {
            debugObserver.unsubscribe();
            listener.complete(context);
        } catch (Throwable e) {
            listener.error(context, e);
        }
    }

    @Override
    public boolean isUnsubscribed() {
        return debugObserver.isUnsubscribed();
    }
}