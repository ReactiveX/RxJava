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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.disposables.Disposable;
import io.reactivex.rxjava4.internal.util.ExceptionHelper;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.subjects.PublishSubject;
import io.reactivex.rxjava4.testsupport.TestHelper;

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
}
