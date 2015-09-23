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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.*;

public final class NbpOnSubscribeRetryBiPredicate<T> implements NbpOnSubscribe<T> {
    final NbpObservable<? extends T> source;
    final BiPredicate<? super Integer, ? super Throwable> predicate;
    public NbpOnSubscribeRetryBiPredicate(
            NbpObservable<? extends T> source, 
            BiPredicate<? super Integer, ? super Throwable> predicate) {
        this.source = source;
        this.predicate = predicate;
    }
    
    @Override
    public void accept(NbpSubscriber<? super T> s) {
        SerialDisposable sa = new SerialDisposable();
        s.onSubscribe(sa);
        
        RetryBiSubscriber<T> rs = new RetryBiSubscriber<>(s, predicate, sa, source);
        rs.subscribeNext();
    }
    
    static final class RetryBiSubscriber<T> extends AtomicInteger implements NbpSubscriber<T> {
        /** */
        private static final long serialVersionUID = -7098360935104053232L;
        
        final NbpSubscriber<? super T> actual;
        final SerialDisposable sa;
        final NbpObservable<? extends T> source;
        final BiPredicate<? super Integer, ? super Throwable> predicate;
        int retries;
        public RetryBiSubscriber(NbpSubscriber<? super T> actual, 
                BiPredicate<? super Integer, ? super Throwable> predicate, SerialDisposable sa, NbpObservable<? extends T> source) {
            this.actual = actual;
            this.sa = sa;
            this.source = source;
            this.predicate = predicate;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            sa.set(s);
        }
        
        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }
        @Override
        public void onError(Throwable t) {
            boolean b;
            try {
                b = predicate.test(++retries, t);
            } catch (Throwable e) {
                e.addSuppressed(t);
                actual.onError(e);
                return;
            }
            if (!b) {
                actual.onError(t);
                return;
            }
            subscribeNext();
        }
        
        @Override
        public void onComplete() {
            actual.onComplete();
        }
        
        /**
         * Subscribes to the source again via trampolining.
         */
        void subscribeNext() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                for (;;) {
                    if (sa.isDisposed()) {
                        return;
                    }
                    source.subscribe(this);
                    
                    missed = addAndGet(-missed);
                    if (missed == 0) {
                        break;
                    }
                }
            }
        }
    }
}
