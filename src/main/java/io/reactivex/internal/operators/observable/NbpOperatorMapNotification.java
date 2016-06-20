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
import io.reactivex.Observable.NbpOperator;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class NbpOperatorMapNotification<T, R> implements NbpOperator<ObservableConsumable<? extends R>, T>{

    final Function<? super T, ? extends ObservableConsumable<? extends R>> onNextMapper;
    final Function<? super Throwable, ? extends ObservableConsumable<? extends R>> onErrorMapper;
    final Supplier<? extends ObservableConsumable<? extends R>> onCompleteSupplier;

    public NbpOperatorMapNotification(Function<? super T, ? extends ObservableConsumable<? extends R>> onNextMapper, 
            Function<? super Throwable, ? extends ObservableConsumable<? extends R>> onErrorMapper, 
            Supplier<? extends ObservableConsumable<? extends R>> onCompleteSupplier) {
        this.onNextMapper = onNextMapper;
        this.onErrorMapper = onErrorMapper;
        this.onCompleteSupplier = onCompleteSupplier;
    }
    
    @Override
    public Observer<? super T> apply(Observer<? super ObservableConsumable<? extends R>> t) {
        return new MapNotificationSubscriber<T, R>(t, onNextMapper, onErrorMapper, onCompleteSupplier);
    }
    
    static final class MapNotificationSubscriber<T, R>
    implements Observer<T> {
        final Observer<? super ObservableConsumable<? extends R>> actual;
        final Function<? super T, ? extends ObservableConsumable<? extends R>> onNextMapper;
        final Function<? super Throwable, ? extends ObservableConsumable<? extends R>> onErrorMapper;
        final Supplier<? extends ObservableConsumable<? extends R>> onCompleteSupplier;
        
        Disposable s;
        
        Observable<? extends R> value;
        
        volatile boolean done;

        public MapNotificationSubscriber(Observer<? super ObservableConsumable<? extends R>> actual,
                Function<? super T, ? extends ObservableConsumable<? extends R>> onNextMapper,
                Function<? super Throwable, ? extends ObservableConsumable<? extends R>> onErrorMapper,
                Supplier<? extends ObservableConsumable<? extends R>> onCompleteSupplier) {
            this.actual = actual;
            this.onNextMapper = onNextMapper;
            this.onErrorMapper = onErrorMapper;
            this.onCompleteSupplier = onCompleteSupplier;
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
            ObservableConsumable<? extends R> p;
            
            try {
                p = onNextMapper.apply(t);
            } catch (Throwable e) {
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
            ObservableConsumable<? extends R> p;
            
            try {
                p = onErrorMapper.apply(t);
            } catch (Throwable e) {
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
            ObservableConsumable<? extends R> p;
            
            try {
                p = onCompleteSupplier.get();
            } catch (Throwable e) {
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
