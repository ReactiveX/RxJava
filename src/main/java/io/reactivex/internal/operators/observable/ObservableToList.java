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

import java.util.*;
import java.util.concurrent.Callable;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.disposables.*;

public final class ObservableToList<T, U extends Collection<? super T>> 
extends AbstractObservableWithUpstream<T, U> {
    
    final Callable<U> collectionSupplier;
    
    public ObservableToList(ObservableSource<T> source, final int defaultCapacityHint) {
        super(source);
        this.collectionSupplier = new Callable<U>() {
            @Override
            @SuppressWarnings("unchecked")
            public U call() throws Exception {
                return (U)new ArrayList<T>(defaultCapacityHint);
            }
        };
    }

    public ObservableToList(ObservableSource<T> source, Callable<U> collectionSupplier) {
        super(source);
        this.collectionSupplier = collectionSupplier;
    }

    @Override
    public void subscribeActual(Observer<? super U> t) {
        U coll;
        try {
            coll = collectionSupplier.call();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, t);
            return;
        }
        source.subscribe(new ToListSubscriber<T, U>(t, coll));
    }
    
    static final class ToListSubscriber<T, U extends Collection<? super T>> implements Observer<T>, Disposable {
        U collection;
        final Observer<? super U> actual;
        
        Disposable s;
        
        public ToListSubscriber(Observer<? super U> actual, U collection) {
            this.actual = actual;
            this.collection = collection;
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
            collection.add(t);
        }
        
        @Override
        public void onError(Throwable t) {
            collection = null;
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            U c = collection;
            collection = null;
            actual.onNext(c);
            actual.onComplete();
        }
    }
}
