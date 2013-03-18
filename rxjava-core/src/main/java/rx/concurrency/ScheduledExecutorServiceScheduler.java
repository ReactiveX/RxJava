package rx.concurrency;

import rx.Subscription;
import rx.util.functions.Func0;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// TODO [@mairbek] silly name
public class ScheduledExecutorServiceScheduler extends AbstractScheduler {
    private final ScheduledExecutorService executorService;

    public ScheduledExecutorServiceScheduler(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public Subscription schedule(Func0<Subscription> action) {
        return schedule(action, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public Subscription schedule(Func0<Subscription> action, long timespan, TimeUnit unit) {
        final DiscardableAction discardableAction = new DiscardableAction(action);
        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                discardableAction.call();
            }
        }, timespan, unit);
        return discardableAction;
    }

}
