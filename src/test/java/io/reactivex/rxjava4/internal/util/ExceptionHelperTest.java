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

package io.reactivex.rxjava4.internal.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class ExceptionHelperTest extends RxJavaTest {
    @Test
    public void utilityClass() {
        TestHelper.checkUtilityClass(ExceptionHelper.class);
    }

    @Test
    public void addRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {

            final AtomicReference<Throwable> error = new AtomicReference<>();

            final TestException ex = new TestException();

            Runnable r = () -> assertTrue(ExceptionHelper.addThrowable(error, ex));

            TestHelper.race(r, r);
        }
    }

    @Test
    public void throwIfThrowable() throws Exception {
        assertThrows(InternalError.class, () -> {
            ExceptionHelper.<Exception>throwIfThrowable(new InternalError());
        });
    }

    @Test
    public void unwrapAndCombine1() {
        assertNull(ExceptionHelper.unwrapAndCombine(null, null));
    }

    @Test
    public void unwrapAndCombine2() {
        var te = new TestException();
        assertSame(te, ExceptionHelper.unwrapAndCombine(te, null));
    }

    @Test
    public void unwrapAndCombine3() {
        var te = new TestException();
        assertSame(te, ExceptionHelper.unwrapAndCombine(null, te));
    }

    @Test
    public void unwrapAndCombine4() {
        var te = new TestException();
        var te2 = new TestException();
        assertSame(te, ExceptionHelper.unwrapAndCombine(te, te2));
        assertSame(te2, te.getSuppressed()[0]);
    }

    @Test
    public void unwrapAndCombine5() {
        var te = new TestException();
        assertSame(te, ExceptionHelper.unwrapAndCombine(new CompletionException(te), null));
    }

    @Test
    public void unwrapAndCombine6() {
        var te = new TestException();
        assertSame(te, ExceptionHelper.unwrapAndCombine(null, new CompletionException(te)));
    }

    @Test
    public void unwrapAndCombine7() {
        var te = new TestException();
        assertSame(te, ExceptionHelper.unwrapAndCombine(new ThrowableWrapper(te), null));
    }

    @Test
    public void unwrapAndCombine8() {
        var te = new TestException();
        assertSame(te, ExceptionHelper.unwrapAndCombine(null, new ThrowableWrapper(te)));
    }
}
