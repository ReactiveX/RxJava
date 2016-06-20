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
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class NbpOperatorTake<T> extends Observable<T> {
    final ObservableConsumable<? extends T> source;
    final long limit;
    public NbpOperatorTake(ObservableConsumable<? extends T> source, long limit) {
        this.source = source;
        this.limit = limit;
    }
    
    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        source.subscribe(new TakeSubscriber<T>(observer, limit));
    }
    
    static final class TakeSubscriber<T> implements Observer<T> {
        final Observer<? super T> actual;

        boolean done;

        Disposable subscription;
        
        long remaining;
        public TakeSubscriber(Observer<? super T> actual, long limit) {
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
