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
import io.reactivex.Observable.*;
import io.reactivex.disposables.*;
import io.reactivex.functions.*;
import io.reactivex.internal.subscribers.observable.NbpToNotificationSubscriber;
import io.reactivex.subjects.BehaviorSubject;

public final class NbpOnSubscribeRedo<T> implements NbpOnSubscribe<T> {
    final Observable<? extends T> source;
    final Function<? super Observable<Try<Optional<Object>>>, ? extends Observable<?>> manager;

    public NbpOnSubscribeRedo(Observable<? extends T> source,
            Function<? super Observable<Try<Optional<Object>>>, ? extends Observable<?>> manager) {
        this.source = source;
        this.manager = manager;
    }
    
    @Override
    public void accept(Observer<? super T> s) {
        
        // FIXE use BehaviorSubject? (once available)
        BehaviorSubject<Try<Optional<Object>>> subject = BehaviorSubject.create();
        
        final RedoSubscriber<T> parent = new RedoSubscriber<T>(s, subject, source);

        s.onSubscribe(parent.arbiter);

        Observable<?> action = manager.apply(subject);
        
        action.subscribe(new NbpToNotificationSubscriber<Object>(new Consumer<Try<Optional<Object>>>() {
            @Override
            public void accept(Try<Optional<Object>> o) {
                parent.handle(o);
            }
        }));
        
        // trigger first subscription
        parent.handle(Notification.<Object>next(0));
    }
    
    static final class RedoSubscriber<T> extends AtomicBoolean implements Observer<T> {
        /** */
        private static final long serialVersionUID = -1151903143112844287L;
        final Observer<? super T> actual;
        final BehaviorSubject<Try<Optional<Object>>> subject;
        final Observable<? extends T> source;
        final MultipleAssignmentDisposable arbiter;
        
        final AtomicInteger wip = new AtomicInteger();
        
        public RedoSubscriber(Observer<? super T> actual, BehaviorSubject<Try<Optional<Object>>> subject, Observable<? extends T> source) {
            this.actual = actual;
            this.subject = subject;
            this.source = source;
            this.arbiter = new MultipleAssignmentDisposable();
            this.lazySet(true);
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            arbiter.set(s);
        }
        
        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }
        
        @Override
        public void onError(Throwable t) {
            if (compareAndSet(false, true)) {
                subject.onNext(Try.<Optional<Object>>ofError(t));
            }
        }
        
        @Override
        public void onComplete() {
            if (compareAndSet(false, true)) {
                subject.onNext(Notification.complete());
            }
        }
        
        void handle(Try<Optional<Object>> notification) {
            if (compareAndSet(true, false)) {
                if (notification.hasError()) {
                    arbiter.dispose();
                    actual.onError(notification.error());
                } else {
                    Optional<?> o = notification.value();
                    
                    if (o.isPresent()) {
                        
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
