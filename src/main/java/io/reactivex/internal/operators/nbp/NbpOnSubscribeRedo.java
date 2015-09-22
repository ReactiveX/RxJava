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

import java.util.Optional;
import java.util.concurrent.atomic.*;
import java.util.function.Function;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.*;
import io.reactivex.internal.subscribers.nbp.NbpToNotificationSubscriber;
import io.reactivex.subjects.nbp.NbpBehaviorSubject;

public final class NbpOnSubscribeRedo<T> implements NbpOnSubscribe<T> {
    final NbpObservable<? extends T> source;
    final Function<? super NbpObservable<Try<Optional<Object>>>, ? extends NbpObservable<?>> manager;

    public NbpOnSubscribeRedo(NbpObservable<? extends T> source,
            Function<? super NbpObservable<Try<Optional<Object>>>, ? extends NbpObservable<?>> manager) {
        this.source = source;
        this.manager = manager;
    }
    
    @Override
    public void accept(NbpSubscriber<? super T> s) {
        
        // FIXE use BehaviorSubject? (once available)
        NbpBehaviorSubject<Try<Optional<Object>>> subject = NbpBehaviorSubject.create();
        
        RedoSubscriber<T> parent = new RedoSubscriber<>(s, subject, source);

        s.onSubscribe(parent.arbiter);

        NbpObservable<?> action = manager.apply(subject);
        
        action.subscribe(new NbpToNotificationSubscriber<>(parent::handle));
        
        // trigger first subscription
        parent.handle(Notification.next(0));
    }
    
    static final class RedoSubscriber<T> extends AtomicBoolean implements NbpSubscriber<T> {
        /** */
        private static final long serialVersionUID = -1151903143112844287L;
        final NbpSubscriber<? super T> actual;
        final NbpBehaviorSubject<Try<Optional<Object>>> subject;
        final NbpObservable<? extends T> source;
        final MultipleAssignmentDisposable arbiter;
        
        volatile int wip;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<RedoSubscriber> WIP =
                AtomicIntegerFieldUpdater.newUpdater(RedoSubscriber.class, "wip");
        
        public RedoSubscriber(NbpSubscriber<? super T> actual, NbpBehaviorSubject<Try<Optional<Object>>> subject, NbpObservable<? extends T> source) {
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
                subject.onNext(Try.ofError(t));
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
                        
                        if (WIP.getAndIncrement(this) == 0) {
                            int missed = 1;
                            for (;;) {
                                if (arbiter.isDisposed()) {
                                    return;
                                }
                                source.subscribe(this);
                            
                                missed = WIP.addAndGet(this, -missed);
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
