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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.Scheduler;
import io.reactivex.rxjava4.core.Scheduler.Worker;
import io.reactivex.rxjava4.exceptions.Exceptions;

/**
 * Represents the state for a Scheduler -&gt; ExecutorService interface.
 * @param scheduler the scheduler to use
 * @param workerStore hosts the worker state
 * @since 4.0.0
 */
public record SchedulerToExecutorService(@NonNull Scheduler scheduler,
        @NonNull AtomicReference<Worker> workerStore) implements ExecutorService {

    @Override
    public void execute(Runnable command) {
        if (workerStore.get() instanceof Worker w) {
            w.schedule(command);
        } else {
            scheduler.scheduleDirect(command);
        }
    }

    @Override
    public void shutdown() {
        if (workerStore.get() instanceof Worker w) {
            w.dispose();
        } else {
            // FIXME, generally we don't want to shut down RxJava schedulers like this!
            // scheduler.shutdown();
            var w = workerStore.getAndSet(Scheduler.Worker.SHUTDOWN);
            if (w != null) {
                w.dispose();
            }
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        if (workerStore.get() instanceof Worker w) {
            w.dispose();
        } else {
            // FIXME, generally we don't want to shut down RxJava schedulers like this!
            // scheduler.shutdown();
            var w = workerStore.getAndSet(Scheduler.Worker.SHUTDOWN);
            if (w != null) {
                w.dispose();
            }
        }
        return List.of();
    }

    @Override
    public boolean isShutdown() {
        var w = workerStore.get();
        return w != null && w.isDisposed();
    }

    @Override
    public boolean isTerminated() {
        return isShutdown();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        // FIXME no idea how to passively wait, not really applicable in Rx
        long totalTime = unit.convert(timeout, TimeUnit.MILLISECONDS);

        while (!isTerminated() && totalTime > 0) {
            totalTime--;
            Thread.sleep(1);
        }
        return totalTime > 0;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                throw Exceptions.propagate(ex);
            }
        }, this::execute);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                task.run();
                return result;
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                throw Exceptions.propagate(ex);
            }
        }, this::execute);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return CompletableFuture.runAsync(() -> {
            try {
                task.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                throw Exceptions.propagate(ex);
            }
        }, this::execute);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        var result = new ArrayList<Future<T>>();
        for (var task : tasks) {
            result.add(submit(task));
        }
        for (var f : result) {
            try {
                f.get();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
            }
        }
        return result;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        var result = new ArrayList<Future<T>>();
        for (var task : tasks) {
            result.add(submit(task));
        }

        // FIXME how to wait in aggregate without spinning???
        long totalTime = unit.convert(timeout, TimeUnit.MILLISECONDS);

        while (!isTerminated() && totalTime > 0) {
            totalTime--;

            int done = 0;
            for (var f : result) {
                if (f.isDone()) {
                    done++;
                }
            }

            if (done == result.size()) {
                break;
            }

            Thread.sleep(1);
        }

        return result;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("The tasks parameter should contain at least one callable!");
        }

        var result = new ArrayList<Future<T>>();
        for (var task : tasks) {
            result.add(submit(task));
        }

        while (!isTerminated()) {
            for (var f : result) {
                if (f.state() == Future.State.SUCCESS) {

                    var v = f.resultNow();

                    for (var g : result) {
                        g.cancel(true);
                    }

                    return v;
                }
            }
            Thread.sleep(1);
        }
        return null; // Practically unreachable
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("The tasks parameter should contain at least one callable!");
        }

        var result = new ArrayList<Future<T>>();
        for (var task : tasks) {
            result.add(submit(task));
        }

        // FIXME how to wait in aggregate without spinning???
        long totalTime = unit.convert(timeout, TimeUnit.MILLISECONDS);

        while (!isTerminated() && totalTime > 0) {
            totalTime--;

            for (var f : result) {
                if (f.state() == Future.State.SUCCESS) {

                    var v = f.resultNow();

                    for (var g : result) {
                        g.cancel(true);
                    }

                    return v;
                }
            }
        }
        throw new TimeoutException("None of the tasks produced a clean result in the time allotted.");
    }

}
