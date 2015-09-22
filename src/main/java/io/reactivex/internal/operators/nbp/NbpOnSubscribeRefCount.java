/**
 * Copyright 2015 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.nbp;

import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.SetCompositeResource;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.observables.nbp.NbpConnectableObservable;

/**
 * Returns an observable sequence that stays connected to the source as long as
 * there is at least one subscription to the observable sequence.
 * 
 * @param <T>
 *            the value type
 */
public final class NbpOnSubscribeRefCount<T> implements NbpOnSubscribe<T> {

    final class ConnectionSubscriber implements NbpSubscriber<T>, Disposable {
        final NbpSubscriber<? super T> subscriber;
        final SetCompositeResource<Disposable> currentBase;
        final Disposable resource;

        Disposable s;
        
        private ConnectionSubscriber(NbpSubscriber<? super T> subscriber,
                SetCompositeResource<Disposable> currentBase, Disposable resource) {
            this.subscriber = subscriber;
            this.currentBase = currentBase;
            this.resource = resource;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            subscriber.onSubscribe(this);
        }

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
        public void onComplete() {
            cleanup();
            subscriber.onComplete();
        }
        
        @Override
        public void dispose() {
            s.dispose();
            resource.dispose();
        }

        void cleanup() {
            // on error or completion we need to unsubscribe the base subscription
            // and set the subscriptionCount to 0 
            lock.lock();
            try {
                if (baseSubscription == currentBase) {
                    baseSubscription.dispose();
                    baseSubscription = new SetCompositeResource<>(Disposable::dispose);
                    subscriptionCount.set(0);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    final NbpConnectableObservable<? extends T> source;
    volatile SetCompositeResource<Disposable> baseSubscription = new SetCompositeResource<>(Disposable::dispose);
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
    public NbpOnSubscribeRefCount(NbpConnectableObservable<? extends T> source) {
        this.source = source;
    }

    @Override
    public void accept(final NbpSubscriber<? super T> subscriber) {

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

    private Consumer<Disposable> onSubscribe(final NbpSubscriber<? super T> subscriber,
            final AtomicBoolean writeLocked) {
        return  subscription -> {
            try {
                baseSubscription.add(subscription);
                // ready to subscribe to source so do it
                doSubscribe(subscriber, baseSubscription);
            } finally {
                // release the write lock
                lock.unlock();
                writeLocked.set(false);
            }
        };
    }
    
    void doSubscribe(final NbpSubscriber<? super T> subscriber, final SetCompositeResource<Disposable> currentBase) {
        // handle unsubscribing from the base subscription
        Disposable d = disconnect(currentBase);
        
        ConnectionSubscriber s = new ConnectionSubscriber(subscriber, currentBase, d);
        
        source.unsafeSubscribe(s);
    }

    private Disposable disconnect(final SetCompositeResource<Disposable> current) {
        return () -> {
            lock.lock();
            try {
                if (baseSubscription == current) {
                    if (subscriptionCount.decrementAndGet() == 0) {
                        baseSubscription.dispose();
                        // need a new baseSubscription because once
                        // unsubscribed stays that way
                        baseSubscription = new SetCompositeResource<>(Disposable::dispose);
                    }
                }
            } finally {
                lock.unlock();
            }
        };
    }
}