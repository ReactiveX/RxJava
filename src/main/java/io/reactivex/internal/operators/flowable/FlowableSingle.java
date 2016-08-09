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

import java.util.NoSuchElementException;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.*;

public final class FlowableSingle<T> extends AbstractFlowableWithUpstream<T, T> {
    
    final T defaultValue;
    
    public FlowableSingle(Publisher<T> source, T defaultValue) {
        super(source);
        this.defaultValue = defaultValue;
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new SingleElementSubscriber<T>(s, defaultValue));
    }
    
    static final class SingleElementSubscriber<T> extends DeferredScalarSubscription<T> 
    implements Subscriber<T> {
        /** */
        private static final long serialVersionUID = -5526049321428043809L;

        final T defaultValue;
        
        Subscription s;
        
        boolean done;
        
        public SingleElementSubscriber(Subscriber<? super T> actual, T defaultValue) {
            super(actual);
            this.defaultValue = defaultValue;
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
            if (value != null) {
                done = true;
                s.cancel();
                actual.onError(new IllegalArgumentException("Sequence contains more than one element!"));
                return;
            }
            value = t;
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
            T v = value;
            value = null;
            if (v == null) {
                v = defaultValue;
            }
            if (v == null) {
                actual.onError(new NoSuchElementException());
            } else {
                complete(v);
            }
        }
        
        @Override
        public void cancel() {
            super.cancel();
            s.cancel();
        }
    }
}
