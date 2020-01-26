/**
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

package io.reactivex.rxjava3.internal.operators.maybe;

import static org.junit.Assert.*;

import java.util.concurrent.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.MaybeSubject;

public class MaybeToFutureTest extends RxJavaTest {

    @Test
    public void success() throws Exception {
        assertEquals((Integer)1, Maybe.just(1)
        .subscribeOn(Schedulers.computation())
        .toFuture()
        .get());
    }

    @Test
    public void empty() throws Exception {
        assertNull(Maybe.empty()
        .subscribeOn(Schedulers.computation())
        .toFuture()
        .get());
    }

    @Test
    public void error() throws InterruptedException {
        try {
            Maybe.error(new TestException())
            .subscribeOn(Schedulers.computation())
            .toFuture()
            .get();

            fail("Should have thrown!");
        } catch (ExecutionException ex) {
            assertTrue("" + ex.getCause(), ex.getCause() instanceof TestException);
        }
    }

    @Test
    public void cancel() {
        MaybeSubject<Integer> ms = MaybeSubject.create();

        Future<Integer> f = ms.toFuture();

        assertTrue(ms.hasObservers());

        f.cancel(true);

        assertFalse(ms.hasObservers());
    }

    @Test
    public void cancel2() {
        MaybeSubject<Integer> ms = MaybeSubject.create();

        Future<Integer> f = ms.toFuture();

        assertTrue(ms.hasObservers());

        f.cancel(false);

        assertFalse(ms.hasObservers());
    }
}
