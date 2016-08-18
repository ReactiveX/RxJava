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

import java.util.concurrent.Callable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;

public final class ObservableMapNotification<T, R> extends AbstractObservableWithUpstream<T, ObservableSource<? extends R>> {

    final Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper;
    final Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper;
    final Callable<? extends ObservableSource<? extends R>> onCompleteSupplier;

    public ObservableMapNotification(
            ObservableSource<T> source,
            Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper,
            Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper,
                    Callable<? extends ObservableSource<? extends R>> onCompleteSupplier) {
        super(source);
        this.onNextMapper = onNextMapper;
        this.onErrorMapper = onErrorMapper;
        this.onCompleteSupplier = onCompleteSupplier;
    }
    
    @Override
    public void subscribeActual(Observer<? super ObservableSource<? extends R>> t) {
        source.subscribe(new MapNotificationSubscriber<T, R>(t, onNextMapper, onErrorMapper, onCompleteSupplier));
    }
    
    static final class MapNotificationSubscriber<T, R>
    implements Observer<T>, Disposable {
        final Observer<? super ObservableSource<? extends R>> actual;
        final Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper;
        final Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper;
        final Callable<? extends ObservableSource<? extends R>> onCompleteSupplier;
        
        Disposable s;
        
        Observable<? extends R> value;
        
        volatile boolean done;

        public MapNotificationSubscriber(Observer<? super ObservableSource<? extends R>> actual,
                Function<? super T, ? extends ObservableSource<? extends R>> onNextMapper,
                Function<? super Throwable, ? extends ObservableSource<? extends R>> onErrorMapper,
                        Callable<? extends ObservableSource<? extends R>> onCompleteSupplier) {
            this.actual = actual;
            this.onNextMapper = onNextMapper;
            this.onErrorMapper = onErrorMapper;
            this.onCompleteSupplier = onCompleteSupplier;
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
            ObservableSource<? extends R> p;
            
            try {
                p = onNextMapper.apply(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }
            
            if (p == null) {
                actual.onError(new NullPointerException("The onNext publisher returned is null"));
                return;
            }
            
            actual.onNext(p);
        }
        
        @Override
        public void onError(Throwable t) {
            ObservableSource<? extends R> p;
            
            try {
                p = onErrorMapper.apply(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }

            if (p == null) {
                actual.onError(new NullPointerException("The onError publisher returned is null"));
                return;
            }

            actual.onNext(p);
            actual.onComplete();
        }
        
        @Override
        public void onComplete() {
            ObservableSource<? extends R> p;
            
            try {
                p = onCompleteSupplier.call();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                actual.onError(e);
                return;
            }

            if (p == null) {
                actual.onError(new NullPointerException("The onComplete publisher returned is null"));
                return;
            }

            actual.onNext(p);
            actual.onComplete();
        }
    }
}
