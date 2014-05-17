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
package rx.swing.sources;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rx.Scheduler.Worker;
import rx.functions.Action0;
import rx.schedulers.SwingScheduler;

/* package-private */final class SwingTestHelper { // only for test

    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile Throwable error;

    private SwingTestHelper() {
    }

    public static SwingTestHelper create() {
        return new SwingTestHelper();
    }

    public SwingTestHelper runInEventDispatchThread(final Action0 action) {
        Worker inner = SwingScheduler.getInstance().createWorker();
        inner.schedule(new Action0() {

            @Override
            public void call() {
                try {
                    action.call();
                } catch (Throwable e) {
                    error = e;
                }
                latch.countDown();
            }
        });
        return this;
    }

    public void awaitTerminal() throws Throwable {
        latch.await();
        if (error != null) {
            throw error;
        }
    }

    public void awaitTerminal(long timeout, TimeUnit unit) throws Throwable {
        latch.await(timeout, unit);
        if (error != null) {
            throw error;
        }
    }

}
