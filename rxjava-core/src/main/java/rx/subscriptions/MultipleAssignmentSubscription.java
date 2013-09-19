package rx.subscriptions;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscription;

/**
 * Subscription that can be checked for status such as in a loop inside an {@link Observable} to exit the loop if unsubscribed.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.disposables.multipleassignmentdisposable">Rx.Net equivalent MultipleAssignmentDisposable</a>
 */
public class MultipleAssignmentSubscription implements Subscription {

    private final AtomicBoolean unsubscribed = new AtomicBoolean(false);
    private AtomicReference<Subscription> subscription = new AtomicReference<Subscription>();

    public boolean isUnsubscribed() {
        return unsubscribed.get();
    }

    @Override
    public synchronized void unsubscribe() {
        unsubscribed.set(true);
        Subscription s = getSubscription();
        if (s != null) {
            s.unsubscribe();
        }

    }

    public synchronized void setSubscription(Subscription s) {
        if (unsubscribed.get()) {
            s.unsubscribe();
        } else {
            subscription.set(s);
        }
    }

    public Subscription getSubscription() {
        return subscription.get();
    }

}
