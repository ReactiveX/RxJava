/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.internal.operators;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import rx.*;
import rx.Completable.*;
import rx.subscriptions.CompositeSubscription;

public final class CompletableOnSubscribeMergeDelayErrorArray implements CompletableOnSubscribe {
    final Completable[] sources;
    
    public CompletableOnSubscribeMergeDelayErrorArray(Completable[] sources) {
        this.sources = sources;
    }
    
    @Override
    public void call(final CompletableSubscriber s) {
        final CompositeSubscription set = new CompositeSubscription();
        final AtomicInteger wip = new AtomicInteger(sources.length + 1);
        
        final Queue<Throwable> q = new ConcurrentLinkedQueue<Throwable>();
        
        s.onSubscribe(set);
        
        for (Completable c : sources) {
            if (set.isUnsubscribed()) {
                return;
            }
            
            if (c == null) {
                q.offer(new NullPointerException("A completable source is null"));
                wip.decrementAndGet();
                continue;
            }
            
            c.unsafeSubscribe(new CompletableSubscriber() {
                @Override
                public void onSubscribe(Subscription d) {
                    set.add(d);
                }

                @Override
                public void onError(Throwable e) {
                    q.offer(e);
                    tryTerminate();
                }

                @Override
                public void onCompleted() {
                    tryTerminate();
                }
                
                void tryTerminate() {
                    if (wip.decrementAndGet() == 0) {
                        if (q.isEmpty()) {
                            s.onCompleted();
                        } else {
                            s.onError(CompletableOnSubscribeMerge.collectErrors(q));
                        }
                    }
                }
                
            });
        }
        
        if (wip.decrementAndGet() == 0) {
            if (q.isEmpty()) {
                s.onCompleted();
            } else {
                s.onError(CompletableOnSubscribeMerge.collectErrors(q));
            }
        }
        
    }
}