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
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.*;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamPublisher;
import io.reactivex.rxjava4.internal.subscriptions.SubscriptionHelper;
import io.reactivex.rxjava4.internal.util.ExceptionHelper;
import io.reactivex.rxjava4.internal.virtual.VirtualResumable;

public record StreamableFromPublisher<T>(@NonNull Publisher<T> source)
implements Streamable<T>, HasUpstreamPublisher<T> {

    @Override
    public @NonNull Streamer<@NonNull T> stream(@NonNull DisposableContainer cancellation) {

        var flow = Flowable.fromPublisher(source);
        var streamer = new FlowableStreamer<T>(
                cancellation, new AtomicReference<>(),
                new AtomicReference<>(),
                new AtomicReference<>(),
                new VirtualResumable()
                );
        flow.subscribe(streamer);
        return streamer;
    }

    record FlowableStreamer<T>(
            DisposableContainer cancellation,
            AtomicReference<Subscription> upstream,
            AtomicReference<T> item,
            AtomicReference<Throwable> error,
            VirtualResumable resumer)
    implements Flow.Subscriber<T>, Streamer<T> {

        @Override
        public void onSubscribe(Subscription subscription) {
            if (SubscriptionHelper.setOnce(upstream, subscription)) {
                subscription.request(1); // FIXME more efficient, queueing !!!
            }
        }

        @Override
        public void onNext(T item) {
            this.item.getAndSet(item);
            resumer.resume();
            upstream.get().request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            item.set(null);
            error.getAndSet(throwable);
            upstream.set(SubscriptionHelper.CANCELLED);
            resumer.resume();
        }

        @Override
        public void onComplete() {
            item.set(null);
            error.compareAndSet(null, ExceptionHelper.TERMINATED);
            upstream.set(SubscriptionHelper.CANCELLED);
            resumer.resume();
        }

        @Override
        public @NonNull CompletionStage<Boolean> next(@NonNull DisposableContainer cancellation) {
            return Streamer.runStage(_ -> {
                resumer.await();
                var e = error.get();
                if (e != null) {
                    if (e == ExceptionHelper.TERMINATED) {
                        return false;
                    }
                    throw ExceptionHelper.wrapOrThrow(e);
                }
                return true;
            }, cancellation);
        }

        @Override
        public @NonNull T current() {
            return item.get();
        }

        @Override
        public @NonNull CompletionStage<Void> finish(@NonNull DisposableContainer cancellation) {
            return Streamer.runStage(_ -> {
                upstream.get().cancel();
                return null;
            }, cancellation);
        }
    }
}
