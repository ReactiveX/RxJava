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
import io.reactivex.rxjava4.schedulers.Schedulers;

public class StreamableFilterTest extends StreamableBaseTest {

    @Test
    public void basic() {
        Streamable.range(1, 5)
        .filter(_ -> true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void basicDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.range(1, 5)
            .filter(_ -> true)
            .test(exec)
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5);
        });
    }

    @Test
    public void allEmpty() {
        Streamable.range(1, 5)
        .filter(_ -> false)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void someEmpty() {
        Streamable.range(1, 5)
        .filter(v -> v % 2 == 0)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2, 4);
    }

    @Test
    public void middleEmpty() {
        Streamable.range(1, 5)
        .filter(v -> v == 1 || v == 5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 5);
    }

    @Test
    public void mapperCrash() {
        Streamable.range(1, 5)
        .filter(_ -> { throw new TestException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void sourceError() {
        Streamable.error(new TestException())
        .filter(_ -> true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void sourceError2() {
        Streamable.error(new TestException())
        .filter(_ -> false)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void delayed() {
        Streamable.range(1, 5)
        .delay(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .filter(_ -> true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void delayed2() {
        Streamable.range(1, 100)
        .delay(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .filter(_ -> false)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void longSync() {
        Streamable.range(1, 1_000_000)
        .filter(v -> (v & 1) != 0)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(500_000)
        .assertNoErrors()
        .assertComplete()
        ;
    }

    @Test
    public void delayedCrash() {
        Streamable.range(1, 5)
        .delay(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .filter(_ -> { throw new TestException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void delayed3() {
        Streamable.range(1, 100)
        .delay(1, TimeUnit.MILLISECONDS, Schedulers.single())
        .filter(_ -> true)
        .filter(_ -> false)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void timeout1() {
        Streamable.never()
        .timeout(1, TimeUnit.MILLISECONDS, Schedulers.single(), Streamable.empty())
        .filter(_ -> true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void timeout2() {
        Streamable.never()
        .timeout(1, TimeUnit.MILLISECONDS, Schedulers.single(), Streamable.error(new TestException()))
        .filter(_ -> true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void basicHidden() {
        Streamable.range(1, 5)
        .hide()
        .filter(_ -> true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void basicHidden2() {
        Streamable.fromIterable(List.of(1, 2, 3, 4, 5))
        .hide()
        .filter(_ -> true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void indexable() {
        Streamable.range(1, 5)
        .filter(_ -> true)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1, 2, 3, 4, 5));
    }

    @Test
    public void indexable2() {
        Streamable.range(1, 5)
        .filter(_ -> true)
        .collect(Collectors.toList())
        .filter(_ -> true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1, 2, 3, 4, 5));
    }

    @Test
    public void enumerable3() {
        Streamable.fromIterable(List.of(1, 2, 3, 4, 5))
        .filter(_ -> true)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1, 2, 3, 4, 5));
    }

    @Test
    public void enumerable4() {
        Streamable.fromIterable(List.of(1, 2, 3, 4, 5))
        .filter(_ -> true)
        .collect(Collectors.toList())
        .filter(_ -> true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1, 2, 3, 4, 5));
    }

    @Test
    public void enumerable5() {
        Streamable.fromIterable(List.of(1, 2, 3, 4, 5))
        .filter(_ -> false)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of());
    }

    @Test
    public void enumerable6() {
        Streamable.fromIterable(List.of(1, 2, 3, 4, 5))
        .filter(_ -> false)
        .collect(Collectors.toList())
        .filter(_ -> false)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void enumerable7() {
        Streamable.fromIterable(List.of(1, 2, 3, 4, 5))
        .filter(_ -> false)
        .collect(Collectors.toList())
        .filter(_ -> false)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of());
    }

    @Test
    public void deferredEnumerable() {
        Single.just(1)
        .flattenAsStreamable(v -> List.of(v, v + 1, v + 2, v + 3, v + 4))
        .filter(_ -> true)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(1, 2, 3, 4, 5));
    }

    @Test
    public void deferredEnumerable2() {
        Single.just(1)
        .flattenAsStreamable(v -> List.of(v, v + 1, v + 2, v + 3, v + 4))
        .filter(v -> v % 2 == 0)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of(2, 4));
    }
}
