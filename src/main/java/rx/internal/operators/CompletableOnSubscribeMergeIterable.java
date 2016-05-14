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

import java.util.Iterator;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Completable.*;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.CompositeSubscription;

public final class CompletableOnSubscribeMergeIterable implements CompletableOnSubscribe {
    final Iterable<? extends Completable> sources;
    
    public CompletableOnSubscribeMergeIterable(Iterable<? extends Completable> sources) {
        this.sources = sources;
    }
    
    @Override
    public void call(final CompletableSubscriber s) {
        final CompositeSubscription set = new CompositeSubscription();
        final AtomicInteger wip = new AtomicInteger(1);
        final AtomicBoolean once = new AtomicBoolean();
        
        s.onSubscribe(set);
        
        Iterator<? extends Completable> iterator;
        
        try {
            iterator = sources.iterator();
        } catch (Throwable e) {
            s.onError(e);
            return;
        }
        
        if (iterator == null) {
            s.onError(new NullPointerException("The source iterator returned is null"));
            return;
        }
        
        for (;;) {
            if (set.isUnsubscribed()) {
                return;
            }
            
            boolean b;
            try {
                b = iterator.hasNext();
            } catch (Throwable e) {
                set.unsubscribe();
                if (once.compareAndSet(false, true)) {
                    s.onError(e);
                } else {
                    RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
                }
                return;
            }
                    
            if (!b) {
                break;
            }
            
            if (set.isUnsubscribed()) {
                return;
            }
            
            Completable c;
            
            try {
                c = iterator.next();
            } catch (Throwable e) {
                set.unsubscribe();
                if (once.compareAndSet(false, true)) {
                    s.onError(e);
                } else {
                    RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
                }
                return;
            }
            
            if (set.isUnsubscribed()) {
                return;
            }
            
            if (c == null) {
                set.unsubscribe();
                NullPointerException npe = new NullPointerException("A completable source is null");
                if (once.compareAndSet(false, true)) {
                    s.onError(npe);
                } else {
                    RxJavaPlugins.getInstance().getErrorHandler().handleError(npe);
                }
                return;
            }
            
            wip.getAndIncrement();
            
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