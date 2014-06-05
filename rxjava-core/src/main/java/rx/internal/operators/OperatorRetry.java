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

import rx.Notification;
import rx.Observable;
import rx.Observable.Operator;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.SerialSubscription;

public final class OperatorRetry<T> implements Operator<T, Observable<T>> {

    private static Scheduler scheduler = Schedulers.trampoline();
    private final Func1<Observable<Notification<?>>, Observable<?>> f;

    public OperatorRetry(Func1<Observable<Notification<?>>, Observable<?>> f) {
        this.f = f;
    }

    @Override
    public Subscriber<? super Observable<T>> call(final Subscriber<? super T> child) {
        final Scheduler.Worker inner = scheduler.createWorker();
        child.add(inner);
        
        final SerialSubscription serialSubscription = new SerialSubscription();
        // add serialSubscription so it gets unsubscribed if child is unsubscribed
        child.add(serialSubscription);
        
        return new SourceSubscriber<T>(child, f, inner, serialSubscription);
    }
    
    static final class SourceSubscriber<T> extends Subscriber<Observable<T>> {
        final Subscriber<? super T> child;
        final Scheduler.Worker inner;
        final SerialSubscription serialSubscription;
        PublishSubject<Notification<?>> ts = PublishSubject.create();
        
        public SourceSubscriber(final Subscriber<? super T> child,
                final Func1<Observable<Notification<?>>, Observable<?>> f,
                Scheduler.Worker inner,
                SerialSubscription serialSubscription) {
            this.child = child;
            this.inner = inner;
            this.serialSubscription = serialSubscription;
            f.call(ts).subscribe(new Observer<Object>() {

                @Override
                public void onCompleted() {
                    child.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    child.onError(e);
                }

                @Override
                public void onNext(Object t) {
                }
            });
        }
        
        
        @Override
        public void onCompleted() {
            // ignore as we expect a single nested Observable<T>
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }

        @Override
        public void onNext(final Observable<T> o) {

            inner.schedule(new Action0() {

                @Override
                public void call() {
                    final Action0 _self = this;
                    // new subscription each time so if it unsubscribes itself it does not prevent retries
                    // by unsubscribing the child subscription
                    Subscriber<T> subscriber = new Subscriber<T>() {

                        @Override
                        public void onCompleted() {
                            child.onCompleted();
                        }

                        @Override
                        public void onError(Throwable e) {
                            ts.onNext(Notification.createOnError(e));
                            if (!child.isUnsubscribed()) inner.schedule(_self);
                        }

                        @Override
                        public void onNext(T v) {
                            child.onNext(v);
                        }

                    };
                    // register this Subscription (and unsubscribe previous if exists)
                    serialSubscription.set(subscriber);
                    o.unsafeSubscribe(subscriber);
                }
            });
        }
    }
}
