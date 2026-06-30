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

import java.util.*;
import java.util.concurrent.*;

import org.junit.jupiter.api.*;

import io.reactivex.rxjava4.exceptions.*;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.testsupport.*;

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

    /**
     * Execute a test body with the help of a virtual thread executor service.
     * <p>
     * Don't forget to {@link ExecutorService#submit(Callable)} your work!
     * @param call the callback to give the VTE.
     * @throws Throwable propagate exceptions
     */
    public static void withVirtual(Consumer<ExecutorService> call) throws Throwable {
        try (var exec = new ExecutorIntercept(Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory()), false)) {
            call.accept(exec);
        }
    }

    /**
     * Execute a call within a virtual thread of the standard virtual thread executor.
     * @param call the call to invoke
     * @throws Throwable the exception propagated out
     */
    public static void onVirtual(Consumer<ExecutorService> call) throws Throwable {
        withVirtual(exec -> exec.submit(() -> {
            try {
                call.accept(exec);
            } catch (Throwable ex) {
                throw Exceptions.propagate(ex);
            }
        }).get());
    }

    record ExecutorIntercept(ExecutorService service, boolean printStackTrace) implements ExecutorService {

        @Override
        public void execute(Runnable command) {
            service.execute(command);
        }

        @Override
        public void shutdown() {
            if (printStackTrace) {
                new CancellationException("ExecutorIntercept::shutdown").printStackTrace();
            }
            service.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            if (printStackTrace) {
                new CancellationException("ExecutorIntercept::shutdownNow").printStackTrace();
            }
            return service.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return service.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return service.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return service.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return service.submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return service.submit(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return service.submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return service.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            return service.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            return service.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return service.invokeAny(tasks, timeout, unit);
        }
    }

    /**
     * Execute a test body with the help of a single thread executor service.
     * <p>
     * Don't forget to {@link ExecutorService#submit(Callable)} your work!
     * @param call the callback to give the VTE.
     * @throws Throwable propagate exceptions
     */
    public static void withSingleExecutor(Consumer<ScheduledExecutorService> call) throws Throwable {
        try (var exec = Executors.newSingleThreadScheduledExecutor()) {
            call.accept(exec);
        }
    }

    /**
     * Execute a test body with the help of a cached executor service.
     * <p>
     * Don't forget to {@link ExecutorService#submit(Callable)} your work!
     * @param call the callback to give the VTE.
     * @throws Throwable propagate exceptions
     */
    public static void withCachedExecutor(Consumer<ExecutorService> call) throws Throwable {
        try (var exec = new ExecutorIntercept(Executors.newCachedThreadPool(), false)) {
            call.accept(exec);
        }
    }

    /**
     * Enable thracking of the global errors for the duration of the action.
     * @param action the action to run with a list of errors encountered
     * @throws Throwable the exception rethrown from the action
     */
    public static void withErrorTracking(Consumer<List<Throwable>> action) throws Throwable {
        List<Throwable> errors = trackPluginErrors();
        try {
            action.accept(errors);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    public static List<Throwable> trackPluginErrors() {
        return TestHelper.trackPluginErrors();
    }
}
