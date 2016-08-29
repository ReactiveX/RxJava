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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.disposables.*;

public final class MaybeTimeout<T> extends Maybe<T> {

    final MaybeSource<T> source;
    
    final long timeout;
    
    final TimeUnit unit;
    
    final Scheduler scheduler;
    
    final MaybeSource<? extends T> other;
    
    public MaybeTimeout(MaybeSource<T> source, long timeout, TimeUnit unit, Scheduler scheduler,
                         MaybeSource<? extends T> other) {
        this.source = source;
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.other = other;
    }

    @Override
    protected void subscribeActual(final MaybeObserver<? super T> s) {

        final CompositeDisposable set = new CompositeDisposable();
        s.onSubscribe(set);
        
        final AtomicBoolean once = new AtomicBoolean();
        
        Disposable timer = scheduler.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                if (once.compareAndSet(false, true)) {
                    if (other != null) {
                        set.clear();
                        other.subscribe(new MaybeObserver<T>() {

                            @Override
                            public void onError(Throwable e) {
                                set.dispose();
                                s.onError(e);
                            }

                            @Override
                            public void onSubscribe(Disposable d) {
                                set.add(d);
                            }

                            @Override
                            public void onSuccess(T value) {
                                set.dispose();
                                s.onSuccess(value);
                            }

                            @Override
                            public void onComplete() {
                                set.dispose();
                                s.onComplete();
                            }
                        });
                    } else {
                        set.dispose();
                        s.onError(new TimeoutException());
                    }
                }
            }
        }, timeout, unit);
        
        set.add(timer);
        
        source.subscribe(new MaybeObserver<T>() {

            @Override
            public void onError(Throwable e) {
                if (once.compareAndSet(false, true)) {
                    set.dispose();
                    s.onError(e);
                }
            }

            @Override
            public void onSubscribe(Disposable d) {
                set.add(d);
            }

            @Override
            public void onSuccess(T value) {
                if (once.compareAndSet(false, true)) {
                    set.dispose();
                    s.onSuccess(value);
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
