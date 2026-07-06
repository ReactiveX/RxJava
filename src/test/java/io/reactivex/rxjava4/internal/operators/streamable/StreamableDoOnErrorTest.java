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

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;

public class StreamableDoOnErrorTest extends StreamableBaseTest {

    @Test
    public void normal() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        Streamable.range(1, 5)
        .doOnError(error::set)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1, 2, 3, 4, 5)
        ;

        assertNull(error.get(), "error is not empty?");
    }

    @Test
    public void hasError() {
        AtomicReference<Throwable> error = new AtomicReference<>();
        var te = new TestException();
        Streamable.error(te)
        .doOnError(error::set)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class)
        ;

        assertSame(te, error.get(), "doOnError differs from TestSubscriber.onError?");
    }

    @Test
    public void consumerCrash() {
        var te = new TestException();
        Streamable.error(te)
        .doOnError(_ -> { throw new IOException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class)
        .assertError(e -> e.getSuppressed()[0] == te)
        ;
    }
}
