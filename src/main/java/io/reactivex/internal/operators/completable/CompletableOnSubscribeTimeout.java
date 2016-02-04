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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.Completable.*;
import io.reactivex.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletableOnSubscribeTimeout implements CompletableOnSubscribe {
    
    final Completable source;
    final long timeout;
    final TimeUnit unit;
    final Scheduler scheduler;
    final Completable other;

    public CompletableOnSubscribeTimeout(Completable source, long timeout, 
            TimeUnit unit, Scheduler scheduler, Completable other) {
        this.source = source;
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.other = other;
    }

    @Override
    public void accept(final CompletableSubscriber s) {
        final CompositeDisposable set = new CompositeDisposable();
        s.onSubscribe(set);
        
        final AtomicBoolean once = new AtomicBoolean();
        
        Disposable timer = scheduler.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                if (once.compareAndSet(false, true)) {
                    set.clear();
                    if (other == null) {
                        s.onError(new TimeoutException());
                    } else {
                        other.subscribe(new CompletableSubscriber() {
   
                            @Override
                            public void onSubscribe(Disposable d) {
                                set.add(d);
                            }
   
                            @Override
                            public void onError(Throwable e) {
                                set.dispose();
                                s.onError(e);
                            }
   
                            @Override
                            public void onComplete() {
                                set.dispose();
                                s.onComplete();
                            }
                            
                        });
                    }
                }
            }
        }, timeout, unit);
        
        set.add(timer);
        
        source.subscribe(new CompletableSubscriber() {

            @Override
            public void onSubscribe(Disposable d) {
                set.add(d);
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
            public void onComplete() {
                if (once.compareAndSet(false, true)) {
                    set.dispose();
                    s.onComplete();
                }
            }
            
        });
    }
}