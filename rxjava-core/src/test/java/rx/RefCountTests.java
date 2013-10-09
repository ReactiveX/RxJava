package rx;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class RefCountTests {

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void onlyFirstShouldSubscribeAndLastUnsubscribe() {
        final AtomicInteger subscriptionCount = new AtomicInteger();
        final AtomicInteger unsubscriptionCount = new AtomicInteger();
        Observable<Integer> observable = Observable.create(new Observable.OnSubscribeFunc<Integer>() {
            @Override
            public Subscription onSubscribe(Observer<? super Integer> observer) {
                subscriptionCount.incrementAndGet();
                return Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        unsubscriptionCount.incrementAndGet();
                    }
                });
            }
        });
        Observable<Integer> refCounted = observable.publish().refCount();
        Observer<Integer> observer = mock(Observer.class);
        Subscription first = refCounted.subscribe(observer);
        assertEquals(1, subscriptionCount.get());
        Subscription second = refCounted.subscribe(observer);
        assertEquals(1, subscriptionCount.get());
        first.unsubscribe();
        assertEquals(0, unsubscriptionCount.get());
        second.unsubscribe();
        assertEquals(1, unsubscriptionCount.get());
    }
}
