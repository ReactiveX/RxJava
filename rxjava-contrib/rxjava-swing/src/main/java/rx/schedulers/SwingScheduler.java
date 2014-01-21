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

import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
    public <T> Subscription schedule(final T state, final Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
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
    public <T> Subscription schedule(final T state, final Func2<? super Scheduler, ? super T, ? extends Subscription> action, long dueTime, TimeUnit unit) {
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
    public <T> Subscription schedulePeriodically(T state, final Func2<? super Scheduler, ? super T, ? extends Subscription> action, long initialDelay, long period, TimeUnit unit) {
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
                        subscriptions.add(action.call(scheduler, state0));
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

}
