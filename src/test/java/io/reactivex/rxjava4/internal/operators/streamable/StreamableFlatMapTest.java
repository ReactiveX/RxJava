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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.core.config.StandardConcurrentConfig;
import io.reactivex.rxjava4.disposables.DisposableContainer;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.schedulers.Schedulers;

@Isolated
public class StreamableFlatMapTest extends StreamableBaseTest {

    @Test
    public void basic() {
        Streamable.range(1, 5)
        .flatMap(v -> Streamable.range(10 * v, 3), StandardConcurrentConfig.DEFAULT)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueSet(10, 11, 12, 20, 21, 22, 30, 31, 32, 40, 41, 42, 50, 51, 52)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void basicDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.range(1, 5)
            .flatMap(v -> Streamable.range(10 * v, 3), StandardConcurrentConfig.DEFAULT)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValueSet(10, 11, 12, 20, 21, 22, 30, 31, 32, 40, 41, 42, 50, 51, 52)
            .assertNoErrors()
            .assertComplete();
        });
    }

    @Test
    public void basicDebugOne() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.just(1)
            .flatMap(v -> Streamable.just(10 * v), StandardConcurrentConfig.DEFAULT)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(10);
        });
    }

    @Test
    public void basicDebugOneInnerMany() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.just(1)
            .flatMap(v -> Streamable.range(10 * v, 3), StandardConcurrentConfig.DEFAULT)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(10, 11, 12);
        });
    }

    @Test
    public void concurrentMany() {
        Streamable.range(1, 5)
        .flatMap(v -> Flowable.range(10 * v, 3).subscribeOn(Schedulers.computation()).toStreamable(), StandardConcurrentConfig.DEFAULT)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueSet(10, 11, 12, 20, 21, 22, 30, 31, 32, 40, 41, 42, 50, 51, 52)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void concurrentOneOne() {
        Streamable.just(1)
        .flatMap(v -> Flowable.just(v * 10).subscribeOn(Schedulers.computation()).toStreamable(), StandardConcurrentConfig.DEFAULT)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(10);
    }

    @Test
    public void concurrentOneOneDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.just(1)
            .flatMap(v -> Flowable.just(v * 10).subscribeOn(Schedulers.computation()).toStreamable(exec), StandardConcurrentConfig.DEFAULT)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(10);
        });
    }

    @Test
    public void concurrentOneToError() {
        Streamable.just(1)
        .flatMap(_ -> Streamable.error(new TestException()), StandardConcurrentConfig.DEFAULT)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void concurrentMainError() {
        Streamable.error(new TestException())
        .flatMap(_ -> Streamable.just(1), StandardConcurrentConfig.DEFAULT)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void basicMaxConcurrent() {
        Streamable.range(1, 5)
        .flatMap(v -> Streamable.range(10 * v, 3), new StandardConcurrentConfig(1))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(10, 11, 12, 20, 21, 22, 30, 31, 32, 40, 41, 42, 50, 51, 52)
        ;
    }

    @Test
    public void mapperNull() {
        Streamable.range(1, 5)
        .flatMap(_ -> null, new StandardConcurrentConfig(1))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class)
        ;
    }

    @Test
    public void mapperCrash() {
        Streamable.range(1, 5)
        .flatMap(_ -> { throw new TestException(); }, new StandardConcurrentConfig(1))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class)
        ;
    }

    @Test
    public void mapperNullDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.range(1, 5)
            .flatMap(_ -> null, new StandardConcurrentConfig(1))
            .test(exec)
            .awaitDone(5, TimeUnit.MINUTES)
            .assertFailure(NullPointerException.class)
            ;
        });
    }

    @Test
    public void mapperFailingFinisher() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.just(1)
            .flatMap(_ -> (Streamable<Object>)c -> new FailingFinishStreamer(c), new StandardConcurrentConfig(1))
            .test(exec)
            .awaitDone(5, TimeUnit.MINUTES)
            .assertFailure(TestException.class)
            ;
        });
    }

    public record FailingFinishStreamer(DisposableContainer canceller) implements Streamer<Object> {

        @Override
        public @NonNull CompletionStage<Boolean> next(@NonNull DisposableContainer cancellation) {
            return NEXT_FALSE;
        }

        @Override
        public @NonNull Object current() {
            return null;
        }

        @Override
        public @NonNull CompletionStage<Void> finish(@NonNull DisposableContainer cancellation) {
            return CompletableFuture.failedFuture(new TestException());
        }
    }
}
