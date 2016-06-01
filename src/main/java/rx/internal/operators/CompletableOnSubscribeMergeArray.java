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

import java.util.concurrent.atomic.*;

import rx.*;
import rx.Completable.*;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.CompositeSubscription;

public final class CompletableOnSubscribeMergeArray implements CompletableOnSubscribe {
    final Completable[] sources;
    
    public CompletableOnSubscribeMergeArray(Completable[] sources) {
        this.sources = sources;
    }
    
    @Override
    public void call(final CompletableSubscriber s) {
        final CompositeSubscription set = new CompositeSubscription();
        final AtomicInteger wip = new AtomicInteger(sources.length + 1);
        final AtomicBoolean once = new AtomicBoolean();
        
        s.onSubscribe(set);
        
        for (Completable c : sources) {
            if (set.isUnsubscribed()) {
                return;
            }
            
            if (c == null) {
                set.unsubscribe();
                NullPointerException npe = new NullPointerException("A completable source is null");
                if (once.compareAndSet(false, true)) {
                    s.onError(npe);
                    return;
                } else {
                    RxJavaPlugins.getInstance().getErrorHandler().handleError(npe);
                }
            }
            
            c.unsafeSubscribe(new CompletableSubscriber() {
                @Override
                public void onSubscribe(Subscription d) {
                    set.add(d);
                }

                @Override
                public void onError(Throwable e) {
                    set.unsubscribe();
                    if (once.compareAndSet(false, true)) {
                        s.onError(e);
                    } else {
                        RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
                    }
                }

                @Override
                public void onCompleted() {
                    if (wip.decrementAndGet() == 0) {
                        if (once.compareAndSet(false, true)) {
                            s.onCompleted();
                        }
                    }
                }
                
            });
        }
        
        if (wip.decrementAndGet() == 0) {
            if (once.compareAndSet(false, true)) {
                s.onCompleted();
            }
        }
    }
}