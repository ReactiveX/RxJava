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

package io.reactivex.internal.operators;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class OperatorOnBackpressureDrop<T> implements Operator<T, T> {
    
    private static final OperatorOnBackpressureDrop<Object> DEFAULT =
            new OperatorOnBackpressureDrop<>(v -> { });
    
    @SuppressWarnings("unchecked")
    public static <T> OperatorOnBackpressureDrop<T> instance() {
        return (OperatorOnBackpressureDrop<T>)DEFAULT;
    }
    
    private final Consumer<? super T> onDrop;
    
    public OperatorOnBackpressureDrop(Consumer<? super T> onDrop) {
        this.onDrop = onDrop;
    }
    
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> t) {
        return new BackpressureDropSubscriber<>(t, onDrop);
    }
    
    static final class BackpressureDropSubscriber<T> extends AtomicLong implements Subscriber<T>, Subscription {
        /** */
        private static final long serialVersionUID = -6246093802440953054L;
        
        final Subscriber<? super T> actual;
        final Consumer<? super T> onDrop;
        
        Subscription s;
        
        boolean done;
        
        public BackpressureDropSubscriber(Subscriber<? super T> actual, Consumer<? super T> onDrop) {
            this.actual = actual;
            this.onDrop = onDrop;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (this.s != null) {
                s.cancel();
                RxJavaPlugins.onError(new IllegalStateException("Subscription already set!"));
                return;
            }
            this.s = s;
            actual.onSubscribe(this);
            s.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            long r = get();
            if (r != 0L) {
                actual.onNext(t);
                if (r != Long.MAX_VALUE) {
                    decrementAndGet();
                }
            } else {
                try {
                    onDrop.accept(t);
                } catch (Throwable e) {
                    done = true;
                    cancel();
                    actual.onError(e);
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                return;
            }
            done = true;
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            actual.onComplete();
        }
        
        @Override
        public void request(long n) {
            if (n <= 0) {
                RxJavaPlugins.onError(new IllegalArgumentException("n > required but it was " + n));
                return;
            }
            BackpressureHelper.add(this, n);
        }
        
        @Override
        public void cancel() {
            s.cancel();
        }
    }
}
