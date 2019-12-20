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
import java.util.function.BiConsumer;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.jdk8.FlowableFromCompletionStage.BiConsumerAtomicReference;

/**
 * Wrap a CompletionStage and signal its outcome.
 * @param <T> the element type of the CompletionsStage
 * @since 3.0.0
 */
public final class CompletableFromCompletionStage<T> extends Completable {

    final CompletionStage<T> stage;

    public CompletableFromCompletionStage(CompletionStage<T> stage) {
        this.stage = stage;
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        // We need an indirection because one can't detach from a whenComplete
        // and cancellation should not hold onto the stage.
        BiConsumerAtomicReference<Object> whenReference = new BiConsumerAtomicReference<>();
        CompletionStageHandler<Object> handler = new CompletionStageHandler<>(observer, whenReference);
        whenReference.lazySet(handler);

        observer.onSubscribe(handler);
        stage.whenComplete(whenReference);
    }

    static final class CompletionStageHandler<T>
    implements Disposable, BiConsumer<T, Throwable> {

        final CompletableObserver downstream;

        final BiConsumerAtomicReference<T> whenReference;

        CompletionStageHandler(CompletableObserver downstream, BiConsumerAtomicReference<T> whenReference) {
            this.downstream = downstream;
            this.whenReference = whenReference;
        }

        @Override
        public void accept(T item, Throwable error) {
            if (error != null) {
                downstream.onError(error);
            } else {
                downstream.onComplete();
            }
        }

        @Override
        public void dispose() {
            whenReference.set(null);
        }

        @Override
        public boolean isDisposed() {
            return whenReference.get() == null;
        }
    }
}
