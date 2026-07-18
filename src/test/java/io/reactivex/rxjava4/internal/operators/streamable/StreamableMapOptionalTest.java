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

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.processors.DispatchStreamProcessor;
import io.reactivex.rxjava4.schedulers.Schedulers;

public class StreamableMapOptionalTest extends StreamableBaseTest {

    @Test
    public void basic() {
        Streamable.range(1, 5)
        .mapOptional(v -> Optional.of(v.toString()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("1", "2", "3", "4", "5");
    }

    @Test
    public void basicDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.range(1, 5)
            .mapOptional(v -> Optional.of(v.toString()))
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult("1", "2", "3", "4", "5");
        });
    }

    @Test
    public void allEmpty() {
        Streamable.range(1, 5)
        .mapOptional(_ -> Optional.empty())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void someEmpty() {
        Streamable.range(1, 5)
        .mapOptional(v -> v % 2 == 0 ? Optional.of(v.toString()) : Optional.empty())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("2", "4");
    }

    @Test
    public void middleEmpty() {
        Streamable.range(1, 5)
        .mapOptional(v -> v == 1 || v == 5 ? Optional.of(v.toString()) : Optional.empty())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("1", "5");
    }

    @Test
    public void mapperNull() {
        Streamable.range(1, 5)
        .mapOptional(_ -> null)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void mapperCrash() {
        Streamable.range(1, 5)
        .mapOptional(_ -> { throw new TestException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void sourceError() {
        Streamable.error(new TestException())
        .mapOptional(v -> Optional.of(v))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    public void intervalRange() {
        Streamable.intervalRange(1, 5, 1, 1, TimeUnit.MILLISECONDS, Schedulers.single())
        .mapOptional(v -> Optional.of(v.toString()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult("1", "2", "3", "4", "5");
    }

    @Test
    public void delay() {
        Streamable.range(1, 5)
        .delay(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .mapOptional(v -> Optional.of(v.toString()))
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of("1", "2", "3", "4", "5"));
    }

    @Test
    public void delayNull() {
        Streamable.range(1, 5)
        .delay(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .mapOptional(_ -> null)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void delayCrash() {
        Streamable.range(1, 5)
        .delay(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .mapOptional(_ -> { throw new TestException(); })
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void delayMIxed() {
        Streamable.range(1, 5)
        .delay(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .mapOptional(v -> v % 2 == 0 ? Optional.of(v.toString()) : Optional.empty())
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of("2", "4"));
    }

    @Test
    public void asyncError() throws Throwable {
        var dsp = new DispatchStreamProcessor<Integer>();

        var ts = dsp
        .mapOptional(v -> v % 2 == 0 ? Optional.of(v.toString()) : Optional.empty())
        .test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000);

        dsp.next(1).toCompletableFuture().join();
        dsp.finish(new TestException()).toCompletableFuture().join();

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void asyncEmpty() throws Throwable {
        var dsp = new DispatchStreamProcessor<Integer>();

        var ts = dsp
        .mapOptional(v -> v % 2 == 0 ? Optional.of(v.toString()) : Optional.empty())
        .test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000);

        dsp.next(1).toCompletableFuture().join();
        dsp.finish(null).toCompletableFuture().join();

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void enumerable() {
        Streamable.range(1, 5)
        .mapOptional(v -> Optional.of(v.toString()))
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of("1", "2", "3", "4", "5"));
    }

    @Test
    public void enumerable2() {
        Streamable.fromIterable(List.of(1, 2, 3, 4, 5))
        .mapOptional(v -> Optional.of(v.toString()))
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of("1", "2", "3", "4", "5"));
    }

    @Test
    public void enumerable3() {
        Streamable.fromIterable(List.of(1, 2, 3, 4, 5))
        .mapOptional(v -> v % 2 == 0 ? Optional.of(v.toString()) : Optional.empty())
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of("2", "4"));
    }

    @Test
    public void deferredEnumerable() {
        Single.just(1)
        .flattenAsStreamable(v -> List.of(v, 2, 3, 4, 5))
        .mapOptional(v -> v % 2 == 0 ? Optional.of(v.toString()) : Optional.empty())
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of("2", "4"));
    }

}
