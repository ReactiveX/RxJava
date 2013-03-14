package rx.concurrency;

import rx.Subscription;
import rx.util.AtomicObservableSubscription;
import rx.util.functions.Func0;

import java.util.concurrent.atomic.AtomicBoolean;

public class DiscardableAction implements Func0<Subscription>, Subscription {
    private final Func0<Subscription> underlying;

    private final AtomicObservableSubscription wrapper = new AtomicObservableSubscription();
    private final AtomicBoolean ready = new AtomicBoolean(true);

    public DiscardableAction(Func0<Subscription> underlying) {
        this.underlying = underlying;
    }

    @Override
    public Subscription call() {
        if (ready.compareAndSet(true, false)) {
            Subscription subscription = underlying.call();
            wrapper.wrap(subscription);
            return subscription;
        }
        return wrapper;
    }

    @Override
    public void unsubscribe() {
        ready.set(false);
        wrapper.unsubscribe();
    }
}

