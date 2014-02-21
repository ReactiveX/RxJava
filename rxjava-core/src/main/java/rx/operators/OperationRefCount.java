/**
 * Copyright 2014 Netflix, Inc.
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

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action0;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

/**
 * Returns an observable sequence that stays connected to the source as long
 * as there is at least one subscription to the observable sequence.
 */
public final class OperationRefCount<T> {
    public static <T> Observable.OnSubscribeFunc<T> refCount(ConnectableObservable<T> connectableObservable) {
        return new RefCount<T>(connectableObservable);
    }

    private static class RefCount<T> implements Observable.OnSubscribeFunc<T> {
        private final ConnectableObservable<T> innerConnectableObservable;
        private final Object gate = new Object();
        private int count = 0;
        private Subscription connection = null;

        public RefCount(ConnectableObservable<T> innerConnectableObservable) {
            this.innerConnectableObservable = innerConnectableObservable;
        }

        @Override
        public Subscription onSubscribe(Observer<? super T> observer) {
            final Subscription subscription = innerConnectableObservable.subscribe(observer);
            synchronized (gate) {
                if (count++ == 0) {
                    connection = innerConnectableObservable.connect();
                }
            }
            return Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    synchronized (gate) {
                        if (--count == 0) {
                            connection.unsubscribe();
                            connection = null;
                        }
                    }
                    subscription.unsubscribe();
                }
            });
        }
    }
}