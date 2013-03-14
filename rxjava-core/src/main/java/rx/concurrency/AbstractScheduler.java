package rx.concurrency;

import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func0;

import java.util.concurrent.TimeUnit;

public abstract class AbstractScheduler implements Scheduler {

    @Override
    public Subscription schedule(Action0 action) {
        return schedule(asFunc0(action));
    }

    @Override
    public Subscription schedule(Action0 action, long timespan, TimeUnit unit) {
        return schedule(asFunc0(action), timespan, unit);
    }

    @Override
    public Subscription schedule(Func0<Subscription> action, long timespan, TimeUnit unit) {
        return schedule(new DelayedAction(action, this, timespan, unit));
    }

    @Override
    public long now() {
        return System.nanoTime();
    }

    private static Func0<Subscription> asFunc0(final Action0 action) {
        return new Func0<Subscription>() {
            @Override
            public Subscription call() {
                action.call();
                return Subscriptions.empty();
            }
        };
    }

}
