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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.SubscriptionArbiter;

public final class PublisherRepeatUntil<T> implements Publisher<T> {
    final Publisher<? extends T> source;
    final BooleanSupplier until;
    public PublisherRepeatUntil(Publisher<? extends T> source, BooleanSupplier until) {
        this.source = source;
        this.until = until;
    }
    
    @Override
    public void subscribe(Subscriber<? super T> s) {
        SubscriptionArbiter sa = new SubscriptionArbiter();
        s.onSubscribe(sa);
        
        RepeatSubscriber<T> rs = new RepeatSubscriber<>(s, until, sa, source);
        rs.subscribeNext();
    }
    
    static final class RepeatSubscriber<T> extends AtomicInteger implements Subscriber<T> {
        /** */
        private static final long serialVersionUID = -7098360935104053232L;
        
        final Subscriber<? super T> actual;
        final SubscriptionArbiter sa;
        final Publisher<? extends T> source;
        final BooleanSupplier stop;
        public RepeatSubscriber(Subscriber<? super T> actual, BooleanSupplier until, SubscriptionArbiter sa, Publisher<? extends T> source) {
            this.actual = actual;
            this.sa = sa;
            this.source = source;
            this.stop = until;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            sa.setSubscription(s);
        }
        
        @Override
        public void onNext(T t) {
            actual.onNext(t);
            sa.produced(1L);
        }
        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            boolean b;
            try {
                b = stop.getAsBoolean();
            } catch (Throwable e) {
                actual.onError(e);
                return;
            }
            if (b) {
                actual.onComplete();
            } else {
                subscribeNext();
            }
        }
        
        /**
         * Subscribes to the source again via trampolining.
         */
        void subscribeNext() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                for (;;) {
                    source.subscribe(this);
                    
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                }
            }
        }
    }
}
