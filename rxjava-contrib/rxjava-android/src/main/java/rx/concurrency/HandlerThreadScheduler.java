package rx.concurrency;

import android.os.Handler;
import rx.Scheduler;
import rx.Subscription;
import rx.operators.AtomicObservableSubscription;
import rx.util.functions.Func2;

import java.util.concurrent.TimeUnit;

/**
 * Schedules actions to run on an Android Handler thread.
 */
public class HandlerThreadScheduler extends Scheduler {

    private final Handler handler;

    public HandlerThreadScheduler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action) {
        final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
        final Scheduler _scheduler = this;

        handler.post(new Runnable() {
            @Override
            public void run() {
                subscription.wrap(action.call(_scheduler, state));
            }
        });
        return subscription;
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action, long delayTime, TimeUnit unit) {
        if (delayTime == 0) {
            return schedule(state, action);
        } else {
            final AtomicObservableSubscription subscription = new AtomicObservableSubscription();
            final Scheduler _scheduler = this;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    subscription.wrap(action.call(_scheduler, state));
                }
            }, unit.toMillis(delayTime));
            return subscription;
        }
    }
}


