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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Timer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func0;
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
        assertThatTheDelayIsValidForTheSwingTimer(delay);
        
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

    @Override
    public <T> Subscription schedulePeriodically(T state, final Func2<Scheduler, T, Subscription> action, long initialDelay, long period, TimeUnit unit) {
        final AtomicReference<Timer> timer = new AtomicReference<Timer>();
        
        final long delay = unit.toMillis(period); 
        assertThatTheDelayIsValidForTheSwingTimer(delay);
        
        final CompositeSubscription subscriptions = new CompositeSubscription();
        final Func2<Scheduler, T, Subscription> initialAction = new Func2<Scheduler, T, Subscription>() {
              @Override
              public Subscription call(final Scheduler scheduler, final T state0) {
                  // start timer for periodic execution, collect subscriptions
                  timer.set(new Timer((int) delay, new ActionListener() {
                      @Override
                      public void actionPerformed(ActionEvent e) {
                          subscriptions.add(action.call(scheduler,  state0));
                      }
                  }));
                  timer.get().start();
                  
                  return action.call(scheduler, state0);
              }
        };
        subscriptions.add(schedule(state, initialAction, initialDelay, unit));
        
        subscriptions.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                // in addition to all the individual unsubscriptions, stop the timer on unsubscribing
                Timer maybeTimer = timer.get();
                if (maybeTimer != null) {
                    maybeTimer.stop();
                }
            }
        }));
        
        return subscriptions;
    }

    private static void assertThatTheDelayIsValidForTheSwingTimer(long delay) {
        if (delay < 0 || delay > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(String.format("The swing timer only accepts non-negative delays up to %d milliseconds.", Integer.MAX_VALUE));
        }
    }
    
    public static class UnitTest {
        @Rule
        public ExpectedException exception = ExpectedException.none();
        
        @Test
        public void testInvalidDelayValues() {
            final SwingScheduler scheduler = new SwingScheduler();
            final Action0 action = mock(Action0.class);

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

            final Action0 innerAction = mock(Action0.class);
            final Action0 unsubscribe = mock(Action0.class);
            final Func0<Subscription> action = new Func0<Subscription>() {
                @Override
                public Subscription call() {
                    innerAction.call();
                    return Subscriptions.create(unsubscribe);
                }
            };
            
            Subscription sub = scheduler.schedulePeriodically(action, 20, 100, TimeUnit.MILLISECONDS);
            Thread.sleep(400);
            sub.unsubscribe();
            waitForEmptyEventQueue();
            verify(innerAction, times(4)).call();
            verify(unsubscribe, times(4)).call();
        }
        
        @Test
        public void testNestedActions() throws Exception {
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
}
