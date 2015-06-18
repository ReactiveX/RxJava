/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;

import rx.Observable.Operator;
import rx.*;

public final class OperatorRequestBatcher<T> implements Operator<T, T> {
    final int batchSize;
    final int replenishLevel;
    
    public OperatorRequestBatcher(int batchSize, int replenishLevel) {
        this.batchSize = batchSize;
        this.replenishLevel = replenishLevel;
    }
    @Override
    public Subscriber<? super T> call(Subscriber<? super T> child) {
        RequestBatcherSubscriber<T> parent = new RequestBatcherSubscriber<T>(
                child, batchSize, replenishLevel);
        RequestBatcherProducer<T> producer = new RequestBatcherProducer<T>(parent);
        child.add(parent);
        child.setProducer(producer);
        return parent;
    }

    static final class RequestBatcherSubscriber<T> extends Subscriber<T> {
        final Subscriber<? super T> child;
        final int batchSize;
        final int replenishLevel;
        RequestBatcherProducer<T> producer;
        
        public RequestBatcherSubscriber(Subscriber<? super T> child, int batchSize, int replenishLevel) {
            this.child = child;
            this.batchSize = batchSize;
            this.replenishLevel = replenishLevel;
            this.request(0);
        }
        @Override
        public void onNext(T t) {
            child.onNext(t);
            producer.produced();
        }
        @Override
        public void onError(Throwable e) {
            child.onError(e);
        }
        @Override
        public void onCompleted() {
            child.onCompleted();
        }
        public void requestMore(long n) {
            request(n);
        }
    }
    
    static final class RequestBatcherProducer<T> implements Producer {
        
        final RequestBatcherSubscriber<T> parent;
        long childRequested;
        long upstreamRequested;
        
        public RequestBatcherProducer(RequestBatcherSubscriber<T> parent) {
            parent.producer = this;
            this.parent = parent;
        }
        
        @Override
        public void request(long n) {
            if (n < 0) {
                throw new IllegalArgumentException("n >= 0 required");
            }
            if (n > 0) {
                long n0;
                synchronized (this) {
                    long r = childRequested;
                    long u = r + n;
                    if (u < 0) {
                        u = Long.MAX_VALUE;
                    }
                    childRequested = u;

                    if (r != 0) {
                        return;
                    }
                    long k = upstreamRequested;
                    n0 = Math.min(parent.batchSize, u) - k;
                    upstreamRequested = k + n0;
                }
                if (n0 != 0L) {
                    parent.requestMore(n0);
                }
            }
        }
        void produced() {
            long n0;
            synchronized (this) {
                long c = --childRequested;
                
                long k = --upstreamRequested;
                if (k > parent.replenishLevel) {
                    return;
                }
                
                n0 = Math.min(parent.batchSize - k, c - k);
                upstreamRequested = k + n0;
            }
            if (n0 != 0L) {
                parent.requestMore(n0);
            }
        }
    }
}
