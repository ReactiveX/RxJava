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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

public final class ObservableMaterialize<T> extends AbstractObservableWithUpstream<T, Try<Optional<T>>> {
    
    
    public ObservableMaterialize(ObservableSource<T> source) {
        super(source);
    }

    @Override
    public void subscribeActual(Observer<? super Try<Optional<T>>> t) {
        source.subscribe(new MaterializeSubscriber<T>(t));
    }
    
    static final class MaterializeSubscriber<T> implements Observer<T>, Disposable {
        final Observer<? super Try<Optional<T>>> actual;
        
        Disposable s;
        
        volatile boolean done;
        
        public MaterializeSubscriber(Observer<? super Try<Optional<T>>> actual) {
            this.actual = actual;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }
        

        @Override
        public void dispose() {
            s.dispose();
        }
        
        @Override
        public boolean isDisposed() {
            return s.isDisposed();
        }

        @Override
        public void onNext(T t) {
            actual.onNext(Notification.next(t));
        }
        
        void tryEmit(Try<Optional<T>> v) {
                
        }
        
        @Override
        public void onError(Throwable t) {
            Try<Optional<T>> v = Notification.error(t);
            actual.onNext(v);
            actual.onComplete();
        }
        
        @Override
        public void onComplete() {
            Try<Optional<T>> v = Notification.complete();
            
            actual.onNext(v);
            actual.onComplete();
        }
    }
}
