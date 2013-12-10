package rx.concurrency;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Deprecated. Package changed from rx.concurrency to rx.schedulers.
 * 
 * @deprecated Use {@link rx.schedulers.ExecutorScheduler} instead. This will be removed before 1.0 release.
 */
@Deprecated
public class ExecutorScheduler extends rx.schedulers.ExecutorScheduler {

    @Deprecated
    public ExecutorScheduler(Executor executor) {
        super(executor);
    }

    @Deprecated
    public ExecutorScheduler(ScheduledExecutorService executor) {
        super(executor);
    }

}
