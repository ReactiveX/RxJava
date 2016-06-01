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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.*;
import rx.Completable.*;
import rx.functions.Action0;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.CompositeSubscription;

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
    public void call(final CompletableSubscriber s) {
        final CompositeSubscription set = new CompositeSubscription();
        s.onSubscribe(set);
        
        final AtomicBoolean once = new AtomicBoolean();
        
        Scheduler.Worker w = scheduler.createWorker();
        
        set.add(w);
        w.schedule(new Action0() {
            @Override
            public void call() {
                if (once.compareAndSet(false, true)) {
                    set.clear();
                    if (other == null) {
                        s.onError(new TimeoutException());
                    } else {
                        other.unsafeSubscribe(new CompletableSubscriber() {
   
                            @Override
                            public void onSubscribe(Subscription d) {
                                set.add(d);
                            }
   
                            @Override
                            public void onError(Throwable e) {
                                set.unsubscribe();
                                s.onError(e);
                            }
   
                            @Override
                            public void onCompleted() {
                                set.unsubscribe();
                                s.onCompleted();
                            }
                            
                        });
                    }
                }
            }
        }, timeout, unit);
        
        source.unsafeSubscribe(new CompletableSubscriber() {

            @Override
            public void onSubscribe(Subscription d) {
                set.add(d);
            }

            @Override
            public void onError(Throwable e) {
                if (once.compareAndSet(false, true)) {
                    set.unsubscribe();
                    s.onError(e);
                } else {
                    RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
                }
            }

            @Override
            public void onCompleted() {
                if (once.compareAndSet(false, true)) {
                    set.unsubscribe();
                    s.onCompleted();
                }
            }
            
        });
    }
}