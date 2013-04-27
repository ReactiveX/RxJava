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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Timer;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func2;

/**
 * Executes work on the Swing UI thread. 
 * This scheduler should only be used with actions that execute quickly.
 */
public final class SwingScheduler extends Scheduler {
    private static final SwingScheduler INSTANCE = new SwingScheduler();

    public static SwingScheduler getInstance() {
        return INSTANCE;
    }

    private SwingScheduler() {
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action) {
        final AtomicReference<Subscription> sub = new AtomicReference<Subscription>();
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                sub.set(action.call(SwingScheduler.this, state));
            }
        });
        return Subscriptions.create(new Action0() {
            @Override
            public void call() {
                Subscription subscription = sub.get();
                if (subscription != null) {
                    subscription.unsubscribe();
                }
            }
        });
    }

    @Override
    public <T> Subscription schedule(final T state, final Func2<Scheduler, T, Subscription> action, long dueTime, TimeUnit unit) {
        final AtomicReference<Subscription> sub = new AtomicReference<Subscription>();
        long delay = unit.toMillis(dueTime); 
        
        if (delay > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(String.format(
                    "The swing timer only accepts delays up to %d milliseconds.", Integer.MAX_VALUE));
        }
        
        class ExecuteOnceAction implements ActionListener {
            private Timer timer;
            
            private void setTimer(Timer timer) {
                this.timer = timer;
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                timer.stop();
                sub.set(action.call(SwingScheduler.this, state));
            }
        }
        
        ExecuteOnceAction executeOnce = new ExecuteOnceAction();
        final Timer timer = new Timer((int) delay, executeOnce);
        executeOnce.setTimer(timer);
        timer.start();
        
        return Subscriptions.create(new Action0() {
            @Override
            public void call() {
                timer.stop();
                
                Subscription subscription = sub.get();
                if (subscription != null) {
                    subscription.unsubscribe();
                }
            }
        });
    }
    
    public static class UnitTest {
        @Test
        public void testNestedActions() throws InterruptedException, InvocationTargetException {
            final SwingScheduler scheduler = new SwingScheduler();

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
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    // nothing to do, we're just waiting here for the event queue to be emptied
                }
            });

            inOrder.verify(thirdStepStart, times(1)).call();
            inOrder.verify(thirdStepEnd, times(1)).call();
            inOrder.verify(secondStepStart, times(1)).call();
            inOrder.verify(secondStepEnd, times(1)).call();
            inOrder.verify(firstStepStart, times(1)).call();
            inOrder.verify(firstStepEnd, times(1)).call();
        }

    }
}
