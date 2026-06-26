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
import io.reactivex.rxjava4.disposables.DisposableContainer;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamPublisher;
import io.reactivex.rxjava4.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava4.internal.util.*;
import io.reactivex.rxjava4.internal.virtual.VirtualResumable;

public record StreamableFromPublisher<T>(@NonNull Publisher<T> source,
        @Nullable Executor executor)
implements Streamable<T>, HasUpstreamPublisher<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull DisposableContainer cancellation) {

        var flow = Flowable.fromPublisher(source);
        var streamer = new FlowableStreamer<T>(
                cancellation,
                new AtomicReference<>(),
                new AtomicLong(),
                new AtomicReference<>(),
                new AtomicReference<>(),
                new VirtualResumable(),
                executor
                );
        flow.subscribe(streamer);
        return streamer;
    }

    record FlowableStreamer<T>(
            DisposableContainer cancellation,
            AtomicReference<Subscription> upstream,
            AtomicLong requester,
            AtomicReference<T> item,
            AtomicReference<Throwable> error,
            VirtualResumable resumer,
            Executor executor)
    implements Flow.Subscriber<T>, Streamer<T> {

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
        public @NonNull CompletionStage<Boolean> next(@NonNull DisposableContainer canceller) {
            // System.out.println("next()");
            return AwaitCoordinatorStatic.runStage(_ -> {
                item.lazySet(null);
                // System.out.println("Requesting the next item");
                SubscriptionHelper.deferredRequest(upstream, requester, 1);

                // System.out.println("waiting for it");

                var e = error.get();
                var v = item.get();

                do {
                    // System.out.println("1");
                    if (e != null || v != null) {
                        break;
                    }

                    resumer.await();

                    e = error.get();
                    v = item.get();

                    // System.out.println("Loop | Value: " + v + ", Error: " + e);

                } while (!canceller.isDisposed());

                // Because Eclipse craps itself when trying to debug virtual threads
                // FU whoever said debugging in virtual threads is straightforward
                // System.out.println("Value: " + v + ", Error: " + e);
                // if (e != null) {
                //    e.printStackTrace();
                // }

                if (v == null) {
                    if (e != null) {
                        if (e == ExceptionHelper.TERMINATED) {
                            return false;
                        }
                        throw ExceptionHelper.wrapOrThrow(e);
                    }
                    throw new IllegalStateException("null current item and null current error? How?");
                }
                // System.out.println("Returning true");
                return true;
            }, canceller, executor);
        }

        @Override
        public @NonNull T current() {
            return item.get();
        }

        @Override
        public @NonNull CompletionStage<Void> finish(@NonNull DisposableContainer cancellation) {
            // new Exception("StreamableFromPublisher::finish").printStackTrace();
            return AwaitCoordinatorStatic.runStage(_ -> {
                SubscriptionHelper.cancel(upstream);
                return null;
            }, cancellation, executor);
        }
    }
}
