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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;

import javax.swing.Timer;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Executes work on the Swing UI thread.
 * This scheduler should only be used with actions that execute quickly.
 */
public final class SwingScheduler extends Scheduler {
    private static final SwingScheduler INSTANCE = new SwingScheduler();

    public static SwingScheduler getInstance() {
        return INSTANCE;
    }

    /* package for unit test */SwingScheduler() {
    }

    @Override
    public Worker createWorker() {
        return new InnerSwingScheduler();
    }

    private static class InnerSwingScheduler extends Worker {

        private final CompositeSubscription innerSubscription = new CompositeSubscription();

        @Override
        public void unsubscribe() {
            innerSubscription.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return innerSubscription.isUnsubscribed();
        }

        @Override
        public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
            long delay = Math.max(0, unit.toMillis(delayTime));
            assertThatTheDelayIsValidForTheSwingTimer(delay);
            final BooleanSubscription s = BooleanSubscription.create();
            class ExecuteOnceAction implements ActionListener {
                private Timer timer;

                private void setTimer(Timer timer) {
                    this.timer = timer;
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    timer.stop();
                    if (innerSubscription.isUnsubscribed() || s.isUnsubscribed()) {
                        return;
                    }
                    action.call();
                    innerSubscription.remove(s);
                }
            }

            ExecuteOnceAction executeOnce = new ExecuteOnceAction();
            final Timer timer = new Timer((int) delay, executeOnce);
            executeOnce.setTimer(timer);
            timer.start();

            innerSubscription.add(s);

            // wrap for returning so it also removes it from the 'innerSubscription'
            return Subscriptions.create(new Action0() {

                @Override
                public void call() {
                    timer.stop();
                    s.unsubscribe();
                    innerSubscription.remove(s);
                }

            });
        }

        @Override
        public Subscription schedule(final Action0 action) {
            final BooleanSubscription s = BooleanSubscription.create();
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (innerSubscription.isUnsubscribed() || s.isUnsubscribed()) {
                        return;
                    }
                    action.call();
                    innerSubscription.remove(s);
                }
            });

            innerSubscription.add(s);
            // wrap for returning so it also removes it from the 'innerSubscription'
            return Subscriptions.create(new Action0() {

                @Override
                public void call() {
                    s.unsubscribe();
                    innerSubscription.remove(s);
                }

            });
        }

    }

    private static void assertThatTheDelayIsValidForTheSwingTimer(long delay) {
        if (delay < 0 || delay > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(String.format("The swing timer only accepts non-negative delays up to %d milliseconds.", Integer.MAX_VALUE));
        }
    }
}
