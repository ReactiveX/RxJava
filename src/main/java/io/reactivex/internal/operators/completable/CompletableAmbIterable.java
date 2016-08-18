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

package io.reactivex.internal.operators.completable;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletableAmbIterable extends Completable {

    final Iterable<? extends CompletableSource> sources;
    
    public CompletableAmbIterable(Iterable<? extends CompletableSource> sources) {
        this.sources = sources;
    }
    
    @Override
    protected void subscribeActual(final CompletableObserver s) {
        final CompositeDisposable set = new CompositeDisposable();
        s.onSubscribe(set);

        Iterator<? extends CompletableSource> it;
        
        try {
            it = sources.iterator();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            s.onError(e);
            return;
        }
        
        if (it == null) {
            s.onError(new NullPointerException("The iterator returned is null"));
            return;
        }
        
        boolean empty = true;

        final AtomicBoolean once = new AtomicBoolean();
        
        CompletableObserver inner = new CompletableObserver() {
            @Override
            public void onComplete() {
                if (once.compareAndSet(false, true)) {
                    set.dispose();
                    s.onComplete();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (once.compareAndSet(false, true)) {
                    set.dispose();
                    s.onError(e);
                } else {
                    RxJavaPlugins.onError(e);
                }
            }

            @Override
            public void onSubscribe(Disposable d) {
                set.add(d);
            }
            
        };

        for (;;) {
            if (once.get() || set.isDisposed()) {
                return;
            }
            
            boolean b;
            
            try {
                b = it.hasNext();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                if (once.compareAndSet(false, true)) {
                    set.dispose();
                    s.onError(e);
                } else {
                    RxJavaPlugins.onError(e);
                }
                return;
            }
            
            if (!b) {
                if (empty) {
                    s.onComplete();
                }
                break;
            }
            
            empty = false;
            
            if (once.get() || set.isDisposed()) {
                return;
            }

            CompletableSource c;
            
            try {
                c = it.next();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                if (once.compareAndSet(false, true)) {
                    set.dispose();
                    s.onError(e);
                } else {
                    RxJavaPlugins.onError(e);
                }
                return;
            }
            
            if (c == null) {
                NullPointerException npe = new NullPointerException("One of the sources is null");
                if (once.compareAndSet(false, true)) {
                    set.dispose();
                    s.onError(npe);
                } else {
                    RxJavaPlugins.onError(npe);
                }
                return;
            }
            
            if (once.get() || set.isDisposed()) {
                return;
            }
            
            // no need to have separate subscribers because inner is stateless
            c.subscribe(inner);
        }
    }

}
