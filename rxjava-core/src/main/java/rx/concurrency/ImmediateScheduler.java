package rx.concurrency;

import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Func2;

/**
 * Deprecated. Package changed from rx.concurrency to rx.schedulers.
 * 
 * @deprecated Use {@link rx.schedulers.ImmediateScheduler} instead. This will be removed before 1.0 release.
 */
@Deprecated
public class ImmediateScheduler extends Scheduler {

    private final static ImmediateScheduler INSTANCE = new ImmediateScheduler();

    public static ImmediateScheduler getInstance() {
        return INSTANCE;
    }

    private final rx.schedulers.ImmediateScheduler actual;

    private ImmediateScheduler() {
        actual = rx.schedulers.ImmediateScheduler.getInstance();
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        return actual.schedule(state, action);
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action, long delayTime, TimeUnit unit) {
        return actual.schedule(state, action, delayTime, unit);
    }

}
