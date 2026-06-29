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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

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
        assertTrue(result.equals("result1") || result.equals("result2"), "result should be one of the task results");
    }

    @Test
    public void invokeAnyWithSingleTask() throws Exception {
        Scheduler scheduler = Schedulers.trampoline();
        @SuppressWarnings("resource")
        SchedulerToExecutorService executor = new SchedulerToExecutorService(
                scheduler, new AtomicReference<>(null));

        Callable<Integer> task = () -> 42;

        Integer result = executor.invokeAny(Arrays.asList(task));

        assertEquals(Integer.valueOf(42), result, "invokeAny should return the single task result");
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
}
