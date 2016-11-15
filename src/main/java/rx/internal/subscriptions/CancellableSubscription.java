package rx.internal.subscriptions;

import java.util.concurrent.atomic.AtomicReference;

import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.Cancellable;
import rx.plugins.RxJavaHooks;

/**
 * A Subscription that wraps an Cancellable instance.
 */
public final class CancellableSubscription
extends AtomicReference<Cancellable>
implements Subscription {

    /** */
    private static final long serialVersionUID = 5718521705281392066L;

    public CancellableSubscription(Cancellable cancellable) {
        super(cancellable);
    }

    @Override
    public boolean isUnsubscribed() {
        return get() == null;
    }

    @Override
    public void unsubscribe() {
        if (get() != null) {
            Cancellable c = getAndSet(null);
            if (c != null) {
                try {
                    c.cancel();
                } catch (Exception ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaHooks.onError(ex);
                }
            }
        }
    }
}
