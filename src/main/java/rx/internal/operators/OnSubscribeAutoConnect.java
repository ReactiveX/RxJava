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
package rx.internal.operators;

import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.observers.Subscribers;

/**
 * Wraps a ConnectableObservable and calls its connect() method once
 * the specified number of Subscribers have subscribed.
 *
 * @param <T> the value type of the chain
 */
public final class OnSubscribeAutoConnect<T> implements OnSubscribe<T> {
    final ConnectableObservable<? extends T> source;
    final int numberOfSubscribers;
    final Action1<? super Subscription> connection;
    final AtomicInteger clients;
    
    public OnSubscribeAutoConnect(ConnectableObservable<? extends T> source,
            int numberOfSubscribers,
            Action1<? super Subscription> connection) {
        if (numberOfSubscribers <= 0) {
            throw new IllegalArgumentException("numberOfSubscribers > 0 required");
        }
        this.source = source;
        this.numberOfSubscribers = numberOfSubscribers;
        this.connection = connection;
        this.clients = new AtomicInteger();
    }
    @Override
    public void call(Subscriber<? super T> child) {
        source.unsafeSubscribe(Subscribers.wrap(child));
        if (clients.incrementAndGet() == numberOfSubscribers) {
            source.connect(connection);
        }
    }
}
