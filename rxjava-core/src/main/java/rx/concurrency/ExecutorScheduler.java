package rx.concurrency;

import rx.Subscription;
import rx.util.functions.Func0;

import java.util.concurrent.Executor;

public class ExecutorScheduler extends AbstractScheduler {
    private final Executor executor;

    public ExecutorScheduler(Executor executor) {
        this.executor = executor;
    }

    @Override
    public Subscription schedule(Func0<Subscription> action) {
        final DiscardableAction discardableAction = new DiscardableAction(action);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                discardableAction.call();
            }
        });

        return discardableAction;

    }
}
