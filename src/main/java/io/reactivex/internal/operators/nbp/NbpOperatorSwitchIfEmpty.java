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

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.*;

public final class NbpOperatorSwitchIfEmpty<T> implements NbpOperator<T, T> {
    final NbpObservable<? extends T> other;
    public NbpOperatorSwitchIfEmpty(NbpObservable<? extends T> other) {
        this.other = other;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super T> t) {
        SwitchIfEmptySubscriber<T> parent = new SwitchIfEmptySubscriber<>(t, other);
        t.onSubscribe(parent.arbiter);
        return parent;
    }
    
    static final class SwitchIfEmptySubscriber<T> implements NbpSubscriber<T> {
        final NbpSubscriber<? super T> actual;
        final NbpObservable<? extends T> other;
        final SerialDisposable arbiter;
        
        boolean empty;
        
        public SwitchIfEmptySubscriber(NbpSubscriber<? super T> actual, NbpObservable<? extends T> other) {
            this.actual = actual;
            this.other = other;
            this.empty = true;
            this.arbiter = new SerialDisposable();
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            arbiter.set(s);
        }
        
        @Override
        public void onNext(T t) {
            if (empty) {
                empty = false;
            }
            actual.onNext(t);
        }
        
        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (empty) {
                empty = false;
                other.subscribe(this);
            } else {
                actual.onComplete();
            }
        }
    }
}
