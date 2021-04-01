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

package io.reactivex.rxjava3.internal.schedulers;

import static org.junit.Assert.*;

import java.util.concurrent.FutureTask;

import org.junit.Test;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class AbstractDirectTaskTest extends RxJavaTest {

    @Test
    public void cancelSetFuture() {
        AbstractDirectTask task = new AbstractDirectTask(Functions.EMPTY_RUNNABLE, true) {
            private static final long serialVersionUID = 208585707945686116L;
        };
        final Boolean[] interrupted = { null };

        assertFalse(task.isDisposed());

        task.dispose();

        assertTrue(task.isDisposed());

        task.dispose();

        assertTrue(task.isDisposed());

        FutureTask<Void> ft = new FutureTask<Void>(Functions.EMPTY_RUNNABLE, null) {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                interrupted[0] = mayInterruptIfRunning;
                return super.cancel(mayInterruptIfRunning);
            }
        };
        task.setFuture(ft);

        assertTrue(interrupted[0]);

        assertTrue(task.isDisposed());
    }

    @Test
    public void cancelSetFutureCurrentThread() {
        AbstractDirectTask task = new AbstractDirectTask(Functions.EMPTY_RUNNABLE, true) {
            private static final long serialVersionUID = 208585707945686116L;
        };
        final Boolean[] interrupted = { null };

        assertFalse(task.isDisposed());

        task.runner = Thread.currentThread();

        task.dispose();

        assertTrue(task.isDisposed());

        task.dispose();

        assertTrue(task.isDisposed());

        FutureTask<Void> ft = new FutureTask<Void>(Functions.EMPTY_RUNNABLE, null) {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                interrupted[0] = mayInterruptIfRunning;
                return super.cancel(mayInterruptIfRunning);
            }
        };
        task.setFuture(ft);

        assertFalse(interrupted[0]);

        assertTrue(task.isDisposed());
    }

    @Test
    public void setFutureCancel() {
        AbstractDirectTask task = new AbstractDirectTask(Functions.EMPTY_RUNNABLE, true) {
            private static final long serialVersionUID = 208585707945686116L;
        };
        final Boolean[] interrupted = { null };

        FutureTask<Void> ft = new FutureTask<Void>(Functions.EMPTY_RUNNABLE, null) {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                interrupted[0] = mayInterruptIfRunning;
                return super.cancel(mayInterruptIfRunning);
            }
        };

        assertFalse(task.isDisposed());

        task.setFuture(ft);

        assertFalse(task.isDisposed());

        task.dispose();

        assertTrue(task.isDisposed());

        assertTrue(interrupted[0]);
    }

    @Test
    public void setFutureCancelSameThread() {
        AbstractDirectTask task = new AbstractDirectTask(Functions.EMPTY_RUNNABLE, true) {
            private static final long serialVersionUID = 208585707945686116L;
        };
        final Boolean[] interrupted = { null };

        FutureTask<Void> ft = new FutureTask<Void>(Functions.EMPTY_RUNNABLE, null) {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                interrupted[0] = mayInterruptIfRunning;
                return super.cancel(mayInterruptIfRunning);
            }
        };
        assertFalse(task.isDisposed());

        task.setFuture(ft);

        task.runner = Thread.currentThread();

        assertFalse(task.isDisposed());

        task.dispose();

        assertTrue(task.isDisposed());

        assertFalse(interrupted[0]);
    }

    @Test
    public void finished() {
        AbstractDirectTask task = new AbstractDirectTask(Functions.EMPTY_RUNNABLE, true) {
            private static final long serialVersionUID = 208585707945686116L;
        };
        final Boolean[] interrupted = { null };
        FutureTask<Void> ft = new FutureTask<Void>(Functions.EMPTY_RUNNABLE, null) {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                interrupted[0] = mayInterruptIfRunning;
                return super.cancel(mayInterruptIfRunning);
            }
        };

        task.set(AbstractDirectTask.FINISHED);

        task.setFuture(ft);

        assertTrue(task.isDisposed());

        assertNull(interrupted[0]);

        task.dispose();

        assertTrue(task.isDisposed());

        assertNull(interrupted[0]);
    }

    @Test
    public void finishedCancel() {
        AbstractDirectTask task = new AbstractDirectTask(Functions.EMPTY_RUNNABLE, true) {
            private static final long serialVersionUID = 208585707945686116L;
        };
        final Boolean[] interrupted = { null };
        FutureTask<Void> ft = new FutureTask<Void>(Functions.EMPTY_RUNNABLE, null) {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                interrupted[0] = mayInterruptIfRunning;
                return super.cancel(mayInterruptIfRunning);
            }
        };

        task.set(AbstractDirectTask.FINISHED);

        assertTrue(task.isDisposed());

        task.dispose();

        assertTrue(task.isDisposed());

        task.setFuture(ft);

        assertTrue(task.isDisposed());

        assertNull(interrupted[0]);

        assertTrue(task.isDisposed());

        assertNull(interrupted[0]);
    }

    @Test
    public void disposeSetFutureRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final AbstractDirectTask task = new AbstractDirectTask(Functions.EMPTY_RUNNABLE, true) {
                private static final long serialVersionUID = 208585707945686116L;
            };

            final Boolean[] interrupted = { null };
            final FutureTask<Void> ft = new FutureTask<Void>(Functions.EMPTY_RUNNABLE, null) {
                @Override
                public boolean cancel(boolean mayInterruptIfRunning) {
                    interrupted[0] = mayInterruptIfRunning;
                    return super.cancel(mayInterruptIfRunning);
                }
            };

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    task.dispose();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    task.setFuture(ft);
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    static class TestDirectTask extends AbstractDirectTask {
        private static final long serialVersionUID = 587679821055711738L;

        TestDirectTask() {
            super(Functions.EMPTY_RUNNABLE, true);
        }
    }

    @Test
    public void toStringStates() {
        TestDirectTask task = new TestDirectTask();

        assertEquals("TestDirectTask[Waiting]", task.toString());

        task.runner = Thread.currentThread();

        assertEquals("TestDirectTask[Running on " + Thread.currentThread() + "]", task.toString());

        task.dispose();

        assertEquals("TestDirectTask[Disposed]", task.toString());

        task.set(AbstractDirectTask.FINISHED);

        assertEquals("TestDirectTask[Finished]", task.toString());
    }
}
