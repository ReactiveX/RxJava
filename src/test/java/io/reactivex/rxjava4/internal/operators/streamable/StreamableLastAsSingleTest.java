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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.internal.fuseable.HasUpstreamStreamableSource;
import io.reactivex.rxjava4.processors.DispatchStreamProcessor;
import io.reactivex.rxjava4.schedulers.Schedulers;

public class StreamableLastAsSingleTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.range(1, 5)
        .lastOrError()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(5);
    }

    @Test
    public void emptyDefault() throws Throwable {
        Streamable.empty()
        .last(6)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(6);
    }

    @Test
    public void error() throws Throwable {
        Streamable.error(new TestException())
        .lastOrError()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyNoDefault() throws Throwable {
        Streamable.empty()
        .lastOrError()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NoSuchElementException.class);
    }

    @Test
    public void nonEmptyDefault() throws Throwable {
        Streamable.range(1, 5)
        .last(7)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(5);
    }

    @Test
    public void finallyCrash() throws Throwable {
        StreamableFailingFinish.MAIN_COMPLETES
        .last(7)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void cancelled() throws Throwable {
        var dsp = new DispatchStreamProcessor<>();

        var to = dsp.lastOrError()
        .test();

        to.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000);

        to.dispose();

        awaitNoStreamers(dsp, 1000);
    }

    @Test
    public void hasSource() {
        var source = Streamable.range(1, 5);

        var operator = source.lastOrError();

        assertTrue(operator instanceof HasUpstreamStreamableSource<?> huss && source == huss.source(),
                "HasUpstreamStreamableSource not supported or source() returns something unexpected: " + operator);
    }

    @Test
    public void intervalRange() throws Throwable {
        Streamable.intervalRange(1, 5, 1, 1, TimeUnit.MILLISECONDS, Schedulers.single())
        .lastOrError()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(5L);
    }
}
