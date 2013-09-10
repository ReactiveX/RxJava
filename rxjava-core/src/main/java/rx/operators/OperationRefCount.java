/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import static org.mockito.Mockito.*;

/**
 * Returns an observable sequence that stays connected to the source as long
 * as there is at least one subscription to the observable sequence.
 */
public final class OperationRefCount<T> {
    public static <T> Func1<Observer<T>, Subscription> refCount(ConnectableObservable<T> connectableObservable) {
        return new RefCount<T>(connectableObservable);
    }

    private static class RefCount<T> implements Func1<Observer<T>, Subscription> {
        private final ConnectableObservable<T> innerConnectableObservable;

        public RefCount(ConnectableObservable<T> innerConnectableObservable) {
            this.innerConnectableObservable = innerConnectableObservable;
        }

        @Override
        public Subscription call(Observer<T> observer) {
            throw new UnsupportedOperationException();
        }
    }

    public static class UnitTest {

        @Before
        public void setUp() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void subscriptionToUnderlyingOnFirstSubscription() {
            ConnectableObservable<Integer> connectable = mock(ConnectableObservable.class);
            Observable<Integer> refCounted = Observable.create(refCount(connectable));
            Observer<Integer> observer = mock(Observer.class);
            when(connectable.subscribe(observer)).thenReturn(Subscriptions.empty());
            when(connectable.connect()).thenReturn(Subscriptions.empty());
            refCounted.subscribe(observer);
            verify(connectable, times(1)).subscribe(observer);
            verify(connectable, times(1)).connect();
        }

        @Test
        public void noSubscriptionToUnderlyingOnSecondSubscription() {

        }

        @Test
        public void unsubscriptionFromUnderlyingOnLastUnsubscription() {

        }

        @Test
        public void noUnsubscriptionFromUnderlyingOnFirstUnsubscription() {

        }
    }
}
