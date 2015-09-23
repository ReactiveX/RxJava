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

import java.util.function.Function;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOperatorOnErrorNext<T> implements NbpOperator<T, T> {
    final Function<? super Throwable, ? extends NbpObservable<? extends T>> nextSupplier;
    final boolean allowFatal;
    
    public NbpOperatorOnErrorNext(Function<? super Throwable, ? extends NbpObservable<? extends T>> nextSupplier, boolean allowFatal) {
        this.nextSupplier = nextSupplier;
        this.allowFatal = allowFatal;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super T> t) {
        OnErrorNextSubscriber<T> parent = new OnErrorNextSubscriber<>(t, nextSupplier, allowFatal);
        t.onSubscribe(parent.arbiter);
        return parent;
    }
    
    static final class OnErrorNextSubscriber<T> implements NbpSubscriber<T> {
        final NbpSubscriber<? super T> actual;
        final Function<? super Throwable, ? extends NbpObservable<? extends T>> nextSupplier;
        final boolean allowFatal;
        final MultipleAssignmentDisposable arbiter;
        
        boolean once;
        
        boolean done;
        
        public OnErrorNextSubscriber(NbpSubscriber<? super T> actual, Function<? super Throwable, ? extends NbpObservable<? extends T>> nextSupplier, boolean allowFatal) {
            this.actual = actual;
            this.nextSupplier = nextSupplier;
            this.allowFatal = allowFatal;
            this.arbiter = new MultipleAssignmentDisposable();
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            arbiter.set(s);
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            actual.onNext(t);
        }
        
        @Override
        public void onError(Throwable t) {
            if (once) {
                if (done) {
                    RxJavaPlugins.onError(t);
                    return;
                }
                actual.onError(t);
                return;
            }
            once = true;

            if (allowFatal && !(t instanceof Exception)) {
                actual.onError(t);
                return;
            }
            
            NbpObservable<? extends T> p;
            
            try {
                p = nextSupplier.apply(t);
            } catch (Throwable e) {
                t.addSuppressed(e);
                actual.onError(t);
                return;
            }
            
            if (p == null) {
                NullPointerException npe = new NullPointerException("Observable is null");
                t.addSuppressed(npe);
                actual.onError(t);
                return;
            }
            
            p.subscribe(this);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            once = true;
            actual.onComplete();
        }
    }
}
