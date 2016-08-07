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

import io.reactivex.*;
import io.reactivex.disposables.*;

public final class SingleObserveOn<T> extends Single<T> {

    final SingleSource<T> source;

    final Scheduler scheduler;
    
    public SingleObserveOn(SingleSource<T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> s) {

        final CompositeDisposable mad = new CompositeDisposable();
        s.onSubscribe(mad);
        
        source.subscribe(new SingleObserver<T>() {

            @Override
            public void onError(final Throwable e) {
                mad.add(scheduler.scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        s.onError(e);
                    }
                }));
            }

            @Override
            public void onSubscribe(Disposable d) {
                mad.add(d);
            }

            @Override
            public void onSuccess(final T value) {
                mad.add(scheduler.scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        s.onSuccess(value);
                    }
                }));
            }
            
        });
    }

}
