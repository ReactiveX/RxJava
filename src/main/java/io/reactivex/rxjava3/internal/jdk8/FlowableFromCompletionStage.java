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
package io.reactivex.rxjava3.internal.jdk8;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.internal.subscriptions.DeferredScalarSubscription;

/**
 * Wrap a CompletionStage and signal its outcome.
 * @param <T> the element type
 * @since 3.0.0
 */
public final class FlowableFromCompletionStage<T> extends Flowable<T> {

    final CompletionStage<T> stage;

    public FlowableFromCompletionStage(CompletionStage<T> stage) {
        this.stage = stage;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        // We need an indirection because one can't detach from a whenComplete
        // and cancellation should not hold onto the stage.
        BiConsumerAtomicReference<T> whenReference = new BiConsumerAtomicReference<>();
        CompletionStageHandler<T> handler = new CompletionStageHandler<>(s, whenReference);
        whenReference.lazySet(handler);

        s.onSubscribe(handler);
        stage.whenComplete(whenReference);
    }

    static final class CompletionStageHandler<T>
    extends DeferredScalarSubscription<T>
    implements BiConsumer<T, Throwable> {

        private static final long serialVersionUID = 4665335664328839859L;

        final BiConsumerAtomicReference<T> whenReference;

        CompletionStageHandler(Subscriber<? super T> downstream, BiConsumerAtomicReference<T> whenReference) {
            super(downstream);
            this.whenReference = whenReference;
        }

        @Override
        public void accept(T item, Throwable error) {
            if (error != null) {
                downstream.onError(error);
            }
            else if (item != null) {
                complete(item);
            } else {
                downstream.onError(new NullPointerException("The CompletionStage terminated with null."));
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            whenReference.set(null);
        }
    }

    static final class BiConsumerAtomicReference<T> extends AtomicReference<BiConsumer<T, Throwable>>
    implements BiConsumer<T, Throwable> {

        private static final long serialVersionUID = 45838553147237545L;

        @Override
        public void accept(T t, Throwable u) {
            BiConsumer<T, Throwable> biConsumer = get();
            if (biConsumer != null) {
                biConsumer.accept(t, u);
            }
        }
    }
}
