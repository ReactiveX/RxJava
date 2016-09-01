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
import io.reactivex.internal.operators.maybe.AbstractMaybeWithUpstreamObservable;

public final class ObservableElementAt<T> extends AbstractMaybeWithUpstreamObservable<T, T> {
    final long index;
    public ObservableElementAt(ObservableSource<T> source, long index) {
        super(source);
        this.index = index;
    }

    @Override
    public void subscribeActual(MaybeObserver<? super T> t) {
        source.subscribe(new ElementAtSubscriber<T>(t, index));
    }
    
    static final class ElementAtSubscriber<T> implements Observer<T>, Disposable {
        final MaybeObserver<? super T> actual;
        final long index;
        
        Disposable s;
        
        long count;
        
        boolean done;
        
        public ElementAtSubscriber(MaybeObserver<? super T> actual, long index) {
            this.actual = actual;
            this.index = index;
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
            if (done) {
                return;
            }
            long c = count;
            if (c == index) {
                done = true;
                s.dispose();
                actual.onSuccess(t);
                return;
            }
            count = c + 1;
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
            if (index <= count && !done) {
                done = true;
                actual.onComplete();
            }
        }
    }
}
