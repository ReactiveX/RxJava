/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

import io.reactivex.*;
import io.reactivex.disposables.Disposable;

public final class FlowableFromObservable<T> extends Flowable<T> {
    private final Observable<T> upstream;

    public FlowableFromObservable(Observable<T> upstream) {
        this.upstream = upstream;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        upstream.subscribe(new SubscriberObserver<T>(s));
    }

    static class SubscriberObserver<T> implements Observer<T>, Subscription {
        private final Subscriber<? super T> s;
        private Disposable d;

        SubscriberObserver(Subscriber<? super T> s) {
            this.s = s;
        }

        @Override
        public void onComplete() {
            s.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            s.onError(e);
        }

        @Override
        public void onNext(T value) {
            s.onNext(value);
        }

        @Override
        public void onSubscribe(Disposable d) {
            this.d = d;
            s.onSubscribe(this);
        }

        @Override public void cancel() {
            d.dispose();
        }

        @Override
        public void request(long n) {
            // no backpressure so nothing we can do about this
        }
    }
}
