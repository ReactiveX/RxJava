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

package io.reactivex.rxjava4.exceptions;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;

public class ThrowableWrapperTest extends RxJavaTest {

    @Test
    public void basic() {
        var original = new Throwable("original");

        try {
            throw new ThrowableWrapper(original);
        } catch (RuntimeException ex) {
            assertSame(original, ex.getCause());
            assertEquals("You forgot to unwrap me!", ex.getMessage());
            assertEquals("original", ex.getCause().getMessage());
        }
    }

    @Test
    public void basicNull() {
        try {
            throw new ThrowableWrapper(null);
        } catch (RuntimeException ex) {
            assertEquals("original is null", ex.getCause().getMessage());
            assertEquals("You forgot to unwrap me!", ex.getMessage());
            assertTrue(ex.getCause() instanceof NullPointerException, ex.getCause().toString());
        }
    }

    @Test
    public void virtualCreateUnwraps() {
        Flowable.virtualCreate(_ -> {
            throw new ThrowableWrapper(new TestException());
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }

    @Test
    public void virtualTransformUnwraps() {
        Flowable.just(1)
        .virtualTransform((_, _, _) -> {
            throw new ThrowableWrapper(new TestException());
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class);
    }
}
