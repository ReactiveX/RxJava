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

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class MaybeAmbArray<T> extends Maybe<T> {

    final MaybeSource<? extends T>[] sources;
    
    public MaybeAmbArray(MaybeSource<? extends T>[] sources) {
        this.sources = sources;
    }

    @Override
    protected void subscribeActual(final MaybeObserver<? super T> s) {

        final AtomicBoolean once = new AtomicBoolean();
        final CompositeDisposable set = new CompositeDisposable();
        s.onSubscribe(set);
        
        for (MaybeSource<? extends T> s1 : sources) {
            if (once.get()) {
                return;
            }
            
            if (s1 == null) {
                set.dispose();
                Throwable e = new NullPointerException("One of the sources is null");
                if (once.compareAndSet(false, true)) {
                    s.onError(e);
                } else {
                    RxJavaPlugins.onError(e);
                }
                return;
            }
            
            s1.subscribe(new MaybeObserver<T>() {

                @Override
                public void onSubscribe(Disposable d) {
                    set.add(d);
                }

                @Override
                public void onSuccess(T value) {
                    if (once.compareAndSet(false, true)) {
                        s.onSuccess(value);
                    }
                }

                @Override
                public void onComplete() {
                    if (once.compareAndSet(false, true)) {
                        s.onComplete();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    if (once.compareAndSet(false, true)) {
                        s.onError(e);
                    } else {
                        RxJavaPlugins.onError(e);
                    }
                }
                
            });
        }
    }

}
