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

import java.util.concurrent.atomic.AtomicInteger;

import rx.*;
import rx.Completable.*;
import rx.subscriptions.SerialSubscription;

public final class CompletableOnSubscribeConcatArray implements CompletableOnSubscribe {
    final Completable[] sources;
    
    public CompletableOnSubscribeConcatArray(Completable[] sources) {
        this.sources = sources;
    }
    
    @Override
    public void call(CompletableSubscriber s) {
        ConcatInnerSubscriber inner = new ConcatInnerSubscriber(s, sources);
        s.onSubscribe(inner.sd);
        inner.next();
    }
    
    static final class ConcatInnerSubscriber extends AtomicInteger implements CompletableSubscriber {
        /** */
        private static final long serialVersionUID = -7965400327305809232L;

        final CompletableSubscriber actual;
        final Completable[] sources;
        
        int index;
        
        final SerialSubscription sd;
        
        public ConcatInnerSubscriber(CompletableSubscriber actual, Completable[] sources) {
            this.actual = actual;
            this.sources = sources;
            this.sd = new SerialSubscription();
        }
        
        @Override
        public void onSubscribe(Subscription d) {
            sd.set(d);
        }
        
        @Override
        public void onError(Throwable e) {
            actual.onError(e);
        }
        
        @Override
        public void onCompleted() {
            next();
        }
        
        void next() {
            if (sd.isUnsubscribed()) {
                return;
            }
            
            if (getAndIncrement() != 0) {
                return;
            }

            Completable[] a = sources;
            do {
                if (sd.isUnsubscribed()) {
                    return;
                }
                
                int idx = index++;
                if (idx == a.length) {
                    actual.onCompleted();
                    return;
                }
                
                a[idx].unsafeSubscribe(this);
            } while (decrementAndGet() != 0);
        }
    }
}