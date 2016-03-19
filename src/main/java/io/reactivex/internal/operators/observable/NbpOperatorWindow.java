/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.observable;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.Observable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.subjects.UnicastSubject;

public final class NbpOperatorWindow<T> implements NbpOperator<Observable<T>, T> {
    final long count;
    final long skip;
    final int capacityHint;
    
    public NbpOperatorWindow(long count, long skip, int capacityHint) {
        this.count = count;
        this.skip = skip;
        this.capacityHint = capacityHint;
    }
    
    @Override
    public Observer<? super T> apply(Observer<? super Observable<T>> t) {
        if (count == skip) {
            return new WindowExactSubscriber<T>(t, count, capacityHint);
        }
        return new WindowSkipSubscriber<T>(t, count, skip, capacityHint);
    }
    
    static final class WindowExactSubscriber<T>
    extends AtomicInteger
    implements Observer<T>, Disposable, Runnable {
        /** */
        private static final long serialVersionUID = -7481782523886138128L;
        final Observer<? super Observable<T>> actual;
        final long count;
        final int capacityHint;
        
        long size;
        
        Disposable s;
        
        UnicastSubject<T> window;
        
        volatile boolean cancelled;
        
        public WindowExactSubscriber(Observer<? super Observable<T>> actual, long count, int capacityHint) {
            this.actual = actual;
            this.count = count;
            this.capacityHint = capacityHint;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            
            this.s = s;
            
            actual.onSubscribe(this);
        }
        
        @Override
        public void onNext(T t) {
            UnicastSubject<T> w = window;
            if (w == null && !cancelled) {
                w = UnicastSubject.create(capacityHint, this);
                window = w;
                actual.onNext(w);
            }

            w.onNext(t);
            if (++size >= count) {
                size = 0;
                window = null;
                w.onComplete();
                if (cancelled) {
                    s.dispose();
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            UnicastSubject<T> w = window;
            if (w != null) {
                window = null;
                w.onError(t);
            }
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            UnicastSubject<T> w = window;
            if (w != null) {
                window = null;
                w.onComplete();
            }
            actual.onComplete();
        }
        
        @Override
        public void dispose() {
            cancelled = true;
        }
        
        @Override
        public void run() {
            if (cancelled) {
                s.dispose();
            }
        }
    }
    
    static final class WindowSkipSubscriber<T> extends AtomicBoolean 
    implements Observer<T>, Disposable, Runnable {
        /** */
        private static final long serialVersionUID = 3366976432059579510L;
        final Observer<? super Observable<T>> actual;
        final long count;
        final long skip;
        final int capacityHint;
        final ArrayDeque<UnicastSubject<T>> windows;
        
        long index;
        
        volatile boolean cancelled;
        
        /** Counts how many elements were emitted to the very first window in windows. */
        long firstEmission;
        
        Disposable s;
        
        final AtomicInteger wip = new AtomicInteger();
        
        public WindowSkipSubscriber(Observer<? super Observable<T>> actual, long count, long skip, int capacityHint) {
            this.actual = actual;
            this.count = count;
            this.skip = skip;
            this.capacityHint = capacityHint;
            this.windows = new ArrayDeque<UnicastSubject<T>>();
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                return;
            }
            
            this.s = s;
            
            actual.onSubscribe(this);
        }

        @Override
        public void onNext(T t) {
            final ArrayDeque<UnicastSubject<T>> ws = windows;
            
            long i = index;
            
            long s = skip;
            
            if (i % s == 0 && !cancelled) {
                wip.getAndIncrement();
                UnicastSubject<T> w = UnicastSubject.create(capacityHint, this);
                ws.offer(w);
                actual.onNext(w);
            }

            long c = firstEmission + 1;
            
            for (UnicastSubject<T> w : ws) {
                w.onNext(t);
            }
            
            if (c >= count) {
                ws.poll().onComplete();
                if (ws.isEmpty() && cancelled) {
                    this.s.dispose();
                    return;
                }
                firstEmission = c - s;
            } else {
                firstEmission = c;
            }
            
            index = i + 1;
        }
        
        @Override
        public void onError(Throwable t) {
            final ArrayDeque<UnicastSubject<T>> ws = windows;
            while (!ws.isEmpty()) {
                ws.poll().onError(t);
            }
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            final ArrayDeque<UnicastSubject<T>> ws = windows;
            while (!ws.isEmpty()) {
                ws.poll().onComplete();
            }
            actual.onComplete();
        }
        
        @Override
        public void dispose() {
            cancelled = true;
        }
        
        @Override
        public void run() {
            if (wip.decrementAndGet() == 0) {
                if (cancelled) {
                    s.dispose();
                }
            }
        }
    }
}
