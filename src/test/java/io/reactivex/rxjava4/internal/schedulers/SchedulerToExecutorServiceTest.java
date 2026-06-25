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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.reactivex.rxjava4.core.Scheduler;
import io.reactivex.rxjava4.schedulers.Schedulers;

public class SchedulerToExecutorServiceTest {

    @Test
    public void invokeAnyShouldReturnResultOfCompletedTask() throws Exception {
        Scheduler scheduler = Schedulers.trampoline();
        @SuppressWarnings("resource")
        SchedulerToExecutorService executor = new SchedulerToExecutorService(
                scheduler, new AtomicReference<>(null));

        Callable<String> task1 = () -> "result1";
        Callable<String> task2 = () -> "result2";

        String result = executor.invokeAny(Arrays.asList(task1, task2));

        assertNotNull("invokeAny should return a result", result);
        assertTrue("result should be one of the task results",
                result.equals("result1") || result.equals("result2"));
    }

    @Test
    public void invokeAnyWithSingleTask() throws Exception {
        Scheduler scheduler = Schedulers.trampoline();
        @SuppressWarnings("resource")
        SchedulerToExecutorService executor = new SchedulerToExecutorService(
                scheduler, new AtomicReference<>(null));

        Callable<Integer> task = () -> 42;

        Integer result = executor.invokeAny(Arrays.asList(task));

        assertEquals("invokeAny should return the single task result", Integer.valueOf(42), result);
    }

    @Test
    public void invokeAnyWithEmptyTasksShouldThrow() throws Exception {
        Scheduler scheduler = Schedulers.trampoline();
        @SuppressWarnings("resource")
        SchedulerToExecutorService executor = new SchedulerToExecutorService(
                scheduler, new AtomicReference<>(null));

        try {
            executor.invokeAny(Arrays.asList());
            fail("invokeAny with empty tasks should throw IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void invokeAnyWithEmptyTasksShouldThrow2() throws Exception {
        Scheduler scheduler = Schedulers.trampoline();
        try (var executor = scheduler.toExecutorService(true)) {

            try {
                executor.invokeAny(Arrays.asList());
                fail("invokeAny with empty tasks should throw IllegalArgumentException");
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
    }
}
