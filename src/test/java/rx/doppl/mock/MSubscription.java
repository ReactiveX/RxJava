package rx.doppl.mock;
import rx.Subscription;

/**
 * Created by kgalligan on 10/14/16.
 */

public class MSubscription implements Subscription
{
    boolean unsubscribed = false;

    @Override
    public void unsubscribe()
    {
        unsubscribed = true;
    }

    @Override
    public boolean isUnsubscribed()
    {
        return unsubscribed;
    }
}
