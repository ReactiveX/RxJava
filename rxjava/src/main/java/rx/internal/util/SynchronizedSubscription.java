package rx.internal.util;

import rx.Subscription;

public class SynchronizedSubscription implements Subscription {

    private final Subscription s;

    public SynchronizedSubscription(Subscription s) {
        this.s = s;
    }

    @Override
    public synchronized void unsubscribe() {
        s.unsubscribe();
    }

    @Override
    public synchronized boolean isUnsubscribed() {
        return s.isUnsubscribed();
    }

}
