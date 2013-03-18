package rx.concurrency;

import rx.Scheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

public class Schedulers {
    private Schedulers() {

    }

    public static Scheduler immediate() {
        return ImmediateScheduler.getInstance();
    }

    public static Scheduler currentThread() {
        return CurrentThreadScheduler.getInstance();
    }

    public static Scheduler newThread() {
        return NewThreadScheduler.getInstance();
    }

    public static Scheduler executor(Executor executor) {
        return new ExecutorScheduler(executor);
    }

    public static Scheduler scheduledExecutor(ScheduledExecutorService executor) {
        return new ScheduledExecutorServiceScheduler(executor);
    }
}
