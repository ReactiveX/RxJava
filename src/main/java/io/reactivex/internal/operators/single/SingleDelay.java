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

import java.util.concurrent.TimeUnit;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.SequentialDisposable;

public final class SingleDelay<T> extends Single<T> {

    
    final SingleSource<? extends T> source;
    final long time;
    final TimeUnit unit;
    final Scheduler scheduler;
    
    public SingleDelay(SingleSource<? extends T> source, long time, TimeUnit unit, Scheduler scheduler) {
        this.source = source;
        this.time = time;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> s) {

        final SequentialDisposable sd = new SequentialDisposable();
        s.onSubscribe(sd);
        source.subscribe(new SingleObserver<T>() {
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
            public void onError(Throwable e) {
                s.onError(e);
            }
            
        });
    }

}
