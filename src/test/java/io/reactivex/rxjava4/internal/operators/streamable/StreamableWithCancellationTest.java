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
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Streamable;
import io.reactivex.rxjava4.disposables.CompositeDisposable;
import io.reactivex.rxjava4.schedulers.Schedulers;

public class StreamableWithCancellationTest extends StreamableBaseTest {

    @Test
    public void normal() throws Throwable {
        var cd = new CompositeDisposable();

        var ts = Streamable.timer(1, TimeUnit.MINUTES, Schedulers.computation())
        .withCancellation(cd)
        .test()
        .awaitOnSubscribe(5, TimeUnit.SECONDS);

        cd.dispose();

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(CancellationException.class);
    }

    @Test
    public void crash() throws Throwable {
        Streamable.never()
        .withCancellation(_ -> { throw new IOException(); })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(IOException.class);
    }

    @Test
    public void nullReturn() throws Throwable {
        Streamable.never()
        .withCancellation(_ -> null)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(NullPointerException.class);
    }
}
