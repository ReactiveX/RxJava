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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

/**
 * Returns an observable sequence that stays connected to the source as long as
 * there is at least one subscription to the observable sequence.
 * 
 * @param <T>
 *            the value type
 */
public final class OnSubscribeRefCount<T> implements OnSubscribe<T> {

    private ConnectableObservable<? extends T> source;
    private volatile Subscription baseSubscription;
    private AtomicInteger subscriptionCount = new AtomicInteger(0);

    /**
     * Ensures that subscribers wait for the first subscription to be assigned
     * to baseSubcription before being subscribed themselves.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

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

        // ensure secondary subscriptions wait for baseSubscription to be set by
        // first subscription
        lock.writeLock().lock();

        if (subscriptionCount.incrementAndGet() == 1) {
            // need to use this overload of connect to ensure that
            // baseSubscription is set in the case that source is a synchronous
            // Observable
            source.connect(new Action1<Subscription>() {
                @Override
                public void call(Subscription subscription) {
                    baseSubscription = subscription;

                    // handle unsubscribing from the base subscription
                    subscriber.add(disconnect());

                    // ready to subscribe to source so do it
                    source.unsafeSubscribe(subscriber);

                    // release the write lock
                    lock.writeLock().unlock();
                }
            });
        } else {
            // release the write lock
            lock.writeLock().unlock();

            // wait till baseSubscription set
            lock.readLock().lock();

            // handle unsubscribing from the base subscription
            subscriber.add(disconnect());
            
            // ready to subscribe to source so do it
            source.unsafeSubscribe(subscriber);
            
            //release the read lock
            lock.readLock().unlock();
        }

    }

    private Subscription disconnect() {
        return Subscriptions.create(new Action0() {
            @Override
            public void call() {
                if (subscriptionCount.decrementAndGet() == 0) {
                    baseSubscription.unsubscribe();
                }
            }
        });
    }
}
