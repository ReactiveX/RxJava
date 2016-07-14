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
import io.reactivex.internal.operators.single.SingleDelayWithCompletable.DelayWithMainSubscriber;

public final class SingleDelayWithSingle<T, U> extends Single<T> {

    final SingleConsumable<T> source;
    
    final SingleConsumable<U> other;
    
    public SingleDelayWithSingle(SingleConsumable<T> source, SingleConsumable<U> other) {
        this.source = source;
        this.other = other;
    }
    
    @Override
    protected void subscribeActual(SingleSubscriber<? super T> subscriber) {
        other.subscribe(new OtherSubscriber<T, U>(subscriber, source));
    }
    
    static final class OtherSubscriber<T, U> 
    extends AtomicReference<Disposable>
    implements SingleSubscriber<U>, Disposable {
        
        /** */
        private static final long serialVersionUID = -8565274649390031272L;

        final SingleSubscriber<? super T> actual;
        
        final SingleConsumable<T> source;

        public OtherSubscriber(SingleSubscriber<? super T> actual, SingleConsumable<T> source) {
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
        public void onSuccess(U value) {
            source.subscribe(new DelayWithMainSubscriber<T>(this, actual));
        }
        
        @Override
        public void onError(Throwable e) {
            actual.onError(e);
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
}
