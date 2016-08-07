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

package io.reactivex.internal.operators.single;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

public final class SingleDelayWithCompletable<T> extends Single<T> {

    final SingleSource<T> source;
    
    final CompletableSource other;
    
    public SingleDelayWithCompletable(SingleSource<T> source, CompletableSource other) {
        this.source = source;
        this.other = other;
    }
    
    @Override
    protected void subscribeActual(SingleObserver<? super T> subscriber) {
        other.subscribe(new OtherObserver<T>(subscriber, source));
    }
    
    static final class OtherObserver<T>
    extends AtomicReference<Disposable>
    implements CompletableObserver, Disposable {
        
        /** */
        private static final long serialVersionUID = -8565274649390031272L;

        final SingleObserver<? super T> actual;
        
        final SingleSource<T> source;

        public OtherObserver(SingleObserver<? super T> actual, SingleSource<T> source) {
            this.actual = actual;
            this.source = source;
        }
        
        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.set(this, d)) {
                
                actual.onSubscribe(this);
            }
        }
        
        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }
        
        @Override
        public void onComplete() {
            source.subscribe(new DelayWithMainObserver<T>(this, actual));
        }
        
        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }
        
        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }
    }
    
    static final class DelayWithMainObserver<T> implements SingleObserver<T> {
        
        final AtomicReference<Disposable> parent;
        
        final SingleObserver<? super T> actual;

        public DelayWithMainObserver(AtomicReference<Disposable> parent, SingleObserver<? super T> actual) {
            this.parent = parent;
            this.actual = actual;
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.replace(parent, d);
        }

        @Override
        public void onSuccess(T value) {
            actual.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }
    }
}
