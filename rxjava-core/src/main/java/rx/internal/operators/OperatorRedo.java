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
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.SerialSubscription;

public final class OperatorRedo<T> implements OnSubscribe<T> {

    private Observable<T> source;
    private final Func1<? super Observable<? extends Notification<?>>, ? extends Observable<? extends Notification<?>>> f;
    private boolean stopOnComplete;
    private boolean stopOnError;
    private final Scheduler scheduler;

    public static <T> OnSubscribe<T> retry(Observable<T> source,
            Func1<? super Observable<? extends Notification<?>>, ? extends Observable<? extends Notification<?>>> notificationHandler) {
        return retry(source, notificationHandler, Schedulers.trampoline());
    }

    public static <T> OnSubscribe<T> retry(Observable<T> source,
            Func1<? super Observable<? extends Notification<?>>, ? extends Observable<? extends Notification<?>>> notificationHandler,
            Scheduler scheduler) {
        return new OperatorRedo<T>(source, notificationHandler, true, false, scheduler);
    }

    public static <T> OnSubscribe<T> repeat(Observable<T> source,
            Func1<? super Observable<? extends Notification<?>>, ? extends Observable<? extends Notification<?>>> notificationHandler) {
        return repeat(source, notificationHandler, Schedulers.trampoline());
    }

    public static <T> OnSubscribe<T> repeat(Observable<T> source,
            Func1<? super Observable<? extends Notification<?>>, ? extends Observable<? extends Notification<?>>> notificationHandler,
            Scheduler scheduler) {
        return new OperatorRedo<T>(source, notificationHandler, false, true, scheduler);
    }

    public static <T> OnSubscribe<T> redo(Observable<T> source,
            Func1<? super Observable<? extends Notification<?>>, ? extends Observable<? extends Notification<?>>> notificationHandler) {
        return redo(source, notificationHandler, Schedulers.trampoline());
    }

    public static <T> OnSubscribe<T> redo(Observable<T> source,
            Func1<? super Observable<? extends Notification<?>>, ? extends Observable<? extends Notification<?>>> notificationHandler,
            Scheduler scheduler) {
        return new OperatorRedo<T>(source, notificationHandler, false, false, scheduler);
    }

    private OperatorRedo(Observable<T> source,
            Func1<? super Observable<? extends Notification<?>>, ? extends Observable<? extends Notification<?>>> f,
            boolean stopOnComplete, boolean stopOnError, Scheduler scheduler) {
        this.source = source;
        this.f = f;
        this.stopOnComplete = stopOnComplete;
        this.stopOnError = stopOnError;
        this.scheduler = scheduler;
    }

    @Override
    public void call(final Subscriber<? super T> child) {
        final Scheduler.Worker inner = scheduler.createWorker();
        child.add(inner);

        final SerialSubscription serialSubscription = new SerialSubscription();
        // add serialSubscription so it gets unsubscribed if child is unsubscribed
        child.add(serialSubscription);

        final PublishSubject<Notification<?>> ts = PublishSubject.create();

        final Action0 action = new Action0() {
            @Override
            public void call() {
                // new subscription each time so if it unsubscribes itself it does not prevent retries
                // by unsubscribing the child subscription
                Subscriber<T> subscriber = new Subscriber<T>() {
                    @Override
                    public void onCompleted() {
                        ts.onNext(Notification.createOnCompleted());
                    }

                    @Override
                    public void onError(Throwable e) {
                        ts.onNext(Notification.createOnError(e));
                    }

                    @Override
                    public void onNext(T v) {
                        child.onNext(v);
                    }
                };
                // register this Subscription (and unsubscribe previous if exists)
                serialSubscription.set(subscriber);
                source.unsafeSubscribe(subscriber);
            }
        };

        f.call(ts.lift(new Operator<Notification<?>, Notification<?>>() {
            @Override
            public Subscriber<? super Notification<?>> call(final Subscriber<? super Notification<?>> terminals) {
                return new Subscriber<Notification<?>>(terminals) {
                    @Override
                    public void onCompleted() {
                        terminals.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        terminals.onError(e);
                    }

                    @Override
                    public void onNext(Notification<?> t) {
                        if (t.isOnCompleted() && stopOnComplete) terminals.onCompleted();
                        else if (t.isOnError() && stopOnError) terminals.onError(t.getThrowable());
                        else terminals.onNext(t);
                    }
                };
            }
        })).subscribe(new Observer<Notification<?>>() {

            @Override
            public void onCompleted() {
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(Notification<?> t) {
                if (t.isOnCompleted()) child.onCompleted();
                else if (t.isOnError()) child.onError(t.getThrowable());
                else if (!child.isUnsubscribed()) inner.schedule(action);
            }
        });

        inner.schedule(action);
    }
}
