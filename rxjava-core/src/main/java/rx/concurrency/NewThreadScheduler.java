package rx.concurrency;

import rx.Subscription;
import rx.util.functions.Func0;

public class NewThreadScheduler extends AbstractScheduler {
    private static final NewThreadScheduler INSTANCE = new NewThreadScheduler();

    public static NewThreadScheduler getInstance() {
        return INSTANCE;
    }


    @Override
    public Subscription schedule(Func0<Subscription> action) {
        final DiscardableAction discardableAction = new DiscardableAction(action);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                discardableAction.call();
            }
        });

        t.start();

        return discardableAction;
    }

}
