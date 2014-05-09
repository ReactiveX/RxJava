/**
 * Copyright 2014 Netflix, Inc.
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
package rx.schedulers;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import rx.Scheduler.Worker;
import rx.functions.Action0;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Executes work on the JavaFx UI thread.
 * This scheduler should only be used with actions that execute quickly.
 */
public final class JavaFxSchedulerTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private static void waitForEmptyEventQueue() throws Exception {
        FXUtilities.runAndWait(new Runnable() {
            @Override
            public void run() {
                // nothing to do, we're just waiting here for the event queue to be emptied
            }
        });
    }

    public static class AsNonApp extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            // noop
        }
    }

    @BeforeClass
    public static void initJFX() {
        Thread t = new Thread("JavaFX Init Thread") {
            public void run() {
                Application.launch(AsNonApp.class, new String[0]);
            }
        };
        t.setDaemon(true);
        t.start();
    }

    @Test
    public void testInvalidDelayValues() {
        final JavaFxScheduler scheduler = new JavaFxScheduler();
        final Worker inner = scheduler.createWorker();
        final Action0 action = mock(Action0.class);

        exception.expect(IllegalArgumentException.class);
        inner.schedulePeriodically(action, -1L, 100L, TimeUnit.SECONDS);

        exception.expect(IllegalArgumentException.class);
        inner.schedulePeriodically(action, 100L, -1L, TimeUnit.SECONDS);

        exception.expect(IllegalArgumentException.class);
        inner.schedulePeriodically(action, 1L + Integer.MAX_VALUE, 100L, TimeUnit.MILLISECONDS);

        exception.expect(IllegalArgumentException.class);
        inner.schedulePeriodically(action, 100L, 1L + Integer.MAX_VALUE / 1000, TimeUnit.SECONDS);
    }

    @Test
    public void testPeriodicScheduling() throws Exception {
        final JavaFxScheduler scheduler = new JavaFxScheduler();
        final Worker inner = scheduler.createWorker();

        final CountDownLatch latch = new CountDownLatch(4);

        final Action0 innerAction = mock(Action0.class);
        final Action0 action = new Action0() {
            @Override
            public void call() {
                try {
                    innerAction.call();
                    assertTrue(Platform.isFxApplicationThread());
                } finally {
                    latch.countDown();
                }
            }
        };

        inner.schedulePeriodically(action, 50, 200, TimeUnit.MILLISECONDS);

        if (!latch.await(5000, TimeUnit.MILLISECONDS)) {
            fail("timed out waiting for tasks to execute");
        }

        inner.unsubscribe();
        waitForEmptyEventQueue();
        verify(innerAction, times(4)).call();
    }

    @Test
    public void testNestedActions() throws Exception {
        final JavaFxScheduler scheduler = new JavaFxScheduler();
        final Worker inner = scheduler.createWorker();

        final Action0 firstStepStart = mock(Action0.class);
        final Action0 firstStepEnd = mock(Action0.class);

        final Action0 secondStepStart = mock(Action0.class);
        final Action0 secondStepEnd = mock(Action0.class);

        final Action0 thirdStepStart = mock(Action0.class);
        final Action0 thirdStepEnd = mock(Action0.class);

        final Action0 firstAction = new Action0() {
            @Override
            public void call() {
                assertTrue(Platform.isFxApplicationThread());
                firstStepStart.call();
                firstStepEnd.call();
            }
        };
        final Action0 secondAction = new Action0() {
            @Override
            public void call() {
                assertTrue(Platform.isFxApplicationThread());
                secondStepStart.call();
                inner.schedule(firstAction);
                secondStepEnd.call();
            }
        };
        final Action0 thirdAction = new Action0() {
            @Override
            public void call() {
                assertTrue(Platform.isFxApplicationThread());
                thirdStepStart.call();
                inner.schedule(secondAction);
                thirdStepEnd.call();
            }
        };

        InOrder inOrder = inOrder(firstStepStart, firstStepEnd, secondStepStart, secondStepEnd, thirdStepStart, thirdStepEnd);

        inner.schedule(thirdAction);
        waitForEmptyEventQueue();

        inOrder.verify(thirdStepStart, times(1)).call();
        inOrder.verify(thirdStepEnd, times(1)).call();
        inOrder.verify(secondStepStart, times(1)).call();
        inOrder.verify(secondStepEnd, times(1)).call();
        inOrder.verify(firstStepStart, times(1)).call();
        inOrder.verify(firstStepEnd, times(1)).call();
    }

    /*
     * based on http://www.guigarage.com/2013/01/invokeandwait-for-javafx/
     * by hendrikebbers
     */
    static public class FXUtilities {

        /**
         * Simple helper class.
         *
         * @author hendrikebbers
         */
        private static class ThrowableWrapper {
            Throwable t;
        }

        /**
         * Invokes a Runnable in JFX Thread and waits while it's finished. Like
         * SwingUtilities.invokeAndWait does for EDT.
         *
         * @param run The Runnable that has to be called on JFX thread.
         * @throws InterruptedException f the execution is interrupted.
         * @throws ExecutionException   If a exception is occurred in the run method of the Runnable
         */
        public static void runAndWait( final Runnable run) throws InterruptedException, ExecutionException {
            if (Platform.isFxApplicationThread()) {
                try {
                    run.run();
                } catch (Exception e) {
                    throw new ExecutionException(e);
                }
            } else {
                final Lock lock = new ReentrantLock();
                final Condition condition = lock.newCondition();
                final ThrowableWrapper throwableWrapper = new ThrowableWrapper();
                lock.lock();
                try {
                    Platform.runLater(new Runnable() {

                        @Override
                        public void run() {
                            lock.lock();
                            try {
                                run.run();
                            } catch (Throwable e) {
                                throwableWrapper.t = e;
                            } finally {
                                try {
                                    condition.signal();
                                } finally {
                                    lock.unlock();
                                }
                            }
                        }
                    });
                    condition.await();
                    if (throwableWrapper.t != null) {
                        throw new ExecutionException(throwableWrapper.t);
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
