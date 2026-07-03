/*
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

package io.reactivex.rxjava4.internal.operators.streamable;

import java.util.concurrent.*;
import java.util.concurrent.Flow.*;
import java.util.concurrent.atomic.*;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamPublisher;
import io.reactivex.rxjava4.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava4.internal.util.ExceptionHelper;
import io.reactivex.rxjava4.internal.virtual.VirtualResumable;

public record StreamableFromPublisher<T>(@NonNull Publisher<T> source,
        @Nullable Executor executor)
implements Streamable<T>, HasUpstreamPublisher<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull DisposableContainer cancellation) {

        var flow = Flowable.fromPublisher(source);
        var streamer = new FlowableStreamer<T>(
                new AtomicReference<>(),
                new AtomicLong(),
                new AtomicReference<>(),
                new AtomicReference<>(),
                new VirtualResumable(),
                executor
                );
        cancellation.add(streamer);
        flow.subscribe(streamer);
        return streamer;
    }

    record FlowableStreamer<T>(
            AtomicReference<Subscription> upstream,
            AtomicLong requester,
            AtomicReference<T> item,
            AtomicReference<Throwable> error,
            VirtualResumable resumer,
            Executor executor)
    implements Flow.Subscriber<T>, Streamer<T>, Disposable, java.util.function.Supplier<Boolean>, Runnable {

        @Override
        public void onSubscribe(Subscription subscription) {
            // System.out.println("onSubscribe | " + subscription);
            SubscriptionHelper.deferredSetOnce(upstream, requester, subscription);
        }

        @Override
        public void onNext(T item) {
            // System.out.println("onNext | " + item);
            this.item.getAndSet(item);
            resumer.resume();
            // System.out.println("Got " + item + " resume signaled");
        }

        @Override
        public void onError(Throwable throwable) {
            // System.out.println("onError | " + throwable);
            error.getAndSet(throwable);
            upstream.set(SubscriptionHelper.CANCELLED);
            resumer.resume();
        }

        @Override
        public void onComplete() {
            // System.out.println("onComplete |");
            error.compareAndSet(null, ExceptionHelper.TERMINATED);
            upstream.set(SubscriptionHelper.CANCELLED);
            resumer.resume();
        }

        @Override
        public void dispose() {
            if (SubscriptionHelper.cancel(upstream)) {
                resumer.resume();
            }
        }

        @Override
        public boolean isDisposed() {
            return SubscriptionHelper.CANCELLED == upstream.get();
        }

        @Override
        public @NonNull CompletionStage<Boolean> next() {
            return CompletableFuture.supplyAsync(this, executor);
        }

        @Override
        public Boolean get() {
            item.lazySet(null);
            SubscriptionHelper.deferredRequest(upstream, requester, 1);
            if (!isDisposed()) {
                resumer.await();
            }

            if (item.get() != null) {
                return true;
            }
            var err = error.get();
            if (err == ExceptionHelper.TERMINATED) {
                return false;
            }
            if (err == null && isDisposed()) {
                throw new CancellationException();
            }
            throw ExceptionHelper.wrapOrThrow(err);
        }

        @Override
        public @NonNull T current() {
            return item.get();
        }

        @Override
        public @NonNull CompletionStage<Void> finish() {
            return CompletableFuture.runAsync(this);
        }

        @Override
        public void run() {
            if (SubscriptionHelper.cancel(upstream)) {
                item.lazySet(null);
            }
        }
    }
}
