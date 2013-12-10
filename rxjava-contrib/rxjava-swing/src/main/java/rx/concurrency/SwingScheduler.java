package rx.concurrency;

import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Func2;

/**
 * Deprecated. Package changed from rx.concurrency to rx.schedulers.
 * 
 * @deprecated Use {@link rx.schedulers.SwingScheduler} instead. This will be removed before 1.0 release.
 */
@Deprecated
public class SwingScheduler extends Scheduler {

    private final static SwingScheduler INSTANCE = new SwingScheduler();

    public static SwingScheduler getInstance() {
        return INSTANCE;
    }

    private final rx.schedulers.SwingScheduler actual;

    private SwingScheduler() {
        actual = rx.schedulers.SwingScheduler.getInstance();
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        return actual.schedule(state, action);
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action, long delayTime, TimeUnit unit) {
        return actual.schedule(state, action, delayTime, unit);
    }

    public <T> Subscription schedulePeriodically(T state, final Func2<? super Scheduler, ? super T, ? extends Subscription> action, long initialDelay, long period, TimeUnit unit) {
        return actual.schedulePeriodically(state, action, initialDelay, period, unit);
    }

}
