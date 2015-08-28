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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.subscribers.SerializedSubscriber;

public final class OperatorSamplePublisher<T> implements Operator<T, T> {
    final Publisher<?> other;
    
    public OperatorSamplePublisher(Publisher<?> other) {
        this.other = other;
    }
    
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        SerializedSubscriber<T> serial = new SerializedSubscriber<>(t);
        return new SamplePublisherSubscriber<>(serial, other);
    }
    
    static final class SamplePublisherSubscriber<T> extends AtomicReference<T> implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -3517602651313910099L;

        final Subscriber<? super T> actual;
        final Publisher<?> sampler;
        
        volatile long requested;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<SamplePublisherSubscriber> REQUESTED =
                AtomicLongFieldUpdater.newUpdater(SamplePublisherSubscriber.class, "requested");

        volatile Subscription other;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<SamplePublisherSubscriber, Subscription> OTHER =
                AtomicReferenceFieldUpdater.newUpdater(SamplePublisherSubscriber.class, Subscription.class, "other");
        
        static final Subscription CANCELLED = new Subscription() {
            @Override
            public void request(long n) {
                
            }
            
            @Override
            public void cancel() {
                
            }
        };
        
        Subscription s;
        
        public SamplePublisherSubscriber(Subscriber<? super T> actual, Publisher<?> other) {
            this.actual = actual;
            this.sampler = other;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validateSubscription(this.s, s)) {
                return;
            }
            
            this.s = s;
            actual.onSubscribe(this);
            if (other == null) {
                sampler.subscribe(new SamplerSubscriber<>(this));
                s.request(Long.MAX_VALUE);
            }
            
        }
        
        @Override
        public void onNext(T t) {
            lazySet(t);
        }
        
        @Override
        public void onError(Throwable t) {
            cancelOther();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            cancelOther();
            actual.onComplete();
        }
        
        void cancelOther() {
            Subscription o = other;
            if (o != CANCELLED) {
                o = OTHER.getAndSet(this, CANCELLED);
                if (o != CANCELLED && o != null) {
                    o.cancel();
                }
            }
        }
        
        boolean setOther(Subscription o) {
            if (other == null) {
                if (OTHER.compareAndSet(this, null, o)) {
                    return true;
                }
                o.cancel();
            }
            return false;
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            
            BackpressureHelper.add(REQUESTED, this, n);
        }
        
        @Override
        public void cancel() {
            cancelOther();
            s.cancel();
        }
        
        public void error(Throwable e) {
            cancel();
            actual.onError(e);
        }
        
        public void complete() {
            cancel();
            actual.onComplete();
        }
        
        public void emit() {
            T value = getAndSet(null);
            if (value != null) {
                long r = requested;
                if (r != 0L) {
                    actual.onNext(value);
                    if (r != Long.MAX_VALUE) {
                        REQUESTED.decrementAndGet(this);
                    }
                } else {
                    cancel();
                    actual.onError(new IllegalStateException("Couldn't emit value due to lack of requests!"));
                }
            }
        }
    }
    
    static final class SamplerSubscriber<T> implements Subscriber<Object> {
        final SamplePublisherSubscriber<T> parent;
        public SamplerSubscriber(SamplePublisherSubscriber<T> parent) {
            this.parent = parent;
            
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (parent.setOther(s)) {
                s.request(Long.MAX_VALUE);
            }
        }
        
        @Override
        public void onNext(Object t) {
            parent.emit();
        }
        
        @Override
        public void onError(Throwable t) {
            parent.error(t);
        }
        
        @Override
        public void onComplete() {
            parent.complete();
        }
    }
}
