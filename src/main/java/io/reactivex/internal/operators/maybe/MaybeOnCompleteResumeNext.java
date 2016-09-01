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

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.functions.Supplier;

public final class MaybeOnCompleteResumeNext<T> extends Maybe<T> {
    final MaybeSource<? extends T> source;
    
    final Supplier<? extends MaybeSource<? extends T>> nextSupplier;
    
    public MaybeOnCompleteResumeNext(MaybeSource<? extends T> source,
            Supplier<? extends MaybeSource<? extends T>> nextSupplier) {
        this.source = source;
        this.nextSupplier = nextSupplier;
    }

    @Override
    protected void subscribeActual(final MaybeObserver<? super T> s) {

        final SerialDisposable sd = new SerialDisposable();
        s.onSubscribe(sd);
        
        source.subscribe(new MaybeObserver<T>() {

            @Override
            public void onSubscribe(Disposable d) {
                sd.replace(d);
            }

            @Override
            public void onSuccess(T value) {
                s.onSuccess(value);
            }

            @Override
            public void onComplete() {
                MaybeSource<? extends T> next;
                
                try {
                    next = nextSupplier.get();
                } catch (Throwable ex) {
                    s.onError(ex);
                    return;
                }
                
                if (next == null) {
                    NullPointerException npe = new NullPointerException("The next Maybe supplied was null");
                    s.onError(npe);
                    return;
                }
                
                next.subscribe(new MaybeObserver<T>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        sd.replace(d);
                    }

                    @Override
                    public void onSuccess(T value) {
                        s.onSuccess(value);
                    }

                    @Override
                    public void onComplete() {
                        s.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }
                    
                });
            }

            @Override
            public void onError(Throwable e) {
                s.onError(e);
            }
            
        });
    }

}
