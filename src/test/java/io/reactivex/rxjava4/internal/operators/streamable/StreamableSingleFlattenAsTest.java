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

import io.reactivex.rxjava4.core.Single;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.subjects.SingleSubject;

public class StreamableSingleFlattenAsTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Single.just(1)
        .flattenAsStreamable(v -> List.of(v * 10, v * 20, v * 30, v * 40, v * 50))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(10, 20, 30, 40, 50);
    }

    @Test
    public void normalDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Single.just(1)
            .flattenAsStreamable(v -> List.of(v * 10, v * 20, v * 30, v * 40, v * 50))
            .test(exec)
            .awaitDone(500, TimeUnit.SECONDS)
            .assertResult(10, 20, 30, 40, 50);
        });
    }

    @Test
    public void error() throws Throwable {
        Single.<Integer>error(new TestException())
        .flattenAsStreamable(v -> List.of(v * 10, v * 20, v * 30, v * 40, v * 50))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperNull() throws Throwable {
        Single.just(1)
        .flattenAsStreamable(_ -> null)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void mapperCrash() throws Throwable {
        Single.just(1)
        .flattenAsStreamable(_ -> { throw new TestException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyInner() throws Throwable {
        Single.just(1)
        .flattenAsStreamable(_ -> List.of())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void cancel() throws Throwable {
        var ss = SingleSubject.create();

        var to = ss.flattenAsStreamable(_ -> List.of())
        .test();

        to.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitCondition(true, () -> ss.hasObservers(), 1000);

        to.cancel();

        awaitCondition(false, () -> ss.hasObservers(), 1000);
    }

    @Test
    public void deferredEnumerable() throws Throwable {
        Single.just(List.of(1, 2, 3, 4, 5))
        .flattenAsStreamable(v -> v)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1, 2, 3, 4, 5));
    }
}
