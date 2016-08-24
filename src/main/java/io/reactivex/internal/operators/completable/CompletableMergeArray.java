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

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletableMergeArray extends Completable {
    final CompletableSource[] sources;
    
    public CompletableMergeArray(CompletableSource[] sources) {
        this.sources = sources;
    }
    
    @Override
    public void subscribeActual(final CompletableObserver s) {
        final CompositeDisposable set = new CompositeDisposable();
        final AtomicInteger wip = new AtomicInteger(sources.length + 1);
        final AtomicBoolean once = new AtomicBoolean();
        
        s.onSubscribe(set);
        
        for (CompletableSource c : sources) {
            if (set.isDisposed()) {
                return;
            }
            
            if (c == null) {
                set.dispose();
                NullPointerException npe = new NullPointerException("A completable source is null");
                if (once.compareAndSet(false, true)) {
                    s.onError(npe);
                } else {
                    RxJavaPlugins.onError(npe);
                }
                return;
            }
            
            c.subscribe(new CompletableObserver() {
                @Override
                public void onSubscribe(Disposable d) {
                    set.add(d);
                }

                @Override
                public void onError(Throwable e) {
                    set.dispose();
                    if (once.compareAndSet(false, true)) {
                        s.onError(e);
                    } else {
                        RxJavaPlugins.onError(e);
                    }
                }

                @Override
                public void onComplete() {
                    if (wip.decrementAndGet() == 0) {
                        if (once.compareAndSet(false, true)) {
                            s.onComplete();
                        }
                    }
                }
                
            });
        }
        
        if (wip.decrementAndGet() == 0) {
            if (once.compareAndSet(false, true)) {
                s.onComplete();
            }
        }
    }
}
