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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.CompositeSubscription;
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
    volatile CompositeSubscription baseSubscription = new CompositeSubscription();
    final AtomicInteger subscriptionCount = new AtomicInteger(0);

    /**
     * Use this lock for every subscription and disconnect action.
     */
    final ReentrantLock lock = new ReentrantLock();

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

        lock.lock();
        if (subscriptionCount.incrementAndGet() == 1) {

            final AtomicBoolean writeLocked = new AtomicBoolean(true);

            try {
                // need to use this overload of connect to ensure that
                // baseSubscription is set in the case that source is a
                // synchronous Observable
                source.connect(onSubscribe(subscriber, writeLocked));
            } finally {
                // need to cover the case where the source is subscribed to
                // outside of this class thus preventing the Action1 passed
                // to source.connect above being called
                if (writeLocked.get()) {
                    // Action1 passed to source.connect was not called
                    lock.unlock();
                }
            }
        } else {
            try {
                // ready to subscribe to source so do it
                doSubscribe(subscriber, baseSubscription);
            } finally {
                // release the read lock
                lock.unlock();
            }
        }

    }

    private Action1<Subscription> onSubscribe(final Subscriber<? super T> subscriber,
            final AtomicBoolean writeLocked) {
        return new Action1<Subscription>() {
            @Override
            public void call(Subscription subscription) {

                try {
                    baseSubscription.add(subscription);
                    // ready to subscribe to source so do it
                    doSubscribe(subscriber, baseSubscription);
                } finally {
                    // release the write lock
                    lock.unlock();
                    writeLocked.set(false);
                }
            }
        };
    }
    
    void doSubscribe(final Subscriber<? super T> subscriber, final CompositeSubscription currentBase) {
        // handle unsubscribing from the base subscription
        subscriber.add(disconnect(currentBase));
        
        source.unsafeSubscribe(new Subscriber<T>(subscriber) {
            @Override
            public void onError(Throwable e) {
                cleanup();
                subscriber.onError(e);
            }
            @Override
            public void onNext(T t) {
                subscriber.onNext(t);
            }
            @Override
            public void onCompleted() {
                cleanup();
                subscriber.onCompleted();
            }
            void cleanup() {
                // on error or completion we need to unsubscribe the base subscription
                // and set the subscriptionCount to 0 
                lock.lock();
                try {
                    if (baseSubscription == currentBase) {
                        baseSubscription.unsubscribe();
                        baseSubscription = new CompositeSubscription();
                        subscriptionCount.set(0);
                    }
                } finally {
                    lock.unlock();
                }
            }
        });
    }

    private Subscription disconnect(final CompositeSubscription current) {
        return Subscriptions.create(new Action0() {
            @Override
            public void call() {
                lock.lock();
                try {
                    if (baseSubscription == current) {
                        if (subscriptionCount.decrementAndGet() == 0) {
                            baseSubscription.unsubscribe();
                            // need a new baseSubscription because once
                            // unsubscribed stays that way
                            baseSubscription = new CompositeSubscription();
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        });
    }
}