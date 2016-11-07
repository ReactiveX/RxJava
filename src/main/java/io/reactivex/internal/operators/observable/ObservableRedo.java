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

package io.reactivex.internal.operators.observable;

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.observers.ToNotificationObserver;
import io.reactivex.subjects.*;

public final class ObservableRedo<T> extends AbstractObservableWithUpstream<T, T> {
    final Function<? super Observable<Notification<Object>>, ? extends ObservableSource<?>> manager;

    final boolean retryMode;

    public ObservableRedo(ObservableSource<T> source,
            Function<? super Observable<Notification<Object>>, ? extends ObservableSource<?>> manager,
                    boolean retryMode) {
        super(source);
        this.manager = manager;
        this.retryMode = retryMode;
    }

    @Override
    public void subscribeActual(Observer<? super T> s) {

        Subject<Notification<Object>> subject = BehaviorSubject.<Notification<Object>>create().toSerialized();

        final RedoObserver<T> parent = new RedoObserver<T>(s, subject, source, retryMode);

        ToNotificationObserver<Object> actionObserver = new ToNotificationObserver<Object>(new Consumer<Notification<Object>>() {
            @Override
            public void accept(Notification<Object> o) {
                parent.handle(o);
            }
        });
        ListCompositeDisposable cd = new ListCompositeDisposable(parent.arbiter, actionObserver);
        s.onSubscribe(cd);

        ObservableSource<?> action;

        try {
            action = ObjectHelper.requireNonNull(manager.apply(subject), "The function returned a null ObservableSource");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            s.onError(ex);
            return;
        }

        action.subscribe(actionObserver);

        // trigger first subscription
        parent.handle(Notification.<Object>createOnNext(0));
    }

    static final class RedoObserver<T> extends AtomicBoolean implements Observer<T> {

        private static final long serialVersionUID = -1151903143112844287L;
        final Observer<? super T> actual;
        final Subject<Notification<Object>> subject;
        final ObservableSource<? extends T> source;
        final SequentialDisposable arbiter;

        final boolean retryMode;

        final AtomicInteger wip = new AtomicInteger();

        RedoObserver(Observer<? super T> actual, Subject<Notification<Object>> subject, ObservableSource<? extends T> source, boolean retryMode) {
            this.actual = actual;
            this.subject = subject;
            this.source = source;
            this.arbiter = new SequentialDisposable();
            this.retryMode = retryMode;
            this.lazySet(true);
        }

        @Override
        public void onSubscribe(Disposable s) {
            arbiter.replace(s);
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (compareAndSet(false, true)) {
                if (retryMode) {
                    subject.onNext(Notification.createOnError(t));
                } else {
                    subject.onError(t);
                }
            }
        }

        @Override
        public void onComplete() {
            if (compareAndSet(false, true)) {
                if (retryMode) {
                    subject.onComplete();
                } else {
                    subject.onNext(Notification.createOnComplete());
                }
            }
        }

        void handle(Notification<Object> notification) {
            if (compareAndSet(true, false)) {
                if (notification.isOnError()) {
                    arbiter.dispose();
                    actual.onError(notification.getError());
                } else {
                    if (notification.isOnNext()) {
                        if (wip.getAndIncrement() == 0) {
                            int missed = 1;
                            for (;;) {
                                if (arbiter.isDisposed()) {
                                    return;
                                }
                                source.subscribe(this);

                                missed = wip.addAndGet(-missed);
                                if (missed == 0) {
                                    break;
                                }
                            }
                        }
                    } else {
                        arbiter.dispose();
                        actual.onComplete();
                    }
                }
            }
        }
    }
}
