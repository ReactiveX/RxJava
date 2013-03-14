package rx.concurrency;

import rx.Subscription;
import rx.util.functions.Func0;

public final class ImmediateScheduler extends AbstractScheduler {
    private static final ImmediateScheduler INSTANCE = new ImmediateScheduler();

    private ImmediateScheduler() {

    }

    public static ImmediateScheduler getInstance() {
        return INSTANCE;
    }

    @Override
    public Subscription schedule(Func0<Subscription> action) {
        return new DiscardableAction(action);
    }

}
