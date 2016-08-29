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

import java.util.concurrent.TimeUnit;

import io.reactivex.*;
import io.reactivex.disposables.*;

public final class MaybeDelay<T> extends Maybe<T> {

    
    final MaybeSource<? extends T> source;
    final long time;
    final TimeUnit unit;
    final Scheduler scheduler;
    
    public MaybeDelay(MaybeSource<? extends T> source, long time, TimeUnit unit, Scheduler scheduler) {
        this.source = source;
        this.time = time;
        this.unit = unit;
        this.scheduler = scheduler;
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
            public void onSuccess(final T value) {
                sd.replace(scheduler.scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        s.onSuccess(value);
                    }
                }, time, unit));
            }

            @Override
            public void onComplete() {
                sd.replace(scheduler.scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        s.onComplete();
                    }
                }, time, unit));
            }

            @Override
            public void onError(Throwable e) {
                s.onError(e);
            }
            
        });
    }

}
