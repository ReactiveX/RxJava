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

import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.DisposableHelper;
import io.reactivex.rxjava3.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava3.internal.util.*;

/**
 * Merges a Flowable and a Completable by emitting the items of the Flowable and waiting until
 * both the Flowable and Completable complete normally.
 * <p>History: 2.1.10 - experimental
 * @param <T> the element type of the Flowable
 * @since 2.2
 */
public final class FlowableMergeWithCompletable<T> extends AbstractFlowableWithUpstream<T, T> {

    final CompletableSource other;

    public FlowableMergeWithCompletable(Flowable<T> source, CompletableSource other) {
        super(source);
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> subscriber) {
        MergeWithSubscriber<T> parent = new MergeWithSubscriber<T>(subscriber);
        subscriber.onSubscribe(parent);
        source.subscribe(parent);
        other.subscribe(parent.otherObserver);
    }

    static final class MergeWithSubscriber<T> extends AtomicInteger
    implements FlowableSubscriber<T>, Subscription {

        private static final long serialVersionUID = -4592979584110982903L;

        final Subscriber<? super T> downstream;

        final AtomicReference<Subscription> mainSubscription;

        final OtherObserver otherObserver;

        final AtomicThrowable errors;

        final AtomicLong requested;

        volatile boolean mainDone;

        volatile boolean otherDone;

        MergeWithSubscriber(Subscriber<? super T> downstream) {
            this.downstream = downstream;
            this.mainSubscription = new AtomicReference<Subscription>();
            this.otherObserver = new OtherObserver(this);
            this.errors = new AtomicThrowable();
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(mainSubscription, requested, s);
        }

        @Override
        public void onNext(T t) {
            HalfSerializer.onNext(downstream, t, this, errors);
        }

        @Override
        public void onError(Throwable ex) {
            DisposableHelper.dispose(otherObserver);
            HalfSerializer.onError(downstream, ex, this, errors);
        }

        @Override
        public void onComplete() {
            mainDone = true;
            if (otherDone) {
                HalfSerializer.onComplete(downstream, this, errors);
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
            errors.tryTerminateAndReport();
        }

        void otherError(Throwable ex) {
            SubscriptionHelper.cancel(mainSubscription);
            HalfSerializer.onError(downstream, ex, this, errors);
        }

        void otherComplete() {
            otherDone = true;
            if (mainDone) {
                HalfSerializer.onComplete(downstream, this, errors);
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
