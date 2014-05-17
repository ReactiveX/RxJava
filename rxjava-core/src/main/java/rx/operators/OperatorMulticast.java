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
package rx.operators;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.subjects.Subject;
import rx.subscriptions.Subscriptions;

/**
 * Shares a single subscription to a source through a Subject.
 * 
 * @param <T> the source value type
 * @param <R> the result value type
 */
public final class OperatorMulticast<T, R> extends ConnectableObservable<R> {
    final Observable<? extends T> source;
    final Subject<? super T, ? extends R> subject;
    final Object guard = new Object();
    /** Guarded by guard. */
    Subscription subscription;

    public OperatorMulticast(Observable<? extends T> source, final Subject<? super T, ? extends R> subject) {
        super(new OnSubscribe<R>() {
                @Override
                public void call(Subscriber<? super R> subscriber) {
                    subject.unsafeSubscribe(subscriber);
                }
            });
        this.source = source;
        this.subject = subject;
    }

    @Override
    public void connect(Action1<? super Subscription> connection) {
        connection.call(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                Subscription s;
                synchronized (guard) {
                    s = subscription;
                    subscription = null;
                }
                if (s != null) {
                    s.unsubscribe();
                }
            }
        }));
        Subscriber<T> s = null;
        synchronized (guard) {
            if (subscription == null) {
                s = new Subscriber<T>() {
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
                subscription = s;
            }
        }
        if (s != null) {
            source.unsafeSubscribe(s);
        }
    }
}
