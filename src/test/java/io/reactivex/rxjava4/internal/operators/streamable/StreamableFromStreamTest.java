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

import org.junit.jupiter.api.Test;
import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;

public class StreamableFromStreamTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.fromIterable(List.of(1, 2, 3))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);
    }

    @Test
    public void empty() throws Throwable {
        Streamable.fromIterable(List.of())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void one() throws Throwable {
        Streamable.fromIterable(List.of(1))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void hasNull() throws Throwable {
        Streamable.fromIterable(Arrays.asList(1, null, 3))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class, 1)
        .assertError(t -> t.getMessage().equals("Item at index 1 is null."));
        ;
    }

    @Test
    public void hasNull2() throws Throwable {
        Streamable.fromIterable(Arrays.asList(null, 1, 2, 3))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class)
        .assertError(t -> t.getMessage().equals("Item at index 0 is null."));
        ;
    }

    @Test
    public void iteratorThrows() throws Throwable {
        Streamable.fromIterable(() -> { throw new TestException("test"); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class)
        .assertError(t -> t.getMessage().equals("test"));
        ;
    }
}
