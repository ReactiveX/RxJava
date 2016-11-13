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

package rx;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.pushtorefresh.private_constructor_checker.PrivateConstructorChecker;

import rx.Scheduler.Worker;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 * Common test utility methods.
 */
public enum TestUtil {
    ;

    /**
     * Verifies that the given class has a private constructor that
     * throws IllegalStateException("No instances!") upon instantiation.
     * @param clazz the target class to check
     */
    public static void checkUtilityClass(Class<?> clazz) {
        PrivateConstructorChecker
            .forClass(clazz)
            .expectedTypeOfException(IllegalStateException.class)
            .expectedExceptionMessage("No instances!")
            .check();
    }

    /**
     * Runs two actions concurrently, one in the current thread and the other on
     * the IO scheduler, synchronizing their execution as much as possible; rethrowing
     * any exceptions they produce.
     * <p>This helper waits until both actions have run or times out in 5 seconds.
     * @param r1 the first action
     * @param r2 the second action
     */
    public static void race(Action0 r1, final Action0 r2) {
        final AtomicInteger counter = new AtomicInteger(2);
        final Throwable[] errors = { null, null };
        final CountDownLatch cdl = new CountDownLatch(1);

        Worker w = Schedulers.io().createWorker();

        try {

            w.schedule(new Action0() {
                @Override
                public void call() {
                    if (counter.decrementAndGet() != 0) {
                        while (counter.get() != 0) { }
                    }

                    try {
                        r2.call();
                    } catch (Throwable ex) {
                        errors[1] = ex;
                    }

                    cdl.countDown();
                }
            });

            if (counter.decrementAndGet() != 0) {
                while (counter.get() != 0) { }
            }

            try {
                r1.call();
            } catch (Throwable ex) {
                errors[0] = ex;
            }

            List<Throwable> errorList = new ArrayList<Throwable>();

            try {
                if (!cdl.await(5, TimeUnit.SECONDS)) {
                    errorList.add(new TimeoutException());
                }
            } catch (InterruptedException ex) {
                errorList.add(ex);
            }

            if (errors[0] != null) {
                errorList.add(errors[0]);
            }

            if (errors[1] != null) {
                errorList.add(errors[1]);
            }

            Exceptions.throwIfAny(errorList);
        } finally {
            w.unsubscribe();
        }
    }
}
