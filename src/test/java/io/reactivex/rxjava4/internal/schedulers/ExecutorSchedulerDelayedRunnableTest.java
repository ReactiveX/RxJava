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

package io.reactivex.rxjava4.internal.schedulers;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.internal.schedulers.ExecutorScheduler.DelayedRunnable;
import io.reactivex.rxjava4.testsupport.SuppressUndeliverable;

public class ExecutorSchedulerDelayedRunnableTest extends RxJavaTest {

    @Test(expected = TestException.class)
    @SuppressUndeliverable
    public void delayedRunnableCrash() {
        @SuppressWarnings("resource")
        DelayedRunnable dl = new DelayedRunnable(() -> {
            throw new TestException();
        });
        dl.run();
    }

    @Test
    public void dispose() {
        final AtomicInteger count = new AtomicInteger();
        @SuppressWarnings("resource")
        DelayedRunnable dl = new DelayedRunnable(() -> count.incrementAndGet());

        dl.dispose();
        dl.dispose();

        dl.run();

        assertEquals(0, count.get());
    }
}
