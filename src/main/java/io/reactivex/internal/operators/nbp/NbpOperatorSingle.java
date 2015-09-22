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

import java.util.NoSuchElementException;

import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class NbpOperatorSingle<T> implements NbpOperator<T, T> {
    
    static final NbpOperatorSingle<Object> NO_DEFAULT = new NbpOperatorSingle<>(null);
    
    @SuppressWarnings("unchecked")
    public static <T> NbpOperatorSingle<T> instanceNoDefault() {
        return (NbpOperatorSingle<T>)NO_DEFAULT;
    }
    
    final T defaultValue;
    public NbpOperatorSingle(T defaultValue) {
        this.defaultValue = defaultValue;
    }
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super T> t) {
        return new SingleElementSubscriber<>(t, defaultValue);
    }
    
    static final class SingleElementSubscriber<T> implements NbpSubscriber<T> {
        final NbpSubscriber<? super T> actual;
        final T defaultValue;
        
        Disposable s;
        
        T value;
        
        boolean done;
        
        public SingleElementSubscriber(NbpSubscriber<? super T> actual, T defaultValue) {
            this.actual = actual;
            this.defaultValue = defaultValue;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            actual.onSubscribe(s);
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            if (value != null) {
                done = true;
                s.dispose();
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
                actual.onNext(v);
                actual.onComplete();
            }
        }
    }
}
