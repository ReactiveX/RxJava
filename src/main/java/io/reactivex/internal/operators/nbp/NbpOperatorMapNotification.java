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

import java.util.function.*;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class NbpOperatorMapNotification<T, R> implements NbpOperator<NbpObservable<? extends R>, T>{

    final Function<? super T, ? extends NbpObservable<? extends R>> onNextMapper;
    final Function<? super Throwable, ? extends NbpObservable<? extends R>> onErrorMapper;
    final Supplier<? extends NbpObservable<? extends R>> onCompleteSupplier;

    public NbpOperatorMapNotification(Function<? super T, ? extends NbpObservable<? extends R>> onNextMapper, 
            Function<? super Throwable, ? extends NbpObservable<? extends R>> onErrorMapper, 
            Supplier<? extends NbpObservable<? extends R>> onCompleteSupplier) {
        this.onNextMapper = onNextMapper;
        this.onErrorMapper = onErrorMapper;
        this.onCompleteSupplier = onCompleteSupplier;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super NbpObservable<? extends R>> t) {
        return new MapNotificationSubscriber<>(t, onNextMapper, onErrorMapper, onCompleteSupplier);
    }
    
    static final class MapNotificationSubscriber<T, R>
    implements NbpSubscriber<T> {
        final NbpSubscriber<? super NbpObservable<? extends R>> actual;
        final Function<? super T, ? extends NbpObservable<? extends R>> onNextMapper;
        final Function<? super Throwable, ? extends NbpObservable<? extends R>> onErrorMapper;
        final Supplier<? extends NbpObservable<? extends R>> onCompleteSupplier;
        
        Disposable s;
        
        NbpObservable<? extends R> value;
        
        volatile boolean done;

        public MapNotificationSubscriber(NbpSubscriber<? super NbpObservable<? extends R>> actual,
                Function<? super T, ? extends NbpObservable<? extends R>> onNextMapper,
                Function<? super Throwable, ? extends NbpObservable<? extends R>> onErrorMapper,
                Supplier<? extends NbpObservable<? extends R>> onCompleteSupplier) {
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
            NbpObservable<? extends R> p;
            
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
            NbpObservable<? extends R> p;
            
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
            NbpObservable<? extends R> p;
            
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
