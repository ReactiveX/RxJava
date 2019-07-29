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

package io.reactivex.internal.util;

import static org.junit.Assert.*;

import java.util.List;

import io.reactivex.RxJavaTest;
import org.junit.Test;

import io.reactivex.exceptions.TestException;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.testsupport.TestHelper;

public class AtomicThrowableTest extends RxJavaTest {

    @Test
    public void isTerminated() {
        AtomicThrowable ex = new AtomicThrowable();

        assertFalse(ex.isTerminated());

        assertNull(ex.terminate());

        assertTrue(ex.isTerminated());
    }

    @Test
    public void tryTerminateAndReportNull() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {

            AtomicThrowable ex = new AtomicThrowable();
            ex.tryTerminateAndReport();

            assertTrue("" + errors, errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void tryTerminateAndReportAlreadyTerminated() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {

            AtomicThrowable ex = new AtomicThrowable();
            ex.terminate();

            ex.tryTerminateAndReport();

            assertTrue("" + errors, errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void tryTerminateAndReportHasError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {

            AtomicThrowable ex = new AtomicThrowable();
            ex.set(new TestException());

            ex.tryTerminateAndReport();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);

            assertEquals(1, errors.size());
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
