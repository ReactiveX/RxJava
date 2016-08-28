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

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.*;
import io.reactivex.observers.SerializedObserver;

public final class ObservableSampleWithObservable<T> extends AbstractObservableWithUpstream<T, T> {
    final ObservableSource<?> other;
    
    public ObservableSampleWithObservable(ObservableSource<T> source, ObservableSource<?> other) {
        super(source);
        this.other = other;
    }
    
    @Override
    public void subscribeActual(Observer<? super T> t) {
        SerializedObserver<T> serial = new SerializedObserver<T>(t);
        source.subscribe(new SamplePublisherSubscriber<T>(serial, other));
    }
    
    static final class SamplePublisherSubscriber<T> extends AtomicReference<T> 
    implements Observer<T>, Disposable {
        /** */
        private static final long serialVersionUID = -3517602651313910099L;

        final Observer<? super T> actual;
        final ObservableSource<?> sampler;
        
        final AtomicReference<Disposable> other = new AtomicReference<Disposable>();
        
        Disposable s;
        
        public SamplePublisherSubscriber(Observer<? super T> actual, ObservableSource<?> other) {
            this.actual = actual;
            this.sampler = other;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
                if (other.get() == null) {
                    sampler.subscribe(new SamplerSubscriber<T>(this));
                }
            }
        }
        
        @Override
        public void onNext(T t) {
            lazySet(t);
        }
        
        @Override
        public void onError(Throwable t) {
            DisposableHelper.dispose(other);
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            DisposableHelper.dispose(other);
            actual.onComplete();
        }

        boolean setOther(Disposable o) {
            if (other.get() == null) {
                if (other.compareAndSet(null, o)) {
                    return true;
                }
                o.dispose();
            }
            return false;
        }
        
        @Override
        public void dispose() {
            DisposableHelper.dispose(other);
            s.dispose();
        }

        @Override
        public boolean isDisposed() {
            return other.get() == DisposableHelper.DISPOSED;
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
    
    static final class SamplerSubscriber<T> implements Observer<Object> {
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
