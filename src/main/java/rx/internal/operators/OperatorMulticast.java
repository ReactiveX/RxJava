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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.observables.ConnectableObservable;
import rx.subjects.Subject;
import rx.subscriptions.Subscriptions;

/**
 * Shares a single subscription to a source through a Subject.
 * 
 * @param <T>
 *            the source value type
 * @param <R>
 *            the result value type
 */
public final class OperatorMulticast<T, R> extends ConnectableObservable<R> {
    final Observable<? extends T> source;
    final Object guard;
    final Func0<? extends Subject<? super T, ? extends R>> subjectFactory;
    private final AtomicReference<Subject<? super T, ? extends R>> connectedSubject;
    private final List<Subscriber<? super R>> waitingForConnect;

    /** Guarded by guard. */
    Subscriber<T> subscription;

    public OperatorMulticast(Observable<? extends T> source, final Func0<? extends Subject<? super T, ? extends R>> subjectFactory) {
        this(new Object(), new AtomicReference<Subject<? super T, ? extends R>>(), new ArrayList<Subscriber<? super R>>(), source, subjectFactory);
    }

    private OperatorMulticast(final Object guard, final AtomicReference<Subject<? super T, ? extends R>> connectedSubject, final List<Subscriber<? super R>> waitingForConnect, Observable<? extends T> source, final Func0<? extends Subject<? super T, ? extends R>> subjectFactory) {
        super(new OnSubscribe<R>() {
            @Override
            public void call(Subscriber<? super R> subscriber) {
                synchronized (guard) {
                    if (connectedSubject.get() == null) {
                        // not connected yet, so register
                        waitingForConnect.add(subscriber);
                    } else {
                        // we are already connected so subscribe directly
                        connectedSubject.get().unsafeSubscribe(subscriber);
                    }
                }
            }
        });
        this.guard = guard;
        this.connectedSubject = connectedSubject;
        this.waitingForConnect = waitingForConnect;
        this.source = source;
        this.subjectFactory = subjectFactory;
    }

    @Override
    public void connect(Action1<? super Subscription> connection) {
        // each time we connect we create a new Subject and Subscription

        boolean shouldSubscribe = false;

        // subscription is the state of whether we are connected or not
        synchronized (guard) {
            if (subscription != null) {
                // already connected, return as there is nothing to do
                return;
            } else {
                shouldSubscribe = true;
                // we aren't connected, so let's create a new Subject and connect
                final Subject<? super T, ? extends R> subject = subjectFactory.call();
                // create new Subscriber that will pass-thru to the subject we just created
                // we do this since it is also a Subscription whereas the Subject is not
                subscription = new Subscriber<T>() {
                    @Override
                    public void onCompleted() {
                        subject.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        subject.onError(e);
                    }

                    @Override
                    public void onNext(T args) {
                        subject.onNext(args);
                    }
                };
                
                // register any subscribers that are waiting with this new subject
                for(Subscriber<? super R> s : waitingForConnect) {
                    subject.unsafeSubscribe(s);
                }
                // clear the waiting list as any new ones that come in after leaving this synchronized block will go direct to the Subject
                waitingForConnect.clear();
                // record the Subject so OnSubscribe can see it
                connectedSubject.set(subject);
            }
        }

        // in the lock above we determined we should subscribe, do it now outside the lock
        if (shouldSubscribe) {
            // register a subscription that will shut this down
            connection.call(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    Subscription s;
                    synchronized (guard) {
                        s = subscription;
                        subscription = null;
                        connectedSubject.set(null);
                    }
                    if (s != null) {
                        s.unsubscribe();
                    }
                }
            }));

            // now that everything is hooked up let's subscribe
            // as long as the subscription is not null (which can happen if already unsubscribed)
            boolean subscriptionIsNull;
            synchronized(guard) {
                subscriptionIsNull = subscription == null;
            }
            if (!subscriptionIsNull)
                source.unsafeSubscribe(subscription);
        }
    }
}