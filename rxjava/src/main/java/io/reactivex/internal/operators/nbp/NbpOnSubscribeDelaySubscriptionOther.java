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

package io.reactivex.internal.operators.nbp;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Delays the subscription to the main source until the other
 * observable fires an event or completes.
 * @param <T> the main type
 * @param <U> the other value type, ignored
 */
public final class NbpOnSubscribeDelaySubscriptionOther<T, U> implements NbpOnSubscribe<T> {
    final NbpObservable<? extends T> main;
    final NbpObservable<U> other;
    
    public NbpOnSubscribeDelaySubscriptionOther(NbpObservable<? extends T> main, NbpObservable<U> other) {
        this.main = main;
        this.other = other;
    }
    
    @Override
    public void accept(final NbpSubscriber<? super T> child) {
        final SerialDisposable serial = new SerialDisposable();
        child.onSubscribe(serial);
        
        NbpSubscriber<U> otherSubscriber = new NbpSubscriber<U>() {
            boolean done;
            @Override
            public void onSubscribe(Disposable d) {
                serial.set(d);
            }
            
            @Override
            public void onNext(U t) {
                onComplete();
            }
            
            @Override
            public void onError(Throwable e) {
                if (done) {
                    RxJavaPlugins.onError(e);
                    return;
                }
                done = true;
                child.onError(e);
            }
            
            @Override
            public void onComplete() {
                if (done) {
                    return;
                }
                done = true;
                
                main.unsafeSubscribe(new NbpSubscriber<T>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        serial.set(d);
                    }
                    
                    @Override
                    public void onNext(T value) {
                        child.onNext(value);
                    }
                    
                    @Override
                    public void onError(Throwable e) {
                        child.onError(e);
                    }
                    
                    @Override
                    public void onComplete() {
                        child.onComplete();
                    }
                });
            }
        };
        
        other.unsafeSubscribe(otherSubscriber);
    }
}