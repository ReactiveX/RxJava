package rx.subscriptions;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.Subscription;

/**
 * Subscription that can be checked for status such as in a loop inside an {@link Observable} to exit the loop if unsubscribed.
 * 
 * @see Rx.Net equivalent BooleanDisposable at http://msdn.microsoft.com/en-us/library/system.reactive.disposables.booleandisposable(v=vs.103).aspx
 */
public class BooleanSubscription implements Subscription {

    private final AtomicBoolean unsubscribed = new AtomicBoolean(false);

    public boolean isUnsubscribed() {
        return unsubscribed.get();
    }

    @Override
    public void unsubscribe() {
        unsubscribed.set(true);
    }

}
