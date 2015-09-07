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

package io.reactivex.internal.operators;

import java.util.Optional;
import java.util.concurrent.atomic.*;
import java.util.function.Function;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.internal.subscribers.ToNotificationSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionArbiter;
import io.reactivex.subjects.*;

public final class PublisherRedo<T> implements Publisher<T> {
    final Publisher<? extends T> source;
    final Function<? super Observable<Try<Optional<Object>>>, ? extends Publisher<?>> manager;

    public PublisherRedo(Publisher<? extends T> source,
            Function<? super Observable<Try<Optional<Object>>>, ? extends Publisher<?>> manager) {
        this.source = source;
        this.manager = manager;
    }
    
    @Override
    public void subscribe(Subscriber<? super T> s) {
        
        // FIXE use BehaviorSubject? (once available)
        BehaviorSubject<Try<Optional<Object>>> subject = BehaviorSubject.create();
        
        RedoSubscriber<T> parent = new RedoSubscriber<>(s, subject, source);

        s.onSubscribe(parent.arbiter);

        Publisher<?> action = manager.apply(subject);
        
        action.subscribe(new ToNotificationSubscriber<>(parent::handle));
        
        // trigger first subscription
        parent.handle(Notification.next(0));
    }
    
    static final class RedoSubscriber<T> extends AtomicBoolean implements Subscriber<T> {
        /** */
        private static final long serialVersionUID = -1151903143112844287L;
        final Subscriber<? super T> actual;
        final BehaviorSubject<Try<Optional<Object>>> subject;
        final Publisher<? extends T> source;
        final SubscriptionArbiter arbiter;
        
        volatile int wip;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<RedoSubscriber> WIP =
                AtomicIntegerFieldUpdater.newUpdater(RedoSubscriber.class, "wip");
        
        public RedoSubscriber(Subscriber<? super T> actual, BehaviorSubject<Try<Optional<Object>>> subject, Publisher<? extends T> source) {
            this.actual = actual;
            this.subject = subject;
            this.source = source;
            this.arbiter = new SubscriptionArbiter();
            this.lazySet(true);
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            arbiter.setSubscription(s);
        }
        
        @Override
        public void onNext(T t) {
            actual.onNext(t);
            arbiter.produced(1L);
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
                    arbiter.cancel();
                    actual.onError(notification.error());
                } else {
                    Optional<?> o = notification.value();
                    
                    if (o.isPresent()) {
                        
                        if (WIP.getAndIncrement(this) == 0) {
                            int missed = 1;
                            for (;;) {
                                if (arbiter.isCancelled()) {
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
                        arbiter.cancel();
                        actual.onComplete();
                    }
                }
            }
        }
    }
}
