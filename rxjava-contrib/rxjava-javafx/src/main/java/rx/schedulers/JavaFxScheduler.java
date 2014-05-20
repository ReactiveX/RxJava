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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.TimeUnit;

/**
 * Executes work on the JavaFx UI thread.
 * This scheduler should only be used with actions that execute quickly.
 */
public final class JavaFxScheduler extends Scheduler {
    private static final JavaFxScheduler INSTANCE = new JavaFxScheduler();

    /* package for unit test */JavaFxScheduler() {
    }

    public static JavaFxScheduler getInstance() {
        return INSTANCE;
    }

    private static void assertThatTheDelayIsValidForTheJavaFxTimer(long delay) {
        if (delay < 0 || delay > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(String.format("The JavaFx timer only accepts non-negative delays up to %d milliseconds.", Integer.MAX_VALUE));
        }
    }

    @Override
    public Worker createWorker() {
        return new InnerJavaFxScheduler();
    }

    private static class InnerJavaFxScheduler extends Worker {

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
            long delay = unit.toMillis(delayTime);
            assertThatTheDelayIsValidForTheJavaFxTimer(delay);
            final BooleanSubscription s = BooleanSubscription.create();

            final Timeline timeline = new Timeline(new KeyFrame(Duration.millis(delay), new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {
                    if (innerSubscription.isUnsubscribed() || s.isUnsubscribed()) {
                        return;
                    }
                    action.call();
                    innerSubscription.remove(s);
                }
            }));

            timeline.setCycleCount(1);
            timeline.play();

            innerSubscription.add(s);

            // wrap for returning so it also removes it from the 'innerSubscription'
            return Subscriptions.create(new Action0() {

                @Override
                public void call() {
                    timeline.stop();
                    s.unsubscribe();
                    innerSubscription.remove(s);
                }

            });
        }

        @Override
        public Subscription schedule(final Action0 action) {
            final BooleanSubscription s = BooleanSubscription.create();
            Platform.runLater(new Runnable() {
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
}
