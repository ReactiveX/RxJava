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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import io.reactivex.NbpObservable.*;
import io.reactivex.Scheduler;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subscribers.nbp.NbpSerializedSubscriber;

public final class NbpOperatorDebounceTimed<T> implements NbpOperator<T, T> {
    final long timeout;
    final TimeUnit unit;
    final Scheduler scheduler;

    public NbpOperatorDebounceTimed(long timeout, TimeUnit unit, Scheduler scheduler) {
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super T> t) {
        return new DebounceTimedSubscriber<>(
                new NbpSerializedSubscriber<>(t), 
                timeout, unit, scheduler.createWorker());
    }
    
    static final class DebounceTimedSubscriber<T>
    implements NbpSubscriber<T>, Disposable {
        final NbpSubscriber<? super T> actual;
        final long timeout;
        final TimeUnit unit;
        final Scheduler.Worker worker;
        
        Disposable s;
        
        volatile Disposable timer;
        @SuppressWarnings("rawtypes")
        static final AtomicReferenceFieldUpdater<DebounceTimedSubscriber, Disposable> TIMER =
                AtomicReferenceFieldUpdater.newUpdater(DebounceTimedSubscriber.class, Disposable.class, "timer");

        static final Disposable CANCELLED = () -> { };

        static final Disposable NEW_TIMER = () -> { };
        
        volatile long index;
        
        boolean done;
        
        public DebounceTimedSubscriber(NbpSubscriber<? super T> actual, long timeout, TimeUnit unit, Worker worker) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
        }
        
        public void disposeTimer() {
            Disposable d = timer;
            if (d != CANCELLED) {
                d = TIMER.getAndSet(this, CANCELLED);
                if (d != CANCELLED && d != null) {
                    d.dispose();
                }
            }
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
            if (done) {
                return;
            }
            long idx = index + 1;
            index = idx;
            
            Disposable d = timer;
            if (d != null) {
                d.dispose();
            }
            
            DebounceEmitter<T> de = new DebounceEmitter<>(t, idx, this);
            if (!TIMER.compareAndSet(this, d, de)) {
                return;
            }
                
            d = worker.schedule(de, timeout, unit);
            
            de.setResource(d);
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            disposeTimer();
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            
            Disposable d = timer;
            if (d != CANCELLED) {
                @SuppressWarnings("unchecked")
                DebounceEmitter<T> de = (DebounceEmitter<T>)d;
                de.emit();
                disposeTimer();
                worker.dispose();
                actual.onComplete();
            }
        }
        
        @Override
        public void dispose() {
            disposeTimer();
            worker.dispose();
            s.dispose();
        }
        
        void emit(long idx, T t, DebounceEmitter<T> emitter) {
            if (idx == index) {
                actual.onNext(t);
                emitter.dispose();
            }
        }
    }
    
    static final class DebounceEmitter<T> extends AtomicReference<Disposable> implements Runnable, Disposable {
        /** */
        private static final long serialVersionUID = 6812032969491025141L;

        static final Disposable DISPOSED = () -> { };
        
        final T value;
        final long idx;
        final DebounceTimedSubscriber<T> parent;
        
        volatile int once;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<DebounceEmitter> ONCE =
                AtomicIntegerFieldUpdater.newUpdater(DebounceEmitter.class, "once");

        
        public DebounceEmitter(T value, long idx, DebounceTimedSubscriber<T> parent) {
            this.value = value;
            this.idx = idx;
            this.parent = parent;
        }

        @Override
        public void run() {
            emit();
        }
        
        void emit() {
            if (ONCE.compareAndSet(this, 0, 1)) {
                parent.emit(idx, value, this);
            }
        }
        
        @Override
        public void dispose() {
            Disposable d = get();
            if (d != DISPOSED) {
                d = getAndSet(DISPOSED);
                if (d != DISPOSED && d != null) {
                    d.dispose();
                }
            }
        }
        
        public void setResource(Disposable d) {
            for (;;) {
                Disposable a = get();
                if (a == DISPOSED) {
                    d.dispose();
                    return;
                }
                if (compareAndSet(a, d)) {
                    return;
                }
            }
        }
    }
}
