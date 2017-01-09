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

package io.reactivex.internal.observers;

import org.reactivestreams.*;

import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

public final class SubscriberCompletableObserver<T> implements CompletableObserver, Subscription {
    final Subscriber<? super T> subscriber;

    Disposable d;

    public SubscriberCompletableObserver(Subscriber<? super T> observer) {
        this.subscriber = observer;
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }

    @Override
    public void onError(Throwable e) {
        subscriber.onError(e);
    }

    @Override
    public void onSubscribe(Disposable d) {
        if (DisposableHelper.validate(this.d, d)) {
            this.d = d;

            subscriber.onSubscribe(this);
        }
    }

    @Override
    public void request(long n) {
        // ignored, no values emitted anyway
    }

    @Override
    public void cancel() {
        d.dispose();
    }
}
