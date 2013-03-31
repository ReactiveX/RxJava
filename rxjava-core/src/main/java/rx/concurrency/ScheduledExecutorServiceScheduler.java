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

import rx.Subscription;
import rx.util.functions.Func0;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutorServiceScheduler extends AbstractScheduler {
    private final ScheduledExecutorService executorService;

    
    // this should probably just become an implementation detail of ExecutorScheduler
    
    
    public ScheduledExecutorServiceScheduler(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public Subscription schedule(Func0<Subscription> action) {
        return schedule(action, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public Subscription schedule(Func0<Subscription> action, long dueTime, TimeUnit unit) {
        final DiscardableAction discardableAction = new DiscardableAction(action);
        executorService.schedule(new Runnable() {
            @Override
            public void run() {
                discardableAction.call();
            }
        }, dueTime, unit);
        return discardableAction;
    }

}
