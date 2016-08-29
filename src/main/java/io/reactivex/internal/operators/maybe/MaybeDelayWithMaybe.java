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

package io.reactivex.internal.operators.maybe;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.operators.maybe.MaybeDelayWithCompletable.DelayWithMainObserver;

public final class MaybeDelayWithMaybe<T, U> extends Maybe<T> {

    final MaybeSource<T> source;
    
    final MaybeSource<U> other;
    
    public MaybeDelayWithMaybe(MaybeSource<T> source, MaybeSource<U> other) {
        this.source = source;
        this.other = other;
    }
    
    @Override
    protected void subscribeActual(MaybeObserver<? super T> subscriber) {
        other.subscribe(new OtherObserver<T, U>(subscriber, source));
    }
    
    static final class OtherObserver<T, U>
    extends AtomicReference<Disposable>
    implements MaybeObserver<U>, Disposable {
        
        /** */
        private static final long serialVersionUID = -8565274649390031272L;

        final MaybeObserver<? super T> actual;

        final MaybeSource<T> source;

        boolean done;

        public OtherObserver(MaybeObserver<? super T> actual, MaybeSource<T> source) {
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
            if (done) {
                return;
            }
            done = true;
            source.subscribe(new DelayWithMainObserver<T>(this, actual));
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            source.subscribe(new DelayWithMainObserver<T>(this, actual));
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
