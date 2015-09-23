/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class NbpOnSubscribeSubscribeOn<T> implements NbpOnSubscribe<T> {
    final NbpObservable<? extends T> source;
    final Scheduler scheduler;
    
    public NbpOnSubscribeSubscribeOn(NbpObservable<? extends T> source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }
    
    @Override
    public void accept(NbpSubscriber<? super T> s) {
        /*
         * TODO can't use the returned disposable because to dispose it,
         * one must set a Subscription on s on the current thread, but
         * it is expected that onSubscribe is run on the target scheduler.
         */
        scheduler.scheduleDirect(() -> {
            source.subscribe(s);
        });
    }
    
    static final class SubscribeOnSubscriber<T> extends AtomicReference<Thread> implements NbpSubscriber<T>, Disposable {
        /** */
        private static final long serialVersionUID = 8094547886072529208L;
        final NbpSubscriber<? super T> actual;
        final Scheduler.Worker worker;
        
        Disposable s;
        
        public SubscribeOnSubscriber(NbpSubscriber<? super T> actual, Scheduler.Worker worker) {
            this.actual = actual;
            this.worker = worker;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            this.s = s;
            lazySet(Thread.currentThread());
            actual.onSubscribe(this);
        }
        
        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }
        
        @Override
        public void onError(Throwable t) {
            try {
                actual.onError(t);
            } finally {
                worker.dispose();
            }
        }
        
        @Override
        public void onComplete() {
            try {
                actual.onComplete();
            } finally {
                worker.dispose();
            }
        }
        
        @Override
        public void dispose() {
            s.dispose();
            worker.dispose();
        }
    }
}
