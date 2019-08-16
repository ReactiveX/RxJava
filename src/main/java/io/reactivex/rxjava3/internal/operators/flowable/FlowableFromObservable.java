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
package io.reactivex.rxjava3.internal.operators.flowable;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;

public final class FlowableFromObservable<T> extends Flowable<T> {
    private final Observable<T> upstream;

    public FlowableFromObservable(Observable<T> upstream) {
        this.upstream = upstream;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        upstream.subscribe(new SubscriberObserver<T>(s));
    }

    static final class SubscriberObserver<T> implements Observer<T>, Subscription {

        final Subscriber<? super T> downstream;

        Disposable upstream;

        SubscriberObserver(Subscriber<? super T> s) {
            this.downstream = s;
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void onNext(T value) {
            downstream.onNext(value);
        }

        @Override
        public void onSubscribe(Disposable d) {
            this.upstream = d;
            downstream.onSubscribe(this);
        }

        @Override public void cancel() {
            upstream.dispose();
        }

        @Override
        public void request(long n) {
            // no backpressure so nothing we can do about this
        }
    }
}
