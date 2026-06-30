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

package io.reactivex.rxjava4.core;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.*;

import io.reactivex.rxjava4.exceptions.UndeliverableException;
import io.reactivex.rxjava4.functions.Action;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.testsupport.SuppressUndeliverable;

@Timeout(value = 5, unit = TimeUnit.MINUTES)
public abstract class RxJavaTest {
    /**
     * Announce creates a log print preventing Travis CI from killing the build.
     */
    @Test
    @Disabled
    public final void announce() {
    }

    @SuppressWarnings("exports")
    @BeforeEach
    public void beforeEach(TestInfo info) {
        info.getTestMethod().ifPresent(description -> {
            if (description.getAnnotation(SuppressUndeliverable.class) != null) {
                RxJavaPlugins.setErrorHandler(throwable -> {
                    if (!(throwable instanceof UndeliverableException)) {
                        throwable.printStackTrace();
                        Thread currentThread = Thread.currentThread();
                        currentThread.getUncaughtExceptionHandler().uncaughtException(currentThread, throwable);
                    }
                });
            }
        });
    }

    @SuppressWarnings("exports")
    @AfterEach
    public void afterEach(TestInfo info) {
        RxJavaPlugins.setErrorHandler(null);
    }

    /**
     * Wrap your test body into this retry lambda-based callback to retry flaky tests
     * that usually depend on Thread.sleep consistency.
     * @param count the number of times to retry
     * @param code the code to run
     */
    public static void withRetry(int count, Action code) {
        AssertionError error = null;
        while (count-- > 0) {
            try {
                code.run();
                return;
            } catch (Throwable ex) {
                if (error == null) {
                    error = new AssertionError("withRetry failures");
                }
                error.addSuppressed(ex);
            }
        }
        if (error != null) {
            throw error;
        }
    }
}
