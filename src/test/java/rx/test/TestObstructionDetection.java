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
package rx.test;

import java.util.*;
import java.util.concurrent.*;

import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 * Check if there is an obstruction in the computation scheduler.
 * Put the following code into test classes:
 * <code><pre>
 * @org.junit.After
 * public void doAfterTest() {
 *     rx.test.TestObstructionDetection.checkObstruction();
 * }
 * </pre></code>
 * or
 * <pre><code>
 * @org.junit.AfterClass
 * public static void doAfterClass() {
 *     rx.test.TestObstructionDetection.checkObstruction();
 * }
 * </code></pre>
 */
public final class TestObstructionDetection {
    private TestObstructionDetection() {
        throw new IllegalStateException("No instances!");
    }
    /**
     * Checks if tasks can be immediately executed on the computation scheduler.
     * @throws ObstructionException if the schedulers don't respond within 1 second
     */
    public static void checkObstruction() {
        final int ncpu = Runtime.getRuntime().availableProcessors();
        
        final CountDownLatch cdl = new CountDownLatch(ncpu);
        final List<Scheduler.Worker> workers = new ArrayList<Scheduler.Worker>();
        final Action0 task = new Action0() {
            @Override
            public void call() {
                cdl.countDown();
            }
        };
        
        for (int i = 0; i < ncpu; i++) {
            workers.add(Schedulers.computation().createWorker());
        }
        for (Scheduler.Worker w : workers) {
            w.schedule(task);
        }
        try {
            if (!cdl.await(1, TimeUnit.SECONDS)) {
                throw new ObstructionException("Obstruction/Timeout detected!");
            }
        } catch (InterruptedException ex) {
            throw new ObstructionException("Interrupted: " + ex);
        } finally {
            for (Scheduler.Worker w : workers) {
                w.unsubscribe();
            }
        }
    }
    /**
     * Exception thrown if obstruction was detected.
     */
    public static final class ObstructionException extends RuntimeException {
        /** */
        private static final long serialVersionUID = -6380717994471291795L;
        public ObstructionException(String message) {
            super(message);
        }
    }
}
