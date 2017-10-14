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

package io.reactivex.disposables;

import java.util.concurrent.FutureTask;

import static org.junit.Assert.*;
import org.junit.Test;

import io.reactivex.internal.functions.Functions;

public class FutureDisposableTest {

    @Test
    public void normal() {
        FutureTask<Object> ft = new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null);
        Disposable d = Disposables.fromFuture(ft);
        assertFalse(d.isDisposed());

        d.dispose();

        assertTrue(d.isDisposed());

        d.dispose();

        assertTrue(d.isDisposed());

        assertTrue(ft.isCancelled());
    }

    @Test
    public void interruptible() {
        FutureTask<Object> ft = new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null);
        Disposable d = Disposables.fromFuture(ft, true);
        assertFalse(d.isDisposed());

        d.dispose();

        assertTrue(d.isDisposed());

        d.dispose();

        assertTrue(d.isDisposed());

        assertTrue(ft.isCancelled());
    }

    @Test
    public void normalDone() {
        FutureTask<Object> ft = new FutureTask<Object>(Functions.EMPTY_RUNNABLE, null);
        FutureDisposable d = new FutureDisposable(ft, false);
        assertFalse(d.isDisposed());

        assertFalse(d.isDisposed());

        ft.run();

        assertTrue(d.isDisposed());
    }
}
