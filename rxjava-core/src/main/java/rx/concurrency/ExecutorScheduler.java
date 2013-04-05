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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Func0;

/**
 * A {@link Scheduler} implementation that uses an {@link Executor} or {@link ScheduledExecutorService} implementation.
 * <p>
 * Note that if an {@link Executor} implementation is used instead of {@link ScheduledExecutorService} then scheduler events requiring delays will not work and an IllegalStateException be thrown.
 */
public class ExecutorScheduler extends AbstractScheduler {
    private final Executor executor;

    public ExecutorScheduler(Executor executor) {
        this.executor = executor;
    }

    public ExecutorScheduler(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public Subscription schedule(Func0<Subscription> action, long dueTime, TimeUnit unit) {
        if (executor instanceof ScheduledExecutorService) {
            final DiscardableAction discardableAction = new DiscardableAction(action);
            ((ScheduledExecutorService) executor).schedule(new Runnable() {
                @Override
                public void run() {
                    discardableAction.call();
                }
            }, dueTime, unit);
            return discardableAction;
        } else {
            throw new IllegalStateException("Delayed scheduling is not supported with 'Executor' please use 'ScheduledExecutorServiceScheduler'");
        }
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
