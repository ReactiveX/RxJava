/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Returns an observable sequence that stays connected to the source as long as
 * there is at least one subscription to the observable sequence.
 * 
 * @param <T>
 *            the value type
 */
public final class OnSubscribeRefCount<T> implements OnSubscribe<T> {

    private final ConnectableObservable<? extends T> source;
    private final AtomicReference<MultipleAssignmentSubscription> connectedSubscription = new AtomicReference<MultipleAssignmentSubscription>();
    private final AtomicInteger subscriptionCount = new AtomicInteger(0);

    /**
     * Constructor.
     * 
     * @param source
     *            observable to apply ref count to
     */
    public OnSubscribeRefCount(ConnectableObservable<? extends T> source) {
        this.source = source;
    }

    @Override
    public void call(final Subscriber<? super T> subscriber) {
        if (subscriber.isUnsubscribed()) {
            return;
        }
        // always subscribe
        source.unsafeSubscribe(subscriber);

        int count = subscriptionCount.getAndIncrement();
        if (count == 0) {
            // starting a new connection
            final MultipleAssignmentSubscription connectableSubscriptionHolder = new MultipleAssignmentSubscription();
            connectedSubscription.set(connectableSubscriptionHolder);
        }

        subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                if (subscriptionCount.decrementAndGet() == 0) {
                    // n->0 so we unsubscribe and disconnect
                    connectedSubscription.get().unsubscribe();
                }
            }
        }));

        // if we are incrementing from 0->1 we connect()
        if (count == 0) {
            try {
                source.connect(new Action1<Subscription>() {
                    @Override
                    public void call(final Subscription connectableSubscription) {
                        connectedSubscription.get().set(connectableSubscription);
                    }
                });
            } catch (Throwable e) {
                // in case any error occurred we want to reset back to 0 so next attempt can retry connecting
                subscriptionCount.set(0);
                Exceptions.propagate(e);
            }

        }
    }

}
