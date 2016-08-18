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

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;

public final class SingleAmbIterable<T> extends Single<T> {

    final Iterable<? extends SingleSource<? extends T>> sources;
    
    public SingleAmbIterable(Iterable<? extends SingleSource<? extends T>> sources) {
        this.sources = sources;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> s) {
        final CompositeDisposable set = new CompositeDisposable();
        s.onSubscribe(set);
        
        Iterator<? extends SingleSource<? extends T>> iterator;
        
        try {
            iterator = sources.iterator();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            s.onError(e);
            return;
        }
        
        if (iterator == null) {
            s.onError(new NullPointerException("The iterator returned is null"));
            return;
        }

        final AtomicBoolean once = new AtomicBoolean();
        int c = 0;
        
        for (;;) {
            if (once.get()) {
                return;
            }
            
            boolean b;
            
            try {
                b = iterator.hasNext();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                s.onError(e);
                return;
            }
            
            if (once.get()) {
                return;
            }

            if (!b) {
                break;
            }
            
            if (once.get()) {
                return;
            }

            SingleSource<? extends T> s1;

            try {
                s1 = iterator.next();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                set.dispose();
                s.onError(e);
                return;
            }
            
            if (s1 == null) {
                set.dispose();
                s.onError(new NullPointerException("The single source returned by the iterator is null"));
                return;
            }
            
            s1.subscribe(new SingleObserver<T>() {

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
                public void onError(Throwable e) {
                    if (once.compareAndSet(false, true)) {
                        s.onError(e);
                    } else {
                        RxJavaPlugins.onError(e);
                    }
                }
                
            });
            c++;
        }
        
        if (c == 0 && !set.isDisposed()) {
            s.onError(new NoSuchElementException());
        }
    }

}
