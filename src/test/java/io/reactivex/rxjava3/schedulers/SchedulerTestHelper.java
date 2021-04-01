/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.schedulers;

import static org.junit.Assert.*;

import java.util.concurrent.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.subscribers.DefaultSubscriber;

final class SchedulerTestHelper {
    private SchedulerTestHelper() {
        // No instances.
    }

    /**
     * Verifies that the given Scheduler does not deliver handled errors to its executing Thread's
     * {@link java.lang.Thread.UncaughtExceptionHandler}.
     *
     * @param scheduler {@link Scheduler} to verify.
     */
    static void handledErrorIsNotDeliveredToThreadHandler(Scheduler scheduler) throws InterruptedException {
        Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        try {
            CapturingUncaughtExceptionHandler handler = new CapturingUncaughtExceptionHandler();
            CapturingObserver<Object> observer = new CapturingObserver<>();
            Thread.setDefaultUncaughtExceptionHandler(handler);
            IllegalStateException error = new IllegalStateException("Should be delivered to handler");
            Flowable.error(error)
                    .subscribeOn(scheduler)
                    .subscribe(observer);

            if (!observer.completed.await(3, TimeUnit.SECONDS)) {
                fail("timed out");
            }

            if (handler.count != 0) {
                handler.caught.printStackTrace();
            }
            assertEquals("Handler should not have received anything: " + handler.caught, 0, handler.count);
            assertEquals("Observer should have received an error", 1, observer.errorCount);
            assertEquals("Observer should not have received a next value", 0, observer.nextCount);

            Throwable cause = observer.error;
            while (cause != null) {
                if (error.equals(cause)) { break; }
                if (cause == cause.getCause()) { break; }
                cause = cause.getCause();
            }
            assertEquals("Our error should have been delivered to the observer", error, cause);
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler);
        }
    }

    private static final class CapturingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        int count;
        Throwable caught;
        CountDownLatch completed = new CountDownLatch(1);

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            count++;
            caught = e;
            completed.countDown();
        }
    }

    static final class CapturingObserver<T> extends DefaultSubscriber<T> {
        CountDownLatch completed = new CountDownLatch(1);
        int errorCount;
        int nextCount;
        Throwable error;

        @Override
        public void onComplete() {
        }

        @Override
        public void onError(Throwable e) {
            errorCount++;
            error = e;
            completed.countDown();
        }

        @Override
        public void onNext(T t) {
            nextCount++;
        }
    }
}
