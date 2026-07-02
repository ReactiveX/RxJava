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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import io.reactivex.rxjava4.core.Streamable;

public class StreamableRangeTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.range(1, 3)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);
    }

    @Test
    public void normal1() throws Throwable {
        Streamable.range(1, 1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void normal0() throws Throwable {
        Streamable.range(1, 0)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void normalLong() throws Throwable {
        Streamable.rangeLong(1, 3)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1L, 2L, 3L);
    }

    @Test
    public void normal1Long() throws Throwable {
        Streamable.rangeLong(1, 1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1L);
    }

    @Test
    public void normal0Long() throws Throwable {
        Streamable.rangeLong(1, 0)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void underflow() throws Throwable {
        assertThrows(IllegalArgumentException.class, () -> {
            Streamable.range(1, -1);
        });
    }

    @Test
    public void underOverflow() throws Throwable {
        assertThrows(IllegalArgumentException.class, () -> {
            Streamable.range(2, Integer.MAX_VALUE);
        });
    }

    @Test
    public void underflowLong() throws Throwable {
        assertThrows(IllegalArgumentException.class, () -> {
            Streamable.rangeLong(1, -1);
        });
    }

    @Test
    public void underOverflowLong() throws Throwable {
        assertThrows(IllegalArgumentException.class, () -> {
            Streamable.rangeLong(2, Long.MAX_VALUE);
        });
    }

    @Test
    public void underNoOverflowLong() throws Throwable {
        Streamable.rangeLong(-2, Long.MAX_VALUE);
    }
}
