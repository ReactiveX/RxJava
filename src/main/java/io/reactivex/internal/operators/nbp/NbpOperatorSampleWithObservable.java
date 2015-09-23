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

import java.util.concurrent.atomic.*;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.subscribers.nbp.NbpSerializedSubscriber;

public final class NbpOperatorSampleWithObservable<T> implements NbpOperator<T, T> {
    final NbpObservable<?> other;
    
    public NbpOperatorSampleWithObservable(NbpObservable<?> other) {
        this.other = other;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super T> t) {
        NbpSerializedSubscriber<T> serial = new NbpSerializedSubscriber<>(t);
        return new SamplePublisherSubscriber<>(serial, other);
    }
    
    static final class SamplePublisherSubscriber<T> extends AtomicReference<T> 
    implements NbpSubscriber<T>, Disposable {
        /** */
        private static final long serialVersionUID = -3517602651313910099L;

        final NbpSubscriber<? super T> actual;
        final NbpObservable<?> sampler;
        
        volatile Disposable other;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<SamplePublisherSubscriber, Disposable> OTHER =
                AtomicReferenceFieldUpdater.newUpdater(SamplePublisherSubscriber.class, Disposable.class, "other");
        
        static final Disposable CANCELLED = () -> { };
        
        Disposable s;
        
        public SamplePublisherSubscriber(NbpSubscriber<? super T> actual, NbpObservable<?> other) {
            this.actual = actual;
            this.sampler = other;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            
            this.s = s;
            actual.onSubscribe(this);
            if (other == null) {
                sampler.subscribe(new SamplerSubscriber<>(this));
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
            Disposable o = other;
            if (o != CANCELLED) {
                o = OTHER.getAndSet(this, CANCELLED);
                if (o != CANCELLED && o != null) {
                    o.dispose();
                }
            }
        }
        
        boolean setOther(Disposable o) {
            if (other == null) {
                if (OTHER.compareAndSet(this, null, o)) {
                    return true;
                }
                o.dispose();
            }
            return false;
        }
        
        @Override
        public void dispose() {
            cancelOther();
            s.dispose();
        }
        
        public void error(Throwable e) {
            dispose();
            actual.onError(e);
        }
        
        public void complete() {
            dispose();
            actual.onComplete();
        }
        
        public void emit() {
            T value = getAndSet(null);
            if (value != null) {
                actual.onNext(value);
            }
        }
    }
    
    static final class SamplerSubscriber<T> implements NbpSubscriber<Object> {
        final SamplePublisherSubscriber<T> parent;
        public SamplerSubscriber(SamplePublisherSubscriber<T> parent) {
            this.parent = parent;
            
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            parent.setOther(s);
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
