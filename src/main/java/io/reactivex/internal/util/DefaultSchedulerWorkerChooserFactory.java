/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.util;

import io.reactivex.Scheduler.Worker;
import io.reactivex.internal.schedulers.NewThreadWorker;

/**
 * Default implementation to choose next {@link Worker}. There are two options, one is simple round-robin and the other
 * is non-round-robin. Simple round-robin uses bit operations and modulo operations. Just use bit calculation instead of
 * modulo calculation when works length is power of two. Non-round-robin operations are only suitable for the worker
 * selection of sub-class of {@link NewThreadWorker} which essence is to select the minimum computational thread pool by
 * comparing thread pool inner queue size.
 */
public class DefaultSchedulerWorkerChooserFactory implements SchedulerWorkerChooserFactory {

    public static final DefaultSchedulerWorkerChooserFactory INSTANCE = new DefaultSchedulerWorkerChooserFactory();

    private static final String IS_NOT_ROUND_ROBIN = "rx2.no-round-robin";

    private DefaultSchedulerWorkerChooserFactory() {
    }

    @Override
    public SchedulerWorkerChooser newChooser(Worker[] eventLoops) {
        if (eventLoops instanceof NewThreadWorker[] && Boolean.getBoolean(IS_NOT_ROUND_ROBIN)) {
            return new NoRoundRobinChooserFactory(eventLoops);
        }
        if (isPowerOfTwo(eventLoops.length)) {
            return new PowerOfTwoChooserFactory(eventLoops);
        } else {
            return new GenericChooserFactory(eventLoops);
        }
    }

    private static boolean isPowerOfTwo(int val) {
        return (val & -val) == val;
    }

    private static abstract class AbstractChooserFactory implements SchedulerWorkerChooser {

        private final Worker[] workers;

        private AbstractChooserFactory(Worker[] workers) {
            this.workers = workers;
        }

        @Override
        public abstract Worker next();
    }

    private static final class PowerOfTwoChooserFactory extends AbstractChooserFactory {

        private long idx = 0L;

        private PowerOfTwoChooserFactory(Worker[] workers) {
            super(workers);
        }

        @Override
        public Worker next() {
            return super.workers[(int) (idx++ & super.workers.length - 1)];
        }
    }

    private static final class GenericChooserFactory extends AbstractChooserFactory {

        private long idx = 0L;

        private GenericChooserFactory(Worker[] workers) {
            super(workers);
        }

        @Override
        public Worker next() {
            return super.workers[(int) (idx++ % super.workers.length)];
        }
    }

    private static final class NoRoundRobinChooserFactory extends AbstractChooserFactory {

        private NoRoundRobinChooserFactory(Worker[] workers) {
            super(workers);
        }

        @Override
        public Worker next() {
            NewThreadWorker[] workers = (NewThreadWorker[]) super.workers;
            int minQsWorkIndex = 0;
            int minQueue = workers[0].getInnerQueueSize();

            for (int i = 1; i < workers.length; i++) {
                if (minQueue > workers[i].getInnerQueueSize()) {
                    minQueue = workers[i].getInnerQueueSize();
                    minQsWorkIndex = i;
                }
            }

            return workers[minQsWorkIndex];
        }
    }
}
