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

package io.reactivex.internal.operators.completable;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.internal.subscriptions.SubscriptionHelper;

public final class CompletableFromPublisher<T> extends Completable {

    final Publisher<T> flowable;

    public CompletableFromPublisher(Publisher<T> flowable) {
        this.flowable = flowable;
    }

    @Override
    protected void subscribeActual(final CompletableObserver cs) {
        flowable.subscribe(new FromPublisherSubscriber<T>(cs));
    }

    static final class FromPublisherSubscriber<T> implements FlowableSubscriber<T>, Disposable {

        final CompletableObserver cs;

        Subscription s;

        FromPublisherSubscriber(CompletableObserver actual) {
            this.cs = actual;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;

                cs.onSubscribe(this);

                s.request(Long.MAX_VALUE);
            }
        }


        @Override
        public void onNext(T t) {
            // ignored
        }

        @Override
        public void onError(Throwable t) {
            cs.onError(t);
        }

        @Override
        public void onComplete() {
            cs.onComplete();
        }

        @Override
        public void dispose() {
            s.cancel();
            s = SubscriptionHelper.CANCELLED;
        }

        @Override
        public boolean isDisposed() {
            return s == SubscriptionHelper.CANCELLED;
        }
    }

}
