package rx.concurrency;

import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Func2;

/**
 * Deprecated. Package changed from rx.concurrency to rx.schedulers.
 * 
 * @deprecated Use {@link rx.schedulers.NewThreadScheduler} instead. This will be removed before 1.0 release.
 */
@Deprecated
public class NewThreadScheduler extends Scheduler {

    private final static NewThreadScheduler INSTANCE = new NewThreadScheduler();

    public static NewThreadScheduler getInstance() {
        return INSTANCE;
    }

    private final rx.schedulers.NewThreadScheduler actual;

    private NewThreadScheduler() {
        actual = rx.schedulers.NewThreadScheduler.getInstance();
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
