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

public class StreamableSkipTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.range(1, 5)
        .skip(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(3, 4, 5);
    }

    @Test
    public void doubleSkip() throws Throwable {
        Streamable.range(1, 5)
        .skip(2)
        .skip(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(5);
    }

    @Test
    public void crash() throws Throwable {
        Streamable.error(new TestException())
        .skip(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void zeroSkip() throws Throwable {
        Streamable.range(1, 5)
        .skip(0)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void empty() throws Throwable {
        Streamable.empty()
        .skip(2)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void skipAll() throws Throwable {
        Streamable.range(1, 5)
        .skip(5)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void skipMore() throws Throwable {
        Streamable.range(1, 5)
        .skip(6)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }
}
