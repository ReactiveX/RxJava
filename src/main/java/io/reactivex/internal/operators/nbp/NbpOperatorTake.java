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

import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * 
 */
public final class NbpOperatorTake<T> implements NbpOperator<T, T> {
    final long limit;
    public NbpOperatorTake(long limit) {
        this.limit = limit;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super T> t) {
        return new TakeSubscriber<>(t, limit);
    }
    
    static final class TakeSubscriber<T> implements NbpSubscriber<T> {
        final NbpSubscriber<? super T> actual;

        boolean done;

        Disposable subscription;
        
        long remaining;
        public TakeSubscriber(NbpSubscriber<? super T> actual, long limit) {
            this.actual = actual;
            this.remaining = limit;
        }
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.subscription, s)) {
                return;
            }
            subscription = s;
            actual.onSubscribe(s);
        }
        @Override
        public void onNext(T t) {
            if (!done && remaining-- > 0) {
                boolean stop = remaining == 0;
                actual.onNext(t);
                if (stop) {
                    onComplete();
                }
            }
        }
        @Override
        public void onError(Throwable t) {
            if (!done) {
                done = true;
                subscription.dispose();
                actual.onError(t);
            }
        }
        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                subscription.dispose();
                actual.onComplete();
            }
        }
    }
}
