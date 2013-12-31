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

import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Scheduler;
import rx.util.functions.Action0;

public class CurrentThreadSchedulerTest extends AbstractSchedulerTests {

    @Override
    protected Scheduler getScheduler() {
        return CurrentThreadScheduler.getInstance();
    }

    @Test
    public void testNestedActions() {
        final CurrentThreadScheduler scheduler = new CurrentThreadScheduler();

        final Action0 firstStepStart = mock(Action0.class);
        final Action0 firstStepEnd = mock(Action0.class);

        final Action0 secondStepStart = mock(Action0.class);
        final Action0 secondStepEnd = mock(Action0.class);

        final Action0 thirdStepStart = mock(Action0.class);
        final Action0 thirdStepEnd = mock(Action0.class);

        final Action0 firstAction = new Action0() {
            @Override
            public void call() {
                firstStepStart.call();
                firstStepEnd.call();
            }
        };
        final Action0 secondAction = new Action0() {
            @Override
            public void call() {
                secondStepStart.call();
                scheduler.schedule(firstAction);
                secondStepEnd.call();

            }
        };
        final Action0 thirdAction = new Action0() {
            @Override
            public void call() {
                thirdStepStart.call();
                scheduler.schedule(secondAction);
                thirdStepEnd.call();
            }
        };

        InOrder inOrder = inOrder(firstStepStart, firstStepEnd, secondStepStart, secondStepEnd, thirdStepStart, thirdStepEnd);

        scheduler.schedule(thirdAction);

        inOrder.verify(thirdStepStart, times(1)).call();
        inOrder.verify(thirdStepEnd, times(1)).call();
        inOrder.verify(secondStepStart, times(1)).call();
        inOrder.verify(secondStepEnd, times(1)).call();
        inOrder.verify(firstStepStart, times(1)).call();
        inOrder.verify(firstStepEnd, times(1)).call();
    }

    @Test
    public void testSequenceOfActions() {
        final CurrentThreadScheduler scheduler = new CurrentThreadScheduler();

        final Action0 first = mock(Action0.class);
        final Action0 second = mock(Action0.class);

        scheduler.schedule(first);
        scheduler.schedule(second);

        verify(first, times(1)).call();
        verify(second, times(1)).call();

    }

    @Test
    public void testSequenceOfDelayedActions() {
        final CurrentThreadScheduler scheduler = new CurrentThreadScheduler();

        final Action0 first = mock(Action0.class);
        final Action0 second = mock(Action0.class);

        scheduler.schedule(new Action0() {
            @Override
            public void call() {
                scheduler.schedule(first, 30, TimeUnit.MILLISECONDS);
                scheduler.schedule(second, 10, TimeUnit.MILLISECONDS);
            }
        });

        InOrder inOrder = inOrder(first, second);

        inOrder.verify(second, times(1)).call();
        inOrder.verify(first, times(1)).call();

    }

    @Test
    public void testMixOfDelayedAndNonDelayedActions() {
        final CurrentThreadScheduler scheduler = new CurrentThreadScheduler();

        final Action0 first = mock(Action0.class);
        final Action0 second = mock(Action0.class);
        final Action0 third = mock(Action0.class);
        final Action0 fourth = mock(Action0.class);

        scheduler.schedule(new Action0() {
            @Override
            public void call() {
                scheduler.schedule(first);
                scheduler.schedule(second, 300, TimeUnit.MILLISECONDS);
                scheduler.schedule(third, 100, TimeUnit.MILLISECONDS);
                scheduler.schedule(fourth);
            }
        });

        InOrder inOrder = inOrder(first, second, third, fourth);

        inOrder.verify(first, times(1)).call();
        inOrder.verify(fourth, times(1)).call();
        inOrder.verify(third, times(1)).call();
        inOrder.verify(second, times(1)).call();

    }
}
