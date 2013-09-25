package rx;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import static org.mockito.Mockito.*;

public class RefCountTests {

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void subscriptionToUnderlyingOnFirstSubscription() {
        @SuppressWarnings("unchecked")
        ConnectableObservable<Integer> connectable = mock(ConnectableObservable.class);
        Observable<Integer> refCounted = ConnectableObservable.refCount(connectable);
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        when(connectable.subscribe(any(Observer.class))).thenReturn(Subscriptions.empty());
        when(connectable.connect()).thenReturn(Subscriptions.empty());
        refCounted.subscribe(observer);
        verify(connectable, times(1)).subscribe(any(Observer.class));
        verify(connectable, times(1)).connect();
    }

    @Test
    public void noSubscriptionToUnderlyingOnSecondSubscription() {
        @SuppressWarnings("unchecked")
        ConnectableObservable<Integer> connectable = mock(ConnectableObservable.class);
        Observable<Integer> refCounted = ConnectableObservable.refCount(connectable);
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        when(connectable.subscribe(any(Observer.class))).thenReturn(Subscriptions.empty());
        when(connectable.connect()).thenReturn(Subscriptions.empty());
        refCounted.subscribe(observer);
        refCounted.subscribe(observer);
        verify(connectable, times(2)).subscribe(any(Observer.class));
        verify(connectable, times(1)).connect();
    }

    @Test
    public void unsubscriptionFromUnderlyingOnLastUnsubscription() {
        @SuppressWarnings("unchecked")
        ConnectableObservable<Integer> connectable = mock(ConnectableObservable.class);
        Observable<Integer> refCounted = ConnectableObservable.refCount(connectable);
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        Subscription underlying = mock(Subscription.class);
        when(connectable.subscribe(any(Observer.class))).thenReturn(underlying);
        Subscription connection = mock(Subscription.class);
        when(connectable.connect()).thenReturn(connection);
        Subscription first = refCounted.subscribe(observer);
        first.unsubscribe();
        verify(underlying, times(1)).unsubscribe();
        verify(connection, times(1)).unsubscribe();
    }

    @Test
    public void noUnsubscriptionFromUnderlyingOnFirstUnsubscription() {
        @SuppressWarnings("unchecked")
        ConnectableObservable<Integer> connectable = mock(ConnectableObservable.class);
        Observable<Integer> refCounted = ConnectableObservable.refCount(connectable);
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        Subscription underlying = mock(Subscription.class);
        when(connectable.subscribe(any(Observer.class))).thenReturn(underlying);
        Subscription connection = mock(Subscription.class);
        when(connectable.connect()).thenReturn(connection);
        Subscription first = refCounted.subscribe(observer);
        Subscription second = refCounted.subscribe(observer);
        first.unsubscribe();
        second.unsubscribe();
        verify(underlying, times(2)).unsubscribe();
        verify(connection, times(1)).unsubscribe();
    }
}
