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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;

public class StreamableOnErrorResumeNextTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.range(1, 5)
        .onErrorResumeNext(_ -> Streamable.range(6, 5))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void error() throws Throwable {
        Streamable.error(new TestException("1"))
        .onErrorResumeNext(t -> Streamable.range(Integer.parseInt(t.getMessage()), 5))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void errorDebug() throws Throwable {
        withCachedExecutor(exec -> {
            Streamable.error(new TestException("1"))
            .onErrorResumeNext(t -> Streamable.range(Integer.parseInt(t.getMessage()), 5))
            .test(exec)
            .awaitDone(500, TimeUnit.SECONDS)
            .assertResult(1, 2, 3, 4, 5);
        });
    }

    @Test
    public void errorToError() throws Throwable {
        Streamable.error(new TestException("1"))
        .onErrorResumeNext(t -> Streamable.error(new IOException(t.getMessage() + "2")))
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class)
        .assertError(e -> e.getMessage().equals("12"));
    }

    @Test
    public void errorNull() throws Throwable {
        Streamable.error(new TestException("1"))
        .onErrorResumeNext(_ -> null)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class)
        .assertError(t -> t.getSuppressed()[0] instanceof TestException);
    }

    @Test
    public void errorCrash() throws Throwable {
        Streamable.error(new TestException("1"))
        .onErrorResumeNext(_ -> { throw new IOException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class)
        .assertError(t -> t.getSuppressed()[0] instanceof TestException);
    }

    @Test
    public void finishFails() {
        StreamableFailingFinish.MAIN_FAILS
        .onErrorResumeNext(_ -> { throw new IOException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class)
        .assertError(e -> e.getSuppressed()[0] instanceof TestException);
    }

    @Test
    public void finishFailsDebug() throws Throwable {
        withCachedExecutor(exec -> {
            StreamableFailingFinish.MAIN_FAILS
            .onErrorResumeNext(_ -> { throw new IOException(); })
            .test(exec)
            .awaitDone(5, TimeUnit.MINUTES)
            .assertFailure(IOException.class)
            .assertError(e -> e.getSuppressed()[0] instanceof TestException);
        });
    }
}
