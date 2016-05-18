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
import rx.observers.Subscribers;
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
    final AtomicReference<Subject<? super T, ? extends R>> connectedSubject;
    final List<Subscriber<? super R>> waitingForConnect;

    /** Guarded by guard. */
    Subscriber<T> subscription;
    // wraps subscription above for unsubscription using guard
    Subscription guardedSubscription;

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

    @SuppressWarnings("unchecked")
    @Override
    public void connect(Action1<? super Subscription> connection) {
        // each time we connect we create a new Subject and Subscription

        // subscription is the state of whether we are connected or not
        synchronized (guard) {
            if (subscription != null) {
                // already connected
                connection.call(guardedSubscription);
                return;
            } else {
                // we aren't connected, so let's create a new Subject and connect
                final Subject<? super T, ? extends R> subject = subjectFactory.call();
                // create new Subscriber that will pass-thru to the subject we just created
                // we do this since it is also a Subscription whereas the Subject is not
                subscription = Subscribers.from(subject);
                final AtomicReference<Subscription> gs = new AtomicReference<Subscription>();
                gs.set(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        Subscription s;
                        synchronized (guard) {
                            if ( guardedSubscription == gs.get()) {
                                s = subscription;
                                subscription = null;
                                guardedSubscription = null;
                                connectedSubject.set(null);
                            } else 
                                return;
                        }
                        if (s != null) {
                            s.unsubscribe();
                        }
                    }
                }));
                guardedSubscription = gs.get();
                
                // register any subscribers that are waiting with this new subject
                for(final Subscriber<? super R> s : waitingForConnect) {
                    subject.unsafeSubscribe(new Subscriber<R>(s) {
                        @Override
                        public void onNext(R t) {
                            s.onNext(t);
                        }
                        @Override
                        public void onError(Throwable e) {
                            s.onError(e);
                        }
                        @Override
                        public void onCompleted() {
                            s.onCompleted();
                        }
                    });
                }
                // clear the waiting list as any new ones that come in after leaving this synchronized block will go direct to the Subject
                waitingForConnect.clear();
                // record the Subject so OnSubscribe can see it
                connectedSubject.set(subject);
            }
            
        }

        // in the lock above we determined we should subscribe, do it now outside the lock
        // register a subscription that will shut this down
        connection.call(guardedSubscription);

        // now that everything is hooked up let's subscribe
        // as long as the subscription is not null (which can happen if already unsubscribed)
        Subscriber<T> sub; 
        synchronized (guard) {
            sub = subscription;
        }
        if (sub != null)
            ((Observable<T>)source).subscribe(sub);
    }
}