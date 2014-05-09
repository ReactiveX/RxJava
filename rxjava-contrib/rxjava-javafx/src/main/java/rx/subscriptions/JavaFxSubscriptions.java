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
package rx.subscriptions;


import javafx.application.Platform;
import rx.Scheduler.Worker;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.JavaFxScheduler;

public final class JavaFxSubscriptions {

    private JavaFxSubscriptions() {
        // no instance
    }

    /**
     * Create an Subscription that always runs <code>unsubscribe</code> in the event dispatch thread.
     *
     * @param unsubscribe the action to be performed in the ui thread at un-subscription
     * @return an Subscription that always runs <code>unsubscribe</code> in the event dispatch thread.
     */
    public static Subscription unsubscribeInEventDispatchThread(final Action0 unsubscribe) {
        return Subscriptions.create(new Action0() {
            @Override
            public void call() {
                if (Platform.isFxApplicationThread()) {
                    unsubscribe.call();
                } else {
                    final Worker inner = JavaFxScheduler.getInstance().createWorker();
                    inner.schedule(new Action0() {
                        @Override
                        public void call() {
                            unsubscribe.call();
                            inner.unsubscribe();
                        }
                    });
                }
            }
        });
    }
}
