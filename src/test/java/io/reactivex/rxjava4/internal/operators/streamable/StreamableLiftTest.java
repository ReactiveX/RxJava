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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;

public class StreamableLiftTest extends StreamableBaseTest {

    @Test
    public void basic() {
        Streamable.range(1, 5)
        .lift((_, _) -> StreamableError.createFailed(new TestException("normal")))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class)
        .assertError(e -> e.getMessage().equals("normal"));
    }

    @Test
    public void lifterNull() {
        Streamable.range(1, 5)
        .lift((_, _) -> null)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class)
        .assertError(e -> e.getMessage().contains("lifter returned a null"));
    }

    @Test
    public void lifterThrows() {
        Streamable.range(1, 5)
        .lift((_, _) -> { throw new TestException("throws"); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class)
        .assertError(e -> e.getMessage().equals("throws"));
    }
}
