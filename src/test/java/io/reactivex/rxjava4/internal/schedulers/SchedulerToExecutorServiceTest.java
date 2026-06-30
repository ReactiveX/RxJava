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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.core.Scheduler;
import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.schedulers.Schedulers;

public class SchedulerToExecutorServiceTest {

    @Test
    public void invokeAnyShouldReturnResultOfCompletedTask() throws Exception {
        Scheduler scheduler = Schedulers.computation();
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
        Scheduler scheduler = Schedulers.computation();
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
            executor.invokeAny(List.of());
            fail("invokeAny with empty tasks should throw IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void invokeAnyTimeoutWithEmptyTasksShouldThrow() throws Exception {
        Scheduler scheduler = Schedulers.trampoline();
        @SuppressWarnings("resource")
        SchedulerToExecutorService executor = new SchedulerToExecutorService(
                scheduler, new AtomicReference<>(null));

        try {
            executor.invokeAny(List.of(), 5, TimeUnit.SECONDS);
            fail("invokeAny with empty tasks should throw IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void invokeAnyAllThrow() {
        var ex = assertThrows(ExecutionException.class, () -> {
            Scheduler scheduler = Schedulers.computation();
            @SuppressWarnings("resource")
            SchedulerToExecutorService executor = new SchedulerToExecutorService(
                    scheduler, new AtomicReference<>(null));

            Callable<Object> run = () -> { throw new TestException(); };

            executor.invokeAny(List.of(run, run, run));
        });

        if (ex.getCause() instanceof CompositeException ce) {
            assertEquals(3, ce.getExceptions().size());
        } else {
            throw new AssertionError("Wrong contents", ex);
        }
    }

    @Test
    public void invokeAnyTimeoutAllSucceed() throws Exception {
        Scheduler scheduler = Schedulers.computation();
        @SuppressWarnings("resource")
        SchedulerToExecutorService executor = new SchedulerToExecutorService(
                scheduler, new AtomicReference<>(null));

        Callable<String> task1 = () -> "result1";
        Callable<String> task2 = () -> "result2";

        String result = executor.invokeAny(Arrays.asList(task1, task2), 5, TimeUnit.SECONDS);

        assertNotNull("invokeAny should return a result", result);
        assertTrue(result.equals("result1") || result.equals("result2"), "result should be one of the task results");
    }

    @Test
    public void invokeAnyTimeoutAllThrow() {
        var ex = assertThrows(ExecutionException.class, () -> {
            Scheduler scheduler = Schedulers.computation();
            @SuppressWarnings("resource")
            SchedulerToExecutorService executor = new SchedulerToExecutorService(
                    scheduler, new AtomicReference<>(null));

            Callable<Object> run = () -> { throw new TestException(); };

            executor.invokeAny(List.of(run, run, run), 5, TimeUnit.SECONDS);
        });

        if (ex.getCause() instanceof CompositeException ce) {
            assertEquals(3, ce.getExceptions().size());
        } else {
            throw new AssertionError("Wrong contents", ex);
        }
    }

    @Test
    public void invokeAnyTimeoutHappens() throws Exception {
        assertThrows(TimeoutException.class, () -> {
            Scheduler scheduler = Schedulers.computation();
            @SuppressWarnings("resource")
            SchedulerToExecutorService executor = new SchedulerToExecutorService(
                    scheduler, new AtomicReference<>(null));

            Callable<String> task1 = () -> { Thread.sleep(1000); return "result1"; };
            Callable<String> task2 = () -> { Thread.sleep(1000); return "result2"; };

            String result = executor.invokeAny(Arrays.asList(task1, task2), 100, TimeUnit.MILLISECONDS);

            assertNotNull("invokeAny should return a result", result);
            assertTrue(result.equals("result1") || result.equals("result2"), "result should be one of the task results");
        });
    }

    @Test
    public void getExceptionNone() throws InterruptedException {
        var cf = new CompletableFuture<Void>();
        cf.complete(null);
        assertNull(SchedulerToExecutorService.getException(cf));
    }

    @Test
    public void invokeAll() throws Throwable{
        Scheduler scheduler = Schedulers.computation();
        @SuppressWarnings("resource")
        SchedulerToExecutorService executor = new SchedulerToExecutorService(
                scheduler, new AtomicReference<>(null));

        Callable<String> task1 = () -> "result1";
        Callable<String> task2 = () -> "result2";

        var result = executor.invokeAll(List.of(task1, task2));

        assertEquals("result1", result.get(0).resultNow());
        assertEquals("result2", result.get(1).resultNow());
    }

    @Test
    public void invokeAllTimeout() throws Throwable{
        Scheduler scheduler = Schedulers.computation();
        @SuppressWarnings("resource")
        SchedulerToExecutorService executor = new SchedulerToExecutorService(
                scheduler, new AtomicReference<>(null));

        Callable<String> task1 = () -> "result1";
        Callable<String> task2 = () -> "result2";

        var result = executor.invokeAll(List.of(task1, task2), 5, TimeUnit.SECONDS);

        assertEquals("result1", result.get(0).resultNow());
        assertEquals("result2", result.get(1).resultNow());
    }

    @Test
    public void invokeAllTimeoutDoesTimeout() throws Throwable {
        Scheduler scheduler = Schedulers.computation();
        @SuppressWarnings("resource")
        SchedulerToExecutorService executor = new SchedulerToExecutorService(
                scheduler, new AtomicReference<>(null));

        Callable<String> task1 = () -> { Thread.sleep(1000); return "result1"; };
        Callable<String> task2 = () -> { Thread.sleep(1000); return "result2"; };

        var result = executor.invokeAll(List.of(task1, task2), 100, TimeUnit.MILLISECONDS);

        for (var f : result) {
            assertTrue(f.isCancelled(), "Task was not cancelled: " + f);
        }
    }
}
