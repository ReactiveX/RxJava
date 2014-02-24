package rx.operators;

import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.plugins.DebugNotification;

final class DebugSubscription<T, C> implements Subscription {
    private final DebugSubscriber<T, C> debugObserver;
    private final Func1<DebugNotification, C> start;
    private final Action1<C> complete;
    private final Action2<C, Throwable> error;

    DebugSubscription(DebugSubscriber<T, C> debugObserver, Func1<DebugNotification, C> start, Action1<C> complete, Action2<C, Throwable> error) {
        this.debugObserver = debugObserver;
        this.start = start;
        this.complete = complete;
        this.error = error;
    }

    @Override
    public void unsubscribe() {
        final DebugNotification<T, C> n = DebugNotification.<T, C> createUnsubscribe(debugObserver.getActual(), debugObserver.getFrom(), debugObserver.getTo());
        C context = start.call(n);
        try {
            debugObserver.unsubscribe();
            complete.call(context);
        } catch (Throwable e) {
            error.call(context, e);
        }
    }

    @Override
    public boolean isUnsubscribed() {
        return debugObserver.isUnsubscribed();
    }
}