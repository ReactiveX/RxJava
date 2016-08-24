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

package io.reactivex.internal.operators.flowable;

import org.reactivestreams.*;

import io.reactivex.functions.BiPredicate;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.subscribers.flowable.*;

public final class FlowableDistinctUntilChanged<T> extends AbstractFlowableWithUpstream<T, T> {

    final BiPredicate<? super T, ? super T> comparer;

    public FlowableDistinctUntilChanged(Publisher<T> source, BiPredicate<? super T, ? super T> comparer) {
        super(source);
        this.comparer = comparer;
    }
    
    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            ConditionalSubscriber<? super T> cs = (ConditionalSubscriber<? super T>) s;
            source.subscribe(new DistinctUntilChangedConditionalSubscriber<T>(cs, comparer));
        } else {
            source.subscribe(new DistinctUntilChangedSubscriber<T>(s, comparer));
        }
    }

    static final class DistinctUntilChangedSubscriber<T> extends BasicFuseableSubscriber<T, T>
    implements ConditionalSubscriber<T> {

        final BiPredicate<? super T, ? super T> comparer;
        
        T last;
        
        boolean hasValue;
        
        public DistinctUntilChangedSubscriber(Subscriber<? super T> actual, 
                BiPredicate<? super T, ? super T> comparer) {
            super(actual);
            this.comparer = comparer;
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                s.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }
            if (sourceMode != NONE) {
                actual.onNext(t);
                return true;
            }
            
            if (hasValue) {
                boolean equal;
                try {
                    equal = comparer.test(last, t);
                } catch (Throwable ex) {
                    fail(ex);
                    return false;
                }
                last = t;
                if (equal) {
                    return false;
                }
                actual.onNext(t);
                return true;
            }
            hasValue = true;
            last = t;
            actual.onNext(t);
            return true;
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
                if (sourceMode != SYNC) {
                    s.request(1);
                }
            }
        }
        
    }
    
    static final class DistinctUntilChangedConditionalSubscriber<T> extends BasicFuseableConditionalSubscriber<T, T> {

        final BiPredicate<? super T, ? super T> comparer;
        
        T last;
        
        boolean hasValue;
        
        public DistinctUntilChangedConditionalSubscriber(ConditionalSubscriber<? super T> actual, 
                BiPredicate<? super T, ? super T> comparer) {
            super(actual);
            this.comparer = comparer;
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                s.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }
            if (sourceMode != NONE) {
                return actual.tryOnNext(t);
            }
            
            if (hasValue) {
                boolean equal;
                try {
                    equal = comparer.test(last, t);
                } catch (Throwable ex) {
                    fail(ex);
                    return false;
                }
                last = t;
                return !equal && actual.tryOnNext(t);
            }
            hasValue = true;
            last = t;
            return actual.tryOnNext(t);
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
                if (sourceMode != SYNC) {
                    s.request(1);
                }
            }
        }
        
    }
}
