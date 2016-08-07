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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.ArrayCompositeDisposable;

public final class CompletableObserveOn extends Completable {

    final CompletableSource source;
    
    final Scheduler scheduler;
    public CompletableObserveOn(CompletableSource source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(final CompletableObserver s) {

        final ArrayCompositeDisposable ad = new ArrayCompositeDisposable(2);
        final Scheduler.Worker w = scheduler.createWorker();
        ad.set(0, w);
        
        s.onSubscribe(ad);
        
        source.subscribe(new CompletableObserver() {

            @Override
            public void onComplete() {
                w.schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            s.onComplete();
                        } finally {
                            ad.dispose();
                        }
                    }
                });
            }

            @Override
            public void onError(final Throwable e) {
                w.schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            s.onError(e);
                        } finally {
                            ad.dispose();
                        }
                    }
                });
            }

            @Override
            public void onSubscribe(Disposable d) {
                ad.set(1, d);
            }
            
        });
    }

}
