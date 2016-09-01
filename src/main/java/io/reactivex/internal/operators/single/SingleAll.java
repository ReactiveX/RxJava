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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.internal.operators.single.AbstractSingleWithUpstreamObservable;

public final class SingleAll<T> extends AbstractSingleWithUpstreamObservable<T, Boolean> {
    final Predicate<? super T> predicate;

    public SingleAll(ObservableSource<T> source, Predicate<? super T> predicate) {
        super(source);
        this.predicate = predicate;
    }
    
    @Override
    protected void subscribeActual(SingleObserver<? super Boolean> t) {
        source.subscribe(new AllSubscriber<T>(t, predicate));
    }
    
    static final class AllSubscriber<T> implements Observer<T>, Disposable {
        final SingleObserver<? super Boolean> actual;
        final Predicate<? super T> predicate;
        
        Disposable s;
        
        boolean done;
        
        public AllSubscriber(SingleObserver<? super Boolean> actual, Predicate<? super T> predicate) {
            this.actual = actual;
            this.predicate = predicate;
        }
        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            boolean b;
            try {
                b = predicate.test(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                s.dispose();
                onError(e);
                return;
            }
            if (!b) {
                done = true;
                s.dispose();
                actual.onSuccess(false);
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
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
            actual.onSuccess(true);
        }
        
        @Override
        public void dispose() {
            s.dispose();
        }
        
        @Override
        public boolean isDisposed() {
            return s.isDisposed();
        }
    }
}
