package rx.subscriptions;

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Action0;

public class ScheduledSubscription implements Subscription {
    private final Subscription underlying;
    private final Scheduler scheduler;

    public ScheduledSubscription(Subscription underlying, Scheduler scheduler) {
        this.underlying = underlying;
        this.scheduler = scheduler;
    }

    @Override
    public void unsubscribe() {
        scheduler.schedule(new Action0() {
            @Override
            public void call() {
                underlying.unsubscribe();
            }
        });
    }

    public Scheduler getScheduler() {
        return this.scheduler;
    }

}
