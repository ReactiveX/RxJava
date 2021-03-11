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

package io.reactivex.rxjava3.flowable;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;

public class FlowableDoOnTest extends RxJavaTest {

    @Test
    public void doOnEach() {
        final AtomicReference<String> r = new AtomicReference<>();
        String output = Flowable.just("one").doOnNext(r::set).blockingSingle();

        assertEquals("one", output);
        assertEquals("one", r.get());
    }

    @Test
    public void doOnError() {
        final AtomicReference<Throwable> r = new AtomicReference<>();
        Throwable t = null;
        try {
            Flowable.<String> error(new RuntimeException("an error"))
            .doOnError(r::set).blockingSingle();
            fail("expected exception, not a return value");
        } catch (Throwable e) {
            t = e;
        }

        assertNotNull(t);
        assertEquals(t, r.get());
    }

    @Test
    public void doOnCompleted() {
        final AtomicBoolean r = new AtomicBoolean();
        String output = Flowable.just("one").doOnComplete(() -> r.set(true)).blockingSingle();

        assertEquals("one", output);
        assertTrue(r.get());
    }

    @Test
    public void doOnTerminateError() {
        final AtomicBoolean r = new AtomicBoolean();
        Flowable.<String>error(new TestException()).doOnTerminate(() -> r.set(true))
        .test()
        .assertFailure(TestException.class);
        assertTrue(r.get());
    }

    @Test
    public void doOnTerminateComplete() {
        final AtomicBoolean r = new AtomicBoolean();
        String output = Flowable.just("one").doOnTerminate(() -> r.set(true)).blockingSingle();

        assertEquals("one", output);
        assertTrue(r.get());

    }
}
