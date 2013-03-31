package rx.testing;

import rx.Observable;
import rx.Observer;
import rx.Subscription;

import static org.junit.Assert.assertTrue;

public class UnsubscribeTester
{
    private Subscription subscription;

    public UnsubscribeTester() {}

    public static <T> UnsubscribeTester createOnNext(Observable<T> observable)
    {
        final UnsubscribeTester test = new UnsubscribeTester();
        test.setSubscription(observable.subscribe(new Observer<T>()
        {
            @Override
            public void onCompleted()
            {
            }

            @Override
            public void onError(Exception e)
            {
            }

            @Override
            public void onNext(T args)
            {
                test.doUnsubscribe();
            }
        }));
        return test;
    }

    public static <T> UnsubscribeTester createOnCompleted(Observable<T> observable)
    {
        final UnsubscribeTester test = new UnsubscribeTester();
        test.setSubscription(observable.subscribe(new Observer<T>()
        {
            @Override
            public void onCompleted()
            {
                test.doUnsubscribe();
            }

            @Override
            public void onError(Exception e)
            {
            }

            @Override
            public void onNext(T args)
            {
            }
        }));
        return test;
    }

    public static <T> UnsubscribeTester createOnError(Observable<T> observable)
    {
        final UnsubscribeTester test = new UnsubscribeTester();
        test.setSubscription(observable.subscribe(new Observer<T>()
        {
            @Override
            public void onCompleted()
            {
            }

            @Override
            public void onError(Exception e)
            {
                test.doUnsubscribe();
            }

            @Override
            public void onNext(T args)
            {
            }
        }));
        return test;
    }

    private void setSubscription(Subscription subscription)
    {
        this.subscription = subscription;
    }

    private void doUnsubscribe()
    {
        Subscription subscription = this.subscription;
        this.subscription = null;
        subscription.unsubscribe();
    }

    public void assertPassed()
    {
        assertTrue("expected notification was received", subscription == null);
    }
}
