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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.*;

/**
 * Merges a Flowable and a Completable by emitting the items of the Flowable and waiting until
 * both the Flowable and Completable complete normally.
 *
 * @param <T> the element type of the Flowable
 * @since 2.1.10 - experimental
 */
public final class FlowableMergeWithCompletable<T> extends AbstractFlowableWithUpstream<T, T> {

    final CompletableSource other;

    public FlowableMergeWithCompletable(Flowable<T> source, CompletableSource other) {
        super(source);
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> observer) {
        MergeWithSubscriber<T> parent = new MergeWithSubscriber<T>(observer);
        observer.onSubscribe(parent);
        source.subscribe(parent);
        other.subscribe(parent.otherObserver);
    }

    static final class MergeWithSubscriber<T> extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -4592979584110982903L;

        final Subscriber<? super T> actual;

        final AtomicReference<Subscription> mainSubscription;

        final OtherObserver otherObserver;

        final AtomicThrowable error;

        final AtomicLong requested;

        volatile boolean mainDone;

        volatile boolean otherDone;

        MergeWithSubscriber(Subscriber<? super T> actual) {
            this.actual = actual;
            this.mainSubscription = new AtomicReference<Subscription>();
            this.otherObserver = new OtherObserver(this);
            this.error = new AtomicThrowable();
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(Subscription d) {
            SubscriptionHelper.deferredSetOnce(mainSubscription, requested, d);
        }

        @Override
        public void onNext(T t) {
            HalfSerializer.onNext(actual, t, this, error);
        }

        @Override
        public void onError(Throwable ex) {
            SubscriptionHelper.cancel(mainSubscription);
            HalfSerializer.onError(actual, ex, this, error);
        }

        @Override
        public void onComplete() {
            mainDone = true;
            if (otherDone) {
                HalfSerializer.onComplete(actual, this, error);
            }
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(mainSubscription, requested, n);
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(mainSubscription);
            DisposableHelper.dispose(otherObserver);
        }

        void otherError(Throwable ex) {
            SubscriptionHelper.cancel(mainSubscription);
            HalfSerializer.onError(actual, ex, this, error);
        }

        void otherComplete() {
            otherDone = true;
            if (mainDone) {
                HalfSerializer.onComplete(actual, this, error);
            }
        }

        static final class OtherObserver extends AtomicReference<Disposable>
        implements CompletableObserver {

            private static final long serialVersionUID = -2935427570954647017L;

            final MergeWithSubscriber<?> parent;

            OtherObserver(MergeWithSubscriber<?> parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onError(Throwable e) {
                parent.otherError(e);
            }

            @Override
            public void onComplete() {
                parent.otherComplete();
            }
        }
    }
}
