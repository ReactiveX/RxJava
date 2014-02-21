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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Timer;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
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
    public Subscription schedule(Action1<Inner> action) {
        InnerSwingScheduler inner = new InnerSwingScheduler();
        inner.schedule(action);
        return inner;
    }

    @Override
    public Subscription schedule(Action1<Inner> action, long delayTime, TimeUnit unit) {
        long delay = unit.toMillis(delayTime);
        assertThatTheDelayIsValidForTheSwingTimer(delay);
        InnerSwingScheduler inner = new InnerSwingScheduler();
        inner.schedule(action, delayTime, unit);
        return inner;
    }

    private static class InnerSwingScheduler extends Inner {

        private final Inner _inner = this;
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
        public void schedule(final Action1<Inner> action, long delayTime, TimeUnit unit) {
            final AtomicReference<Subscription> sub = new AtomicReference<Subscription>();
            long delay = unit.toMillis(delayTime);
            assertThatTheDelayIsValidForTheSwingTimer(delay);

            final AtomicReference<Subscription> sf = new AtomicReference<Subscription>();
            class ExecuteOnceAction implements ActionListener {
                private Timer timer;

                private void setTimer(Timer timer) {
                    this.timer = timer;
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    timer.stop();
                    if (innerSubscription.isUnsubscribed()) {
                        return;
                    }
                    action.call(_inner);
                    Subscription s = sf.get();
                    if (s != null) {
                        innerSubscription.remove(s);
                    }
                }
            }

            ExecuteOnceAction executeOnce = new ExecuteOnceAction();
            final Timer timer = new Timer((int) delay, executeOnce);
            executeOnce.setTimer(timer);
            timer.start();

            Subscription s = Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    timer.stop();

                    Subscription subscription = sub.get();
                    if (subscription != null) {
                        subscription.unsubscribe();
                    }
                }
            });

            sf.set(s);
            innerSubscription.add(s);
        }

        @Override
        public void schedule(final Action1<Inner> action) {
            final AtomicReference<Subscription> sub = new AtomicReference<Subscription>();

            final AtomicReference<Subscription> sf = new AtomicReference<Subscription>();
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (innerSubscription.isUnsubscribed()) {
                        return;
                    }
                    action.call(_inner);
                    Subscription s = sf.get();
                    if (s != null) {
                        innerSubscription.remove(s);
                    }
                }
            });

            Subscription s = Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    Subscription subscription = sub.get();
                    if (subscription != null) {
                        subscription.unsubscribe();
                    }
                }
            });

            sf.set(s);
            innerSubscription.add(s);
        }

    }

    private static void assertThatTheDelayIsValidForTheSwingTimer(long delay) {
        if (delay < 0 || delay > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(String.format("The swing timer only accepts non-negative delays up to %d milliseconds.", Integer.MAX_VALUE));
        }
    }
}
