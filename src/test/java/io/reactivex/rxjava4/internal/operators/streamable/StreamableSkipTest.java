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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.processors.DispatchStreamProcessor;
import io.reactivex.rxjava4.schedulers.Schedulers;

public class StreamableSkipTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.range(1, 5)
        .skip(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(3, 4, 5);
    }

    @Test
    public void doubleSkip() throws Throwable {
        Streamable.range(1, 5)
        .skip(2)
        .skip(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(5);
    }

    @Test
    public void crash() throws Throwable {
        Streamable.error(new TestException())
        .skip(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void zeroSkip() throws Throwable {
        Streamable.range(1, 5)
        .skip(0)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void empty() throws Throwable {
        Streamable.empty()
        .skip(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void skipAll() throws Throwable {
        Streamable.range(1, 5)
        .skip(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void skipMore() throws Throwable {
        Streamable.range(1, 5)
        .skip(6)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void normal2() throws Throwable {
        Streamable.range(1, 5)
        .hide()
        .skip(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(3, 4, 5);
    }

    @Test
    public void normalIndexed() throws Throwable {
        Streamable.range(1, 5)
        .skip(2)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(3, 4, 5));
    }

    @Test
    public void normalEnumerable() throws Throwable {
        Streamable.range(1, 5)
        .filter(v -> v >= 1)
        .skip(2)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(3, 4, 5));
    }

    @Test
    public void normalEnumerable2() throws Throwable {
        Streamable.range(1, 5)
        .filter(v -> v >= 4)
        .skip(3)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of());
    }

    @Test
    public void normalDeferredEnumerable() throws Throwable {
        Single.just(1)
        .flattenAsStreamable(v -> List.of(v, v + 1, v + 2, v + 3, v + 4))
        .skip(2)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(3, 4, 5));
    }

    @Test
    public void normalDeferredEnumerable2() throws Throwable {
        Single.just(1)
        .flattenAsStreamable(v -> List.of(v, v + 1, v + 2, v + 3, v + 4))
        .skip(6)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of());
    }

    @Test
    public void delayed() throws Throwable {
        Streamable.range(1, 5)
        .delay(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .skip(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(3, 4, 5);
    }

    @Test
    public void intervalRange() throws Throwable {
        Streamable.intervalRange(1, 5, 1, 1, TimeUnit.MILLISECONDS, Schedulers.single())
        .skip(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(3L, 4L, 5L);
    }

    @Test
    public void dispatch() throws Throwable {
        var dsp = new DispatchStreamProcessor<>();

        var ts = dsp.skip(2).test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);
        awaitStreamers(dsp, 1000);

        dsp.next(1).toCompletableFuture().join();
        dsp.next(2).toCompletableFuture().join();
        dsp.next(3).toCompletableFuture().join();
        dsp.next(4).toCompletableFuture().join();
        dsp.finish(null).toCompletableFuture().join();

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(3, 4);
    }

    @Test
    public void dispatchError() throws Throwable {
        var dsp = new DispatchStreamProcessor<>();

        var ts = dsp.skip(2).test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);
        awaitStreamers(dsp, 1000);

        dsp.next(1).toCompletableFuture().join();
        dsp.next(2).toCompletableFuture().join();
        dsp.next(3).toCompletableFuture().join();
        dsp.next(4).toCompletableFuture().join();
        dsp.finish(new TestException()).toCompletableFuture().join();

        ts
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class, 3, 4);
    }

    @Test
    public void asyncCompletion() throws Throwable {
        Streamable.fromCompletable(Completable.complete().delay(1, TimeUnit.MILLISECONDS))
        .skip(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void asyncError() throws Throwable {
        Streamable.fromCompletable(Completable.error(new TestException()).delay(1, TimeUnit.MILLISECONDS))
        .skip(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

}
