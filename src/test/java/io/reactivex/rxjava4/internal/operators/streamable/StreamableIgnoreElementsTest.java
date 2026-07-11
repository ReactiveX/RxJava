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

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.disposables.CompositeDisposable;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.processors.DispatchStreamProcessor;

public class StreamableIgnoreElementsTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        Streamable.range(1, 5)
        .ignoreElements()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void empty() throws Throwable {
        Streamable.empty()
        .ignoreElements()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void just() throws Throwable {
        Streamable.just(1)
        .ignoreElements()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void error() throws Throwable {
        Streamable.error(new TestException())
        .ignoreElements()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void finishCrash() throws Throwable {
        StreamableFailingFinish.MAIN_COMPLETES
        .ignoreElements()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void sourceAndFinishCrash() throws Throwable {
        StreamableFailingFinish.MAIN_FAILS
        .ignoreElements()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class)
        .assertError(e -> e.getSuppressed()[0] instanceof TestException)
        ;
    }

    @Test
    public void nextTwice() {
        var streamer = Streamable.empty()
        .ignoreElements()
        .stream(new CompositeDisposable());

        assertFalse(streamer.awaitNext(), "awaitNext-1");
        assertFalse(streamer.awaitNext(), "awaitNext-2");

        streamer.awaitFinish();
    }

    @Test
    public void dispatcher() throws Throwable {
        var dsp = new DispatchStreamProcessor<>();

        var ts = dsp.ignoreElements().test();

        ts.awaitOnSubscribe(1, TimeUnit.SECONDS);

        awaitStreamers(dsp, 1000);

        dsp.awaitNext(1);

        dsp.awaitNext(2);

        dsp.awaitFinish(null);

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }
}
