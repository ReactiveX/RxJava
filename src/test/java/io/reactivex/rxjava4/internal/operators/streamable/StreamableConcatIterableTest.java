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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.schedulers.Schedulers;

public class StreamableConcatIterableTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.concat(List.of(Streamable.range(1, 5), Streamable.range(6, 5), Streamable.range(11, 5)))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(
                1, 2, 3, 4, 5,
                6, 7, 8, 9, 10,
                11, 12, 13, 14, 15
                );
    }

    @Test
    public void normalHidden() throws Throwable {
        Streamable.concat(List.of(Streamable.range(1, 5).hide(), Streamable.range(6, 5).hide(), Streamable.range(11, 5).hide()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(
                1, 2, 3, 4, 5,
                6, 7, 8, 9, 10,
                11, 12, 13, 14, 15
                );
    }

    @Test
    public void normalMixedHidden1() throws Throwable {
        Streamable.concat(List.of(Streamable.range(1, 5).hide(), Streamable.range(6, 5), Streamable.range(11, 5).hide()))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(
                1, 2, 3, 4, 5,
                6, 7, 8, 9, 10,
                11, 12, 13, 14, 15
                );
    }

    @Test
    public void normalHidden2() throws Throwable {
        Streamable.concat(List.of(Streamable.range(1, 5), Streamable.range(6, 5).hide(), Streamable.range(11, 5)))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(
                1, 2, 3, 4, 5,
                6, 7, 8, 9, 10,
                11, 12, 13, 14, 15
                );
    }

    @Test
    public void crash() throws Throwable {
        Streamable.concat(List.of(Streamable.range(1, 5), Streamable.error(new TestException()), Streamable.range(11, 5)))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class,
                1, 2, 3, 4, 5);
    }

    @Test
    public void none() throws Throwable {
        Streamable.concat(List.of())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void solo() throws Throwable {
        Streamable.concat(List.of(Streamable.range(1, 5)))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void nullStreamable() throws Throwable {
        Streamable.concat(Arrays.asList(Streamable.range(1, 5), null))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void lot() {
        var n = 10_000;
        List<Streamable<Integer>> sources = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            sources.add(Streamable.just(i));
        }

        var ts = Streamable.concat(sources)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(n)
        .assertNoErrors()
        .assertComplete();

        for (int i = 0; i < n; i++) {
            assertEquals(i, ts.values().get(i));
        }
    }

    @Test
    public void finishCrash() throws Throwable {
        Streamable.concat(List.of(Streamable.empty(), StreamableFailingFinish.MAIN_COMPLETES))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void finishCrashDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.concat(List.of(Streamable.empty(), StreamableFailingFinish.MAIN_COMPLETES))
            .test(exec)
            .awaitDone(5, TimeUnit.MINUTES)
            .assertFailure(TestException.class);
        });
    }

    @Test
    public void lotEmpties() {
        var n = 10_000;
        List<Streamable<Integer>> sources = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            sources.add(Streamable.empty());
        }

        Streamable.concat(sources)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void delayed() throws Throwable {
        Streamable.concat(List.of(
                Streamable.range(1, 5),
                Streamable.range(6, 5).delay(1, TimeUnit.MILLISECONDS, Schedulers.single()),
                Streamable.range(11, 5)))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(
                1, 2, 3, 4, 5,
                6, 7, 8, 9, 10,
                11, 12, 13, 14, 15
                );
    }

    @Test
    public void intervalRange() throws Throwable {
        Streamable.concat(List.of(
                Streamable.intervalRange(1, 5, 1, 1, TimeUnit.MILLISECONDS, Schedulers.single()),
                Streamable.intervalRange(6, 5, 1, 1, TimeUnit.MILLISECONDS, Schedulers.single()),
                Streamable.intervalRange(11, 5, 1, 1, TimeUnit.MILLISECONDS, Schedulers.single())))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(
                1L, 2L, 3L, 4L, 5L,
                6L, 7L, 8L, 9L, 10L,
                11L, 12L, 13L, 14L, 15L
                );
    }

    @Test
    public void virtualCreate() {
        Streamable.concat(List.of(Streamable.empty(),
            Streamable.create(emitter -> {
                emitter.emit(1);
                emitter.emit(2);
                emitter.emit(3);
                emitter.emit(4);
                emitter.emit(5);
            }),
            Streamable.create(_ -> {
            })
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void virtualCreate2() {
        Streamable.concat(List.of(Streamable.empty(),
            Streamable.create(emitter -> {
                emitter.emit(1);
                emitter.emit(2);
                emitter.emit(3);
                emitter.emit(4);
                emitter.emit(5);
            }),
            Streamable.create(emitter -> {
                emitter.emit(6);
                emitter.emit(7);
            })
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5, 6, 7);
    }

    @Test
    public void virtualCreateNull() {
        Streamable.concat(Arrays.asList(Streamable.empty(),
            Streamable.create(emitter -> {
                emitter.emit(1);
                emitter.emit(2);
                emitter.emit(3);
                emitter.emit(4);
                emitter.emit(5);
            }),
            Streamable.create(_ -> {
            }),
            null
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void virtualCreateNullDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.concat(Arrays.asList(Streamable.empty(),
                    Streamable.create(emitter -> {
                        emitter.emit(1);
                        emitter.emit(2);
                        emitter.emit(3);
                        emitter.emit(4);
                        emitter.emit(5);
                    }, exec),
                    Streamable.create(_ -> {
                    }, exec),
                    null
                ))
                .test(exec)
                .awaitDone(500, TimeUnit.SECONDS)
                .assertFailure(NullPointerException.class, 1, 2, 3, 4, 5);
        });
    }

    @Test
    public void virtualCreateError() {
        Streamable.concat(Arrays.asList(Streamable.empty(),
            Streamable.create(emitter -> {
                emitter.emit(1);
                emitter.emit(2);
                emitter.emit(3);
                emitter.emit(4);
                emitter.emit(5);
            }),
            Streamable.create(_ -> {
            }),
            Streamable.error(new TestException())
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void virtualCreateFinishFail() {
        Streamable.concat(Arrays.asList(Streamable.empty(),
            Streamable.create(emitter -> {
                emitter.emit(1);
                emitter.emit(2);
                emitter.emit(3);
                emitter.emit(4);
                emitter.emit(5);
            }),
            Streamable.create(_ -> {
            }),
            StreamableFailingFinish.MAIN_COMPLETES
        ))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class, 1, 2, 3, 4, 5);
    }

    @Test
    public void delay() throws Throwable {
        Streamable.concat(List.of(
                Streamable.range(1, 5),
                Streamable.range(6, 5).delay(1, TimeUnit.MILLISECONDS, Schedulers.single()),
                Streamable.range(11, 5)))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(
                1, 2, 3, 4, 5,
                6, 7, 8, 9, 10,
                11, 12, 13, 14, 15
                );
    }

}
