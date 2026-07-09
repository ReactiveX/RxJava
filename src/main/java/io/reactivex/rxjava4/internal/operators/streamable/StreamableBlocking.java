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

import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;

import io.reactivex.rxjava4.annotations.*;
import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.disposables.CompositeDisposable;
import io.reactivex.rxjava4.exceptions.Exceptions;
import io.reactivex.rxjava4.internal.util.ExceptionHelper;

public record StreamableBlocking() {

    /**
     * Consumes the first item and finishes the {@link Streamable},
     * throwing {@link NoSuchElementException} if the source is empty.
     * @param <T> the element type
     * @param source the source {@code Streamable}
     * @return the first item
     * @throws RuntimeException if the source signals an unchecked exception
     * @throws CompletionException if the source signals a checked exception
     */
    @CheckReturnValue
    @NonNull
    public static <T> T blockingFirst(Streamable<T> source) {
        var streamer = source.stream(new CompositeDisposable());
        Throwable nextException = null;
        Throwable finishException = null;
        T result = null;
        try {
            if (streamer.awaitNext()) {
                result = streamer.current();
            }
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            nextException = ex;
        }
        try {
            streamer.awaitFinish();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            finishException = ex;
        }

        if (nextException != null || finishException != null) {
            throw ExceptionHelper.wrapOrThrow(ExceptionHelper.unwrapAndCombine(nextException, finishException));
        }
        if (result == null) {
            throw new NoSuchElementException();
        }
        return result;
    }

}
