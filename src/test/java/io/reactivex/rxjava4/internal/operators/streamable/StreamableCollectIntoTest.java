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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;

public class StreamableCollectIntoTest extends StreamableBaseTest {

    @Test
    public void multipleItems() throws Throwable {
        Streamable.range(1, 5)
        .collectInto(new ArrayList<Integer>(), (list, item) -> list.add(item))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(new ArrayList<>(List.of(1, 2, 3, 4, 5)));
    }

    @Test
    public void empty() throws Throwable {
        Streamable.<Integer>empty()
        .collectInto(new ArrayList<Integer>(), (list, item) -> list.add(item))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(new ArrayList<Integer>());
    }

    @Test
    public void error() throws Throwable {
        Streamable.<Integer>error(new TestException("error"))
        .collectInto(new ArrayList<Integer>(), (list, item) -> list.add(item))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void nullArguments() {
        assertThrows(NullPointerException.class, () -> Streamable.<Integer>empty().collectInto((ArrayList<Integer>) null, (list, item) -> list.add(item)));
        assertThrows(NullPointerException.class, () -> Streamable.empty().collectInto(new ArrayList<Integer>(), null));
    }
}
