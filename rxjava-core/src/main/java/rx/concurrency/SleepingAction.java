package rx.concurrency;

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.Func0;

import java.util.concurrent.TimeUnit;

public class SleepingAction implements Func0<Subscription> {
    private final Func0<Subscription> underlying;
    private final Scheduler scheduler;
    private final long execTime;

    public SleepingAction(Func0<Subscription> underlying, Scheduler scheduler, long timespan, TimeUnit timeUnit) {
        this.underlying = underlying;
        this.scheduler = scheduler;
        this.execTime = scheduler.now() + timeUnit.toMillis(timespan);
    }

    @Override
    public Subscription call() {
        if (execTime < scheduler.now()) {
            try {
                Thread.sleep(scheduler.now() - execTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        return underlying.call();

    }
}
