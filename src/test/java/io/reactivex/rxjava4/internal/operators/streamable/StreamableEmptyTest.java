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
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;

public class StreamableEmptyTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.empty()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void collectIndexer() throws Throwable {
        Streamable.empty()
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of());
    }

    @Test
    public void collectEnumerator() throws Throwable {
        Streamable.empty()
        .filter(_ -> true)
        .collect(Collectors.toList())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(List.of());
    }

    @Test
    public void noSuchElementExceptionCurrent() {
        assertThrows(NoSuchElementException.class, () -> {
            StreamableEmpty.EmptyStreamer.INSTANCE.current();
        });
    }
    @Test
    public void noSuchElementExceptionElementAt() {
        assertThrows(NoSuchElementException.class, () -> {
            StreamableEmpty.EmptyStreamer.INSTANCE.elementAt(0L);
        });
    }
}
