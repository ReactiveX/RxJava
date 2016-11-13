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
    final Observable hardRef;

    public SafeObservableUnsubscribe(Observable observable)
    {
        observableWeakReference = J2objcWeakReference.USE_WEAK ? new WeakReference<Observable>(observable) : null;
        hardRef = J2objcWeakReference.USE_WEAK ? null : observable;
    }

    public void unsafeSubscribe(Subscriber subscriber)
    {
        if(J2objcWeakReference.USE_WEAK)
        {
            Observable observable = observableWeakReference.get();
            if(observable != null) observable.unsafeSubscribe(subscriber);
        }
        else
        {
            hardRef.unsafeSubscribe(subscriber);
        }
    }
}
