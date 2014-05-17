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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.awt.EventQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

import rx.Scheduler.Worker;
import rx.functions.Action0;

/**
 * Executes work on the Swing UI thread.
 * This scheduler should only be used with actions that execute quickly.
 */
public final class SwingSchedulerTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testInvalidDelayValues() {
        final SwingScheduler scheduler = new SwingScheduler();
        final Worker inner = scheduler.createWorker();
        final Action0 action = mock(Action0.class);

        inner.schedulePeriodically(action, -1L, 100L, TimeUnit.SECONDS);

        inner.schedulePeriodically(action, 100L, -1L, TimeUnit.SECONDS);

        exception.expect(IllegalArgumentException.class);
        inner.schedulePeriodically(action, 1L + Integer.MAX_VALUE, 100L, TimeUnit.MILLISECONDS);

        exception.expect(IllegalArgumentException.class);
        inner.schedulePeriodically(action, 100L, 1L + Integer.MAX_VALUE / 1000, TimeUnit.SECONDS);
    }

    @Test
    public void testPeriodicScheduling() throws Exception {
        final SwingScheduler scheduler = new SwingScheduler();
        final Worker inner = scheduler.createWorker();

        final CountDownLatch latch = new CountDownLatch(4);

        final Action0 innerAction = mock(Action0.class);
        final Action0 action = new Action0() {
            @Override
            public void call() {
                try {
                    innerAction.call();
                    assertTrue(SwingUtilities.isEventDispatchThread());
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
        final SwingScheduler scheduler = new SwingScheduler();
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
                assertTrue(SwingUtilities.isEventDispatchThread());
                firstStepStart.call();
                firstStepEnd.call();
            }
        };
        final Action0 secondAction = new Action0() {
            @Override
            public void call() {
                assertTrue(SwingUtilities.isEventDispatchThread());
                secondStepStart.call();
                inner.schedule(firstAction);
                secondStepEnd.call();
            }
        };
        final Action0 thirdAction = new Action0() {
            @Override
            public void call() {
                assertTrue(SwingUtilities.isEventDispatchThread());
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

    private static void waitForEmptyEventQueue() throws Exception {
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                // nothing to do, we're just waiting here for the event queue to be emptied
            }
        });
    }

}
