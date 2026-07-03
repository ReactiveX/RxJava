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

public class StreamableTakeWhileTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        var verifier = new StreamableLifecycleVerifier<Integer>();

        Streamable.range(1, 5)
        .intercept(verifier.config())
        .takeWhile(v -> v < 4)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3);

        verifier.verify();
    }

    @Test
    public void crash() throws Throwable {
        var verifier = new StreamableLifecycleVerifier<Integer>();

        Streamable.<Integer>error(new TestException())
        .intercept(verifier.config())
        .takeWhile(v -> v < 4)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);

        verifier.verify();
    }

    @Test
    public void allPass() throws Throwable {
        var verifier = new StreamableLifecycleVerifier<Integer>();

        Streamable.range(1, 5)
        .intercept(verifier.config())
        .takeWhile(_ -> true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);

        verifier.verify();
    }

    @Test
    public void nonePass() throws Throwable {
        var verifier = new StreamableLifecycleVerifier<Integer>();

        Streamable.range(1, 5)
        .intercept(verifier.config())
        .takeWhile(_ -> false)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();

        verifier.verify();
    }

    @Test
    public void predicateCrash() throws Throwable {
        var verifier = new StreamableLifecycleVerifier<Integer>();

        Streamable.range(1, 5)
        .intercept(verifier.config())
        .takeWhile(_ -> { throw new TestException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);

        verifier.verify();
    }
}
