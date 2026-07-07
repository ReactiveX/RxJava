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

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;

public class StreamableUsingTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        var resource = new AtomicReference<Integer>();
        Streamable.using(() -> 2, v -> Streamable.range(v, 5), resource::set)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(2, 3, 4, 5, 6);

        assertEquals(2, resource.get(), "resource cleanup mismatch");
    }

    @Test
    public void nulLResourceAllowed() throws Throwable {
        var resource = new AtomicReference<Integer>(3);
        Streamable.using(() -> null, v -> Streamable.range(v != null ? v : 0, 5), resource::set)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0, 1, 2, 3, 4);

        assertNull(resource.get(), "resource cleanup mismatch");
    }

    @Test
    public void resourceCrash() throws Throwable {
        var resource = new AtomicReference<Integer>();
        Streamable.using(() -> { throw new TestException(); }, v -> Streamable.range(v, 5), resource::set)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);

        assertNull(resource.get(), "resource is not null?");
    }

    @Test
    public void resourceMapperNull() throws Throwable {
        var resource = new AtomicReference<Integer>();
        Streamable.using(() -> 3, _ -> null, resource::set)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);

        assertEquals(3, resource.get(), "resource cleanup mismatch");
    }

    @Test
    public void resourceMapperCrash() throws Throwable {
        var resource = new AtomicReference<Integer>();
        Streamable.using(() -> 4, _ -> { throw new TestException(); }, resource::set)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);

        assertEquals(4, resource.get(), "resource cleanup mismatch");
    }

    @Test
    public void resourceCleanerCrash() throws Throwable {
        Streamable.using(() -> 5, v -> Streamable.range(v, 5), _ -> { throw new TestException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class, 5, 6, 7, 8, 9);
    }
}
