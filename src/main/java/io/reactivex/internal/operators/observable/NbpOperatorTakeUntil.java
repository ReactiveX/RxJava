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

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.Observable.*;
import io.reactivex.disposables.*;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.observers.SerializedObserver;

public final class NbpOperatorTakeUntil<T, U> implements NbpOperator<T, T> {
    final Observable<? extends U> other;
    public NbpOperatorTakeUntil(Observable<? extends U> other) {
        this.other = other;
    }
    @Override
    public Observer<? super T> apply(Observer<? super T> child) {
        final SerializedObserver<T> serial = new SerializedObserver<T>(child);
        
        final ArrayCompositeResource<Disposable> frc = new ArrayCompositeResource<Disposable>(2, Disposables.consumeAndDispose());
        
        final TakeUntilSubscriber<T> tus = new TakeUntilSubscriber<T>(serial, frc); 
        
        other.subscribe(new Observer<U>() {
            @Override
            public void onSubscribe(Disposable s) {
                frc.setResource(1, s);
            }
            @Override
            public void onNext(U t) {
                frc.dispose();
                if (tus.compareAndSet(false, true)) {
                    EmptyDisposable.complete(serial);
                } else {
                    serial.onComplete();
                }
            }
            @Override
            public void onError(Throwable t) {
                frc.dispose();
                if (tus.compareAndSet(false, true)) {
                    EmptyDisposable.error(t, serial);
                } else {
                    serial.onError(t);
                }
            }
            @Override
            public void onComplete() {
                frc.dispose();
                if (tus.compareAndSet(false, true)) {
                    EmptyDisposable.complete(serial);
                } else {
                    serial.onComplete();
                }
            }
        });
        
        return tus;
    }
    
    static final class TakeUntilSubscriber<T> extends AtomicBoolean implements Observer<T>, Disposable {
        /** */
        private static final long serialVersionUID = 3451719290311127173L;
        final Observer<? super T> actual;
        final ArrayCompositeResource<Disposable> frc;
        
        Disposable s;
        
        public TakeUntilSubscriber(Observer<? super T> actual, ArrayCompositeResource<Disposable> frc) {
            this.actual = actual;
            this.frc = frc;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            if (frc.setResource(0, s)) {
                if (compareAndSet(false, true)) {
                    actual.onSubscribe(this);
                }
            }
        }
        
        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }
        
        @Override
        public void onError(Throwable t) {
            frc.dispose();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            frc.dispose();
            actual.onComplete();
        }
        
        @Override
        public void dispose() {
            frc.dispose();
        }
    }
}
