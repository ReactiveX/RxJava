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
package rx.concurrency;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Func0;

/**
 * A {@link Scheduler} implementation that uses an {@link Executor} or {@link ScheduledExecutorService} implementation.
 * <p>
 * Note that if an {@link Executor} implementation is used instead of {@link ScheduledExecutorService} then a system-wide Timer will be used to handle delayed events.
 */
public class ExecutorScheduler extends AbstractScheduler {
    private final Executor executor;

    /**
     * Setup a ScheduledExecutorService that we can use if someone provides an Executor instead of ScheduledExecutorService.
     */
    private final static ScheduledExecutorService SYSTEM_SCHEDULED_EXECUTOR;
    static {
        int count = Runtime.getRuntime().availableProcessors();
        if (count > 8) {
            count = count / 2;
        }
        // we don't need more than 8 to handle just scheduling and doing no work
        if (count > 8) {
            count = 8;
        }
        SYSTEM_SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(count, new ThreadFactory() {

            final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "RxScheduledExecutorPool-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }

        });

    }

    public ExecutorScheduler(Executor executor) {
        this.executor = executor;
    }

    public ExecutorScheduler(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public Subscription schedule(Func0<Subscription> action, long dueTime, TimeUnit unit) {
        final DiscardableAction discardableAction = new DiscardableAction(action);

        if (executor instanceof ScheduledExecutorService) {
            ((ScheduledExecutorService) executor).schedule(new Runnable() {
                @Override
                public void run() {
                    discardableAction.call();
                }
            }, dueTime, unit);
        } else {
            if (dueTime == 0) {
                // no delay so put on the thread-pool right now
                return (schedule(action));
            } else {
                // there is a delay and this isn't a ScheduledExecutorService so we'll use a system-wide ScheduledExecutorService
                // to handle the scheduling and once it's ready then execute on this Executor
                SYSTEM_SCHEDULED_EXECUTOR.schedule(new Runnable() {

                    @Override
                    public void run() {
                        // now execute on the real Executor
                        executor.execute(new Runnable() {

                            @Override
                            public void run() {
                                discardableAction.call();
                            }

                        });
                    }
                }, dueTime, unit);
            }
        }
        return discardableAction;
    }

    @Override
    public Subscription schedule(Func0<Subscription> action) {
        final DiscardableAction discardableAction = new DiscardableAction(action);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                discardableAction.call();
            }
        });

        return discardableAction;

    }

}
