/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.flowable;

import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;

import org.reactivestreams.*;

import io.reactivex.disposables.*;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Returns an observable sequence that stays connected to the source as long as
 * there is at least one subscription to the observable sequence.
 * 
 * @param <T>
 *            the value type
 */
public final class FlowableRefCount<T> extends AbstractFlowableWithUpstream<T, T> {
    final ConnectableFlowable<? extends T> source;
    volatile CompositeDisposable baseSubscription = new CompositeDisposable();
    final AtomicInteger subscriptionCount = new AtomicInteger(0);

    /**
     * Use this lock for every subscription and disconnect action.
     */
    final ReentrantLock lock = new ReentrantLock();

    final class ConnectionSubscriber implements Subscriber<T>, Subscription {
        final Subscriber<? super T> subscriber;
        final CompositeDisposable currentBase;
        final Disposable resource;

        Subscription s;
        
        ConnectionSubscriber(Subscriber<? super T> subscriber,
                CompositeDisposable currentBase, Disposable resource) {
            this.subscriber = subscriber;
            this.currentBase = currentBase;
            this.resource = resource;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                subscriber.onSubscribe(this);
            }
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
        public void request(long n) {
            s.request(n);
        }
        
        @Override
        public void cancel() {
            s.cancel();
            resource.dispose();
        }

        void cleanup() {
            // on error or completion we need to unsubscribe the base subscription
            // and set the subscriptionCount to 0 
            lock.lock();
            try {
                if (baseSubscription == currentBase) {
                    baseSubscription.dispose();
                    baseSubscription = new CompositeDisposable();
                    subscriptionCount.set(0);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Constructor.
     * 
     * @param source
     *            observable to apply ref count to
     */
    public FlowableRefCount(ConnectableFlowable<T> source) {
        super(source);
        this.source = source;
    }

    @Override
    public void subscribeActual(final Subscriber<? super T> subscriber) {

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

    private Consumer<Disposable> onSubscribe(final Subscriber<? super T> subscriber,
            final AtomicBoolean writeLocked) {
        return new Consumer<Disposable>() {
            @Override
            public void accept(Disposable subscription) {
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
    
    void doSubscribe(final Subscriber<? super T> subscriber, final CompositeDisposable currentBase) {
        // handle unsubscribing from the base subscription
        Disposable d = disconnect(currentBase);
        
        ConnectionSubscriber s = new ConnectionSubscriber(subscriber, currentBase, d);
        
        source.subscribe(s);
    }

    private Disposable disconnect(final CompositeDisposable current) {
        return Disposables.fromRunnable(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    if (baseSubscription == current) {
                        if (subscriptionCount.decrementAndGet() == 0) {
                            baseSubscription.dispose();
                            // need a new baseSubscription because once
                            // unsubscribed stays that way
                            baseSubscription = new CompositeDisposable();
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        });
    }
}