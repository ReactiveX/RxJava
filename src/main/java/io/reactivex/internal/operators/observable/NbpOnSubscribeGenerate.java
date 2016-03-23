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

import io.reactivex.Observer;
import io.reactivex.Observable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOnSubscribeGenerate<T, S> implements NbpOnSubscribe<T> {
    final Supplier<S> stateSupplier;
    final BiFunction<S, Observer<T>, S> generator;
    final Consumer<? super S> disposeState;
    
    public NbpOnSubscribeGenerate(Supplier<S> stateSupplier, BiFunction<S, Observer<T>, S> generator,
            Consumer<? super S> disposeState) {
        this.stateSupplier = stateSupplier;
        this.generator = generator;
        this.disposeState = disposeState;
    }
    
    @Override
    public void accept(Observer<? super T> s) {
        S state;
        
        try {
            state = stateSupplier.get();
        } catch (Throwable e) {
            EmptyDisposable.error(e, s);
            return;
        }
        
        GeneratorDisposable<T, S> gd = new GeneratorDisposable<T, S>(s, generator, disposeState, state);
        s.onSubscribe(gd);
        gd.run();
    }
    
    static final class GeneratorDisposable<T, S> 
    implements Observer<T>, Disposable {
        
        final Observer<? super T> actual;
        final BiFunction<S, ? super Observer<T>, S> generator;
        final Consumer<? super S> disposeState;
        
        S state;
        
        volatile boolean cancelled;
        
        boolean terminate;

        public GeneratorDisposable(Observer<? super T> actual, 
                BiFunction<S, ? super Observer<T>, S> generator,
                Consumer<? super S> disposeState, S initialState) {
            this.actual = actual;
            this.generator = generator;
            this.disposeState = disposeState;
            this.state = initialState;
        }
        
        public void run() {
            S s = state;
            
            final BiFunction<S, ? super Observer<T>, S> f = generator;
            
            if (cancelled) {
                dispose(s);
                return;
            }
            
            for (;;) {
                
                if (cancelled) {
                    dispose(s);
                    return;
                }
                
                try {
                    s = f.apply(s, this);
                } catch (Throwable ex) {
                    cancelled = true;
                    actual.onError(ex);
                    return;
                }
                
                if (terminate) {
                    cancelled = true;
                    dispose(s);
                    return;
                }
            }
        
        }

        private void dispose(S s) {
            try {
                disposeState.accept(s);
            } catch (Throwable ex) {
                RxJavaPlugins.onError(ex);
            }
        }
        
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
            }
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            throw new IllegalStateException("Should not call onSubscribe in the generator!");
        }
        
        @Override
        public void onNext(T t) {
            if (t == null) {
                onError(new NullPointerException());
                return;
            }
            actual.onNext(t);
        }
        
        @Override
        public void onError(Throwable t) {
            if (t == null) {
                t = new NullPointerException();
            }
            terminate = true;
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            terminate = true;
            actual.onComplete();
        }
    }
}
