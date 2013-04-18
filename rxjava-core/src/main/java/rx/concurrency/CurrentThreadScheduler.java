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
package rx.concurrency;

import static org.mockito.Mockito.*;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.Func2;

/**
 * Schedules work on the current thread but does not execute immediately. Work is put in a queue and executed after the current unit of work is completed.
 */
public class CurrentThreadScheduler extends Scheduler {
    private static final CurrentThreadScheduler INSTANCE = new CurrentThreadScheduler();

    public static CurrentThreadScheduler getInstance() {
        return INSTANCE;
    }

    private static final ThreadLocal<Queue<DiscardableAction<?>>> QUEUE = new ThreadLocal<Queue<DiscardableAction<?>>>();

    private CurrentThreadScheduler() {
    }

    @Override
    public <T> Subscription schedule(T state, Func2<Scheduler, T, Subscription> action) {
        DiscardableAction<T> discardableAction = new DiscardableAction<T>(state, action);
        enqueue(discardableAction);
        return discardableAction;
    }

    @Override
    public <T> Subscription schedule(T state, Func2<Scheduler, T, Subscription> action, long dueTime, TimeUnit unit) {
        // since we are executing immediately on this thread we must cause this thread to sleep
        // TODO right now the 'enqueue' does not take delay into account so if another task is enqueued after this it will 
        // wait behind the sleeping action ... should that be the case or should it be allowed to proceed ahead of the delayed action?
        return schedule(state, new SleepingAction<T>(action, this, dueTime, unit));
    }

    private void enqueue(DiscardableAction<?> action) {
        Queue<DiscardableAction<?>> queue = QUEUE.get();
        boolean exec = queue == null;

        if (exec) {
            queue = new LinkedList<DiscardableAction<?>>();
            QUEUE.set(queue);
        }

        queue.add(action);

        if (exec) {
            while (!queue.isEmpty()) {
                queue.poll().call(this);
            }

            QUEUE.set(null);
        }
    }

    public static class UnitTest {

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

    }

}
