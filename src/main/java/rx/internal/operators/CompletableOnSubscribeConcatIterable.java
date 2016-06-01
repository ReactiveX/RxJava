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
import java.util.concurrent.atomic.AtomicInteger;

import rx.*;
import rx.Completable.*;
import rx.subscriptions.*;

public final class CompletableOnSubscribeConcatIterable implements CompletableOnSubscribe {
    final Iterable<? extends Completable> sources;
    
    public CompletableOnSubscribeConcatIterable(Iterable<? extends Completable> sources) {
        this.sources = sources;
    }
    
    @Override
    public void call(CompletableSubscriber s) {
        
        Iterator<? extends Completable> it;
        
        try {
            it = sources.iterator();
        } catch (Throwable e) {
            s.onSubscribe(Subscriptions.unsubscribed());
            s.onError(e);
            return;
        }
        
        if (it == null) {
            s.onSubscribe(Subscriptions.unsubscribed());
            s.onError(new NullPointerException("The iterator returned is null"));
            return;
        }
        
        ConcatInnerSubscriber inner = new ConcatInnerSubscriber(s, it);
        s.onSubscribe(inner.sd);
        inner.next();
    }
    
    static final class ConcatInnerSubscriber extends AtomicInteger implements CompletableSubscriber {
        /** */
        private static final long serialVersionUID = -7965400327305809232L;

        final CompletableSubscriber actual;
        final Iterator<? extends Completable> sources;
        
        int index;
        
        final SerialSubscription sd;
        
        public ConcatInnerSubscriber(CompletableSubscriber actual, Iterator<? extends Completable> sources) {
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

            Iterator<? extends Completable> a = sources;
            do {
                if (sd.isUnsubscribed()) {
                    return;
                }
                
                boolean b;
                try {
                    b = a.hasNext();
                } catch (Throwable ex) {
                    actual.onError(ex);
                    return;
                }
                
                if (!b) {
                    actual.onCompleted();
                    return;
                }
                
                Completable c;
                
                try {
                    c = a.next();
                } catch (Throwable ex) {
                    actual.onError(ex);
                    return;
                }
                
                if (c == null) {
                    actual.onError(new NullPointerException("The completable returned is null"));
                    return;
                }
                
                c.unsafeSubscribe(this);
            } while (decrementAndGet() != 0);
        }
    }
}