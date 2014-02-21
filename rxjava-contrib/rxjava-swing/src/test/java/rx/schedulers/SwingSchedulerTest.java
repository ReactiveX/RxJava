/**
 * Copyright 2013 Netflix, Inc.
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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.awt.EventQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

import rx.Scheduler.Inner;
import rx.Subscription;
import rx.functions.Action1;

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
        final Action1<Inner> action = mock(Action1.class);

        exception.expect(IllegalArgumentException.class);
        scheduler.schedulePeriodically(action, -1L, 100L, TimeUnit.SECONDS);

        exception.expect(IllegalArgumentException.class);
        scheduler.schedulePeriodically(action, 100L, -1L, TimeUnit.SECONDS);

        exception.expect(IllegalArgumentException.class);
        scheduler.schedulePeriodically(action, 1L + Integer.MAX_VALUE, 100L, TimeUnit.MILLISECONDS);

        exception.expect(IllegalArgumentException.class);
        scheduler.schedulePeriodically(action, 100L, 1L + Integer.MAX_VALUE / 1000, TimeUnit.SECONDS);
    }

    @Test
    public void testPeriodicScheduling() throws Exception {
        final SwingScheduler scheduler = new SwingScheduler();

        final CountDownLatch latch = new CountDownLatch(4);

        final Action1<Inner> innerAction = mock(Action1.class);
        final Action1<Inner> action = new Action1<Inner>() {
            @Override
            public void call(Inner inner) {
                try {
                    innerAction.call(inner);
                    assertTrue(SwingUtilities.isEventDispatchThread());
                } finally {
                    latch.countDown();
                }
            }
        };

        Subscription sub = scheduler.schedulePeriodically(action, 50, 200, TimeUnit.MILLISECONDS);

        if (!latch.await(5000, TimeUnit.MILLISECONDS)) {
            fail("timed out waiting for tasks to execute");
        }

        sub.unsubscribe();
        waitForEmptyEventQueue();
        verify(innerAction, times(4)).call(any(Inner.class));
    }

    @Test
    public void testNestedActions() throws Exception {
        final SwingScheduler scheduler = new SwingScheduler();

        final Action1<Inner> firstStepStart = mock(Action1.class);
        final Action1<Inner> firstStepEnd = mock(Action1.class);

        final Action1<Inner> secondStepStart = mock(Action1.class);
        final Action1<Inner> secondStepEnd = mock(Action1.class);

        final Action1<Inner> thirdStepStart = mock(Action1.class);
        final Action1<Inner> thirdStepEnd = mock(Action1.class);

        final Action1<Inner> firstAction = new Action1<Inner>() {
            @Override
            public void call(Inner inner) {
                assertTrue(SwingUtilities.isEventDispatchThread());
                firstStepStart.call(inner);
                firstStepEnd.call(inner);
            }
        };
        final Action1<Inner> secondAction = new Action1<Inner>() {
            @Override
            public void call(Inner inner) {
                assertTrue(SwingUtilities.isEventDispatchThread());
                secondStepStart.call(inner);
                scheduler.schedule(firstAction);
                secondStepEnd.call(inner);
            }
        };
        final Action1<Inner> thirdAction = new Action1<Inner>() {
            @Override
            public void call(Inner inner) {
                assertTrue(SwingUtilities.isEventDispatchThread());
                thirdStepStart.call(inner);
                scheduler.schedule(secondAction);
                thirdStepEnd.call(inner);
            }
        };

        InOrder inOrder = inOrder(firstStepStart, firstStepEnd, secondStepStart, secondStepEnd, thirdStepStart, thirdStepEnd);

        scheduler.schedule(thirdAction);
        waitForEmptyEventQueue();

        inOrder.verify(thirdStepStart, times(1)).call(any(Inner.class));
        inOrder.verify(thirdStepEnd, times(1)).call(any(Inner.class));
        inOrder.verify(secondStepStart, times(1)).call(any(Inner.class));
        inOrder.verify(secondStepEnd, times(1)).call(any(Inner.class));
        inOrder.verify(firstStepStart, times(1)).call(any(Inner.class));
        inOrder.verify(firstStepEnd, times(1)).call(any(Inner.class));
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
