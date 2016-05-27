/**
 * Copyright 2016 Netflix, Inc.
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

import java.util.concurrent.atomic.AtomicBoolean;

import rx.*;
import rx.Completable.*;
import rx.internal.util.RxJavaPluginUtils;
import rx.subscriptions.CompositeSubscription;

public final class CompletableOnSubscribeAmbArray implements CompletableOnSubscribe {
    final Completable[] sources;
    
    public CompletableOnSubscribeAmbArray(Completable[] sources) {
        this.sources = sources;
    }
    
    public CompletableOnSubscribeAmbArray ambWith(Completable source) {
        Completable[] oldSources = sources;
        
        int oldLen = oldSources.length;
        Completable[] newSources = new Completable[oldLen + 1];
        System.arraycopy(oldSources, 0, newSources, 0, oldLen);
        newSources[oldLen] = source;
        
        return new CompletableOnSubscribeAmbArray(newSources);
    }
    
    @Override
    public void call(final CompletableSubscriber s) {
        final CompositeSubscription set = new CompositeSubscription();
        s.onSubscribe(set);

        final AtomicBoolean once = new AtomicBoolean();
        
        CompletableSubscriber inner = new CompletableSubscriber() {
            @Override
            public void onCompleted() {
                if (once.compareAndSet(false, true)) {
                    set.unsubscribe();
                    s.onCompleted();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (once.compareAndSet(false, true)) {
                    set.unsubscribe();
                    s.onError(e);
                } else {
                    RxJavaPluginUtils.handleException(e);
                }
            }

            @Override
            public void onSubscribe(Subscription d) {
                set.add(d);
            }
            
        };
        
        for (Completable c : sources) {
            if (set.isUnsubscribed()) {
                return;
            }
            if (c == null) {
                NullPointerException npe = new NullPointerException("One of the sources is null");
                if (once.compareAndSet(false, true)) {
                    set.unsubscribe();
                    s.onError(npe);
                } else {
                    RxJavaPluginUtils.handleException(npe);
                }
                return;
            }
            if (once.get() || set.isUnsubscribed()) {
                return;
            }
            
            // no need to have separate subscribers because inner is stateless
            c.subscribe(inner);
        }
    }
}
