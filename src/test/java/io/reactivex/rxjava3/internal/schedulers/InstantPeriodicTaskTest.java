/**
 * Copyright (c) 2016-present, RxJava Contributors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.rxjava3.internal.schedulers;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class InstantPeriodicTaskTest extends RxJavaTest {

    @Test
    public void taskCrash() throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {

            InstantPeriodicTask task = new InstantPeriodicTask(new Runnable() {
                @Override
                public void run() {
                    throw new TestException();
                }
            }, exec);

            assertNull(task.call());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            exec.shutdownNow();
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dispose() throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {

            InstantPeriodicTask task = new InstantPeriodicTask(new Runnable() {
                @Override
                public void run() {
                    throw new TestException();
                }
            }, exec);

            assertFalse(task.isDisposed());

            task.dispose();

            assertTrue(task.isDisposed());

            task.dispose();

            assertTrue(task.isDisposed());
        } finally {
            exec.shutdownNow();
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dispose2() throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {

            InstantPeriodicTask task = new InstantPeriodicTask(new Runnable() {
                @Override
                public void run() {
                    throw new TestException();
                }
            }, exec);

            task.setFirst(new FutureTask<Void>(Functions.EMPTY_RUNNABLE, null));
            task.setRest(new FutureTask<Void>(Functions.EMPTY_RUNNABLE, null));

            assertFalse(task.isDisposed());

            task.dispose();

            assertTrue(task.isDisposed());

            task.dispose();

            assertTrue(task.isDisposed());
        } finally {
            exec.shutdownNow();
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dispose2CurrentThread() throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {

            InstantPeriodicTask task = new InstantPeriodicTask(new Runnable() {
                @Override
                public void run() {
                    throw new TestException();
                }
            }, exec);

            task.runner = Thread.currentThread();

            task.setFirst(new FutureTask<Void>(Functions.EMPTY_RUNNABLE, null));
            task.setRest(new FutureTask<Void>(Functions.EMPTY_RUNNABLE, null));

            assertFalse(task.isDisposed());

            task.dispose();

            assertTrue(task.isDisposed());

            task.dispose();

            assertTrue(task.isDisposed());
        } finally {
            exec.shutdownNow();
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void dispose3() throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {

            InstantPeriodicTask task = new InstantPeriodicTask(new Runnable() {
                @Override
                public void run() {
                    throw new TestException();
                }
            }, exec);

            task.dispose();

            FutureTask<Void> f1 = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
            task.setFirst(f1);

            assertTrue(f1.isCancelled());

            FutureTask<Void> f2 = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
            task.setRest(f2);

            assertTrue(f2.isCancelled());
        } finally {
            exec.shutdownNow();
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void disposeOnCurrentThread() throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {

            InstantPeriodicTask task = new InstantPeriodicTask(new Runnable() {
                @Override
                public void run() {
                    throw new TestException();
                }
            }, exec);

            task.runner = Thread.currentThread();

            task.dispose();

            FutureTask<Void> f1 = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
            task.setFirst(f1);

            assertTrue(f1.isCancelled());

            FutureTask<Void> f2 = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
            task.setRest(f2);

            assertTrue(f2.isCancelled());
        } finally {
            exec.shutdownNow();
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void firstCancelRace() throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
                final InstantPeriodicTask task = new InstantPeriodicTask(new Runnable() {
                    @Override
                    public void run() {
                        throw new TestException();
                    }
                }, exec);

                final FutureTask<Void> f1 = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        task.setFirst(f1);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        task.dispose();
                    }
                };

                TestHelper.race(r1, r2);

                assertTrue(f1.isCancelled());
                assertTrue(task.isDisposed());
            }
        } finally {
            exec.shutdownNow();
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void restCancelRace() throws Exception {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            for (int i = 0; i < TestHelper.RACE_LONG_LOOPS; i++) {
                final InstantPeriodicTask task = new InstantPeriodicTask(new Runnable() {
                    @Override
                    public void run() {
                        throw new TestException();
                    }
                }, exec);

                final FutureTask<Void> f1 = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        task.setRest(f1);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        task.dispose();
                    }
                };

                TestHelper.race(r1, r2);

                assertTrue(f1.isCancelled());
                assertTrue(task.isDisposed());
            }
        } finally {
            exec.shutdownNow();
            RxJavaPlugins.reset();
        }
    }
}
