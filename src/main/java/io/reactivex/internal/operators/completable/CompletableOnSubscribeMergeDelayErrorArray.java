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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Completable;
import io.reactivex.Completable.*;
import io.reactivex.disposables.*;

public final class CompletableOnSubscribeMergeDelayErrorArray implements CompletableOnSubscribe {
    final Completable[] sources;
    
    public CompletableOnSubscribeMergeDelayErrorArray(Completable[] sources) {
        this.sources = sources;
    }
    
    @Override
    public void accept(final CompletableSubscriber s) {
        final CompositeDisposable set = new CompositeDisposable();
        final AtomicInteger wip = new AtomicInteger(sources.length + 1);
        
        final Queue<Throwable> q = new ConcurrentLinkedQueue<Throwable>();
        
        s.onSubscribe(set);
        
        for (Completable c : sources) {
            if (set.isDisposed()) {
                return;
            }
            
            if (c == null) {
                q.offer(new NullPointerException("A completable source is null"));
                wip.decrementAndGet();
                continue;
            }
            
            c.subscribe(new CompletableSubscriber() {
                @Override
                public void onSubscribe(Disposable d) {
                    set.add(d);
                }

                @Override
                public void onError(Throwable e) {
                    q.offer(e);
                    tryTerminate();
                }

                @Override
                public void onComplete() {
                    tryTerminate();
                }
                
                void tryTerminate() {
                    if (wip.decrementAndGet() == 0) {
                        if (q.isEmpty()) {
                            s.onComplete();
                        } else {
                            s.onError(CompletableOnSubscribeMerge.collectErrors(q));
                        }
                    }
                }
                
            });
        }
        
        if (wip.decrementAndGet() == 0) {
            if (q.isEmpty()) {
                s.onComplete();
            } else {
                s.onError(CompletableOnSubscribeMerge.collectErrors(q));
            }
        }
        
    }
}