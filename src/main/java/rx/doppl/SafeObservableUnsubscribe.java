package rx.doppl;
import java.lang.ref.WeakReference;

import rx.Observable;
import rx.Subscriber;

/**
 * Created by kgalligan on 11/10/16.
 */

public class SafeObservableUnsubscribe
{
    final WeakReference<Observable> observableWeakReference;

    public SafeObservableUnsubscribe(Observable observable)
    {
        observableWeakReference = new WeakReference<Observable>(observable);
    }

    public void unsafeSubscribe(Subscriber subscriber)
    {
        Observable observable = observableWeakReference.get();
        if(observable != null)
            observable.unsafeSubscribe(subscriber);
    }
}
