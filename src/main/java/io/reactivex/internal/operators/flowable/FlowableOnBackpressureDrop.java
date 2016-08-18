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

package io.reactivex.internal.operators.flowable;

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.*;

import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

public final class FlowableOnBackpressureDrop<T> extends AbstractFlowableWithUpstream<T, T> implements Consumer<T> {

    final Consumer<? super T> onDrop;

    public FlowableOnBackpressureDrop(Publisher<T> source) {
        super(source);
        this.onDrop = this;
    }
    
    public FlowableOnBackpressureDrop(Publisher<T> source, Consumer<? super T> onDrop) {
        super(source);
        this.onDrop = onDrop;
    }
    
    @Override
    public void accept(T t) {
        // deliberately ignoring
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        this.source.subscribe(new BackpressureDropSubscriber<T>(s, onDrop));
    }
    
    static final class BackpressureDropSubscriber<T> 
    extends AtomicLong implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -6246093802440953054L;
        
        final Subscriber<? super T> actual;
        final Consumer<? super T> onDrop;
        
        Subscription s;
        
        boolean done;

        public BackpressureDropSubscriber(Subscriber<? super T> actual, Consumer<? super T> onDrop) {
            this.actual = actual;
            this.onDrop = onDrop;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            long r = get();
            if (r != 0L) {
                actual.onNext(t);
                if (r != Long.MAX_VALUE) {
                    decrementAndGet();
                }
            } else {
                try {
                    onDrop.accept(t);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    cancel();
                    onError(e);
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                return;
            }
            done = true;
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(this, n);
            }
        }
        
        @Override
        public void cancel() {
            s.cancel();
        }
    }
}
