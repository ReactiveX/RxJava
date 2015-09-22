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
import java.util.function.BooleanSupplier;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.*;

public final class NbpOnSubscribeRepeatUntil<T> implements NbpOnSubscribe<T> {
    final NbpObservable<? extends T> source;
    final BooleanSupplier until;
    public NbpOnSubscribeRepeatUntil(NbpObservable<? extends T> source, BooleanSupplier until) {
        this.source = source;
        this.until = until;
    }
    
    @Override
    public void accept(NbpSubscriber<? super T> s) {
        MultipleAssignmentDisposable sa = new MultipleAssignmentDisposable();
        s.onSubscribe(sa);
        
        RepeatSubscriber<T> rs = new RepeatSubscriber<>(s, until, sa, source);
        rs.subscribeNext();
    }
    
    static final class RepeatSubscriber<T> extends AtomicInteger implements NbpSubscriber<T> {
        /** */
        private static final long serialVersionUID = -7098360935104053232L;
        
        final NbpSubscriber<? super T> actual;
        final MultipleAssignmentDisposable sa;
        final NbpObservable<? extends T> source;
        final BooleanSupplier stop;
        public RepeatSubscriber(NbpSubscriber<? super T> actual, BooleanSupplier until, MultipleAssignmentDisposable sa, NbpObservable<? extends T> source) {
            this.actual = actual;
            this.sa = sa;
            this.source = source;
            this.stop = until;
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
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            boolean b;
            try {
                b = stop.getAsBoolean();
            } catch (Throwable e) {
                actual.onError(e);
                return;
            }
            if (b) {
                actual.onComplete();
            } else {
                subscribeNext();
            }
        }
        
        /**
         * Subscribes to the source again via trampolining.
         */
        void subscribeNext() {
            if (getAndIncrement() == 0) {
                int missed = 1;
                for (;;) {
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
