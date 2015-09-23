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

import java.util.concurrent.atomic.*;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOnSubscribeAmb<T> implements NbpOnSubscribe<T> {
    final NbpObservable<? extends T>[] sources;
    final Iterable<? extends NbpObservable<? extends T>> sourcesIterable;
    
    public NbpOnSubscribeAmb(NbpObservable<? extends T>[] sources, Iterable<? extends NbpObservable<? extends T>> sourcesIterable) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public void accept(NbpSubscriber<? super T> s) {
        NbpObservable<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new NbpObservable[8];
            for (NbpObservable<? extends T> p : sourcesIterable) {
                if (count == sources.length) {
                    NbpObservable<? extends T>[] b = new NbpObservable[count + (count >> 2)];
                    System.arraycopy(sources, 0, b, 0, count);
                    sources = b;
                }
                sources[count++] = p;
            }
        } else {
            count = sources.length;
        }
        
        if (count == 0) {
            EmptyDisposable.complete(s);
            return;
        } else
        if (count == 1) {
            sources[0].subscribe(s);
            return;
        }

        AmbCoordinator<T> ac = new AmbCoordinator<>(s, count);
        ac.subscribe(sources);
    }
    
    static final class AmbCoordinator<T> implements Disposable {
        final NbpSubscriber<? super T> actual;
        final AmbInnerSubscriber<T>[] subscribers;
        
        volatile int winner;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<AmbCoordinator> WINNER =
                AtomicIntegerFieldUpdater.newUpdater(AmbCoordinator.class, "winner");
        
        @SuppressWarnings("unchecked")
        public AmbCoordinator(NbpSubscriber<? super T> actual, int count) {
            this.actual = actual;
            this.subscribers = new AmbInnerSubscriber[count];
        }
        
        public void subscribe(NbpObservable<? extends T>[] sources) {
            AmbInnerSubscriber<T>[] as = subscribers;
            int len = as.length;
            for (int i = 0; i < len; i++) {
                as[i] = new AmbInnerSubscriber<>(this, i + 1, actual);
            }
            WINNER.lazySet(this, 0); // release the contents of 'as'
            actual.onSubscribe(this);
            
            for (int i = 0; i < len; i++) {
                if (winner != 0) {
                    return;
                }
                
                sources[i].subscribe(as[i]);
            }
        }
        
        public boolean win(int index) {
            int w = winner;
            if (w == 0) {
                if (WINNER.compareAndSet(this, 0, index)) {
                    return true;
                }
                return false;
            }
            return w == index;
        }
        
        @Override
        public void dispose() {
            if (winner != -1) {
                WINNER.lazySet(this, -1);
                
                for (AmbInnerSubscriber<T> a : subscribers) {
                    a.dispose();
                }
            }
        }
    }
    
    static final class AmbInnerSubscriber<T> extends AtomicReference<Disposable> implements NbpSubscriber<T>, Disposable {
        /** */
        private static final long serialVersionUID = -1185974347409665484L;
        final AmbCoordinator<T> parent;
        final int index;
        final NbpSubscriber<? super T> actual;
        
        boolean won;
        
        static final Disposable CANCELLED = () -> { };
        
        public AmbInnerSubscriber(AmbCoordinator<T> parent, int index, NbpSubscriber<? super T> actual) {
            this.parent = parent;
            this.index = index;
            this.actual = actual;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (!compareAndSet(null, s)) {
                s.dispose();
                if (get() != CANCELLED) {
                    SubscriptionHelper.reportDisposableSet();
                }
                return;
            }
        }
        
        @Override
        public void onNext(T t) {
            if (won) {
                actual.onNext(t);
            } else {
                if (parent.win(index)) {
                    won = true;
                    actual.onNext(t);
                } else {
                    get().dispose();
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (won) {
                actual.onError(t);
            } else {
                if (parent.win(index)) {
                    won = true;
                    actual.onError(t);
                } else {
                    get().dispose();
                    RxJavaPlugins.onError(t);
                }
            }
        }
        
        @Override
        public void onComplete() {
            if (won) {
                actual.onComplete();
            } else {
                if (parent.win(index)) {
                    won = true;
                    actual.onComplete();
                } else {
                    get().dispose();
                }
            }
        }
        
        @Override
        public void dispose() {
            Disposable s = get();
            if (s != CANCELLED) {
                s = getAndSet(CANCELLED);
                if (s != CANCELLED && s != null) {
                    s.dispose();
                }
            }
        }
        
    }
}
