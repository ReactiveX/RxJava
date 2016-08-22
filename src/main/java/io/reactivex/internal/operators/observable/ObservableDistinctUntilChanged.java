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

import io.reactivex.*;
import io.reactivex.functions.BiPredicate;
import io.reactivex.internal.subscribers.observable.BasicFuseableObserver;

public final class ObservableDistinctUntilChanged<T> extends AbstractObservableWithUpstream<T, T> {

    final BiPredicate<? super T, ? super T> comparer;

    public ObservableDistinctUntilChanged(ObservableSource<T> source, BiPredicate<? super T, ? super T> comparer) {
        super(source);
        this.comparer = comparer;
    }
    
    @Override
    protected void subscribeActual(Observer<? super T> s) {
        source.subscribe(new DistinctUntilChangedObserver<T>(s, comparer));
    }

    static final class DistinctUntilChangedObserver<T> extends BasicFuseableObserver<T, T> {

        final BiPredicate<? super T, ? super T> comparer;
        
        T last;
        
        boolean hasValue;
        
        public DistinctUntilChangedObserver(Observer<? super T> actual, 
                BiPredicate<? super T, ? super T> comparer) {
            super(actual);
            this.comparer = comparer;
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            if (sourceMode != NONE) {
                actual.onNext(t);
                return;
            }
            
            if (hasValue) {
                boolean equal;
                try {
                    equal = comparer.test(last, t);
                } catch (Throwable ex) {
                    fail(ex);
                    return;
                }
                last = t;
                if (equal) {
                    return;
                }
                actual.onNext(t);
                return;
            }
            hasValue = true;
            last = t;
            actual.onNext(t);
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Override
        public T poll() throws Exception {
            for (;;) {
                T v = qs.poll();
                if (v == null) {
                    return null;
                }
                if (!hasValue) {
                    hasValue = true;
                    last = v;
                    return v;
                }
                
                if (!comparer.test(last, v)) {
                    last = v;
                    return v;
                }
                last = v;
            }
        }
        
    }
}
