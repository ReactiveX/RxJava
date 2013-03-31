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
import rx.util.functions.Action0;
import rx.util.functions.Func0;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class ExecutorScheduler extends AbstractScheduler {
    private final Executor executor;

    public ExecutorScheduler(Executor executor) {
        this.executor = executor;
    }

    @Override
    public Subscription schedule(Action0 action, long dueTime, TimeUnit unit) {
        
        // this should delegate to ScheduledExecutorServiceScheduler
        // and that should be an implemenation detail I think ... not be a choice someone needs to make
        
        return super.schedule(action, dueTime, unit);
    }
    
    @Override
    public Subscription schedule(Func0<Subscription> action) {
        final DiscardableAction discardableAction = new DiscardableAction(action);

        // if it's a delayed Action (has a TimeUnit) then we should use a timer
        // otherwise it will tie up a thread and sleep
        // ... see the method above ...
        
        executor.execute(new Runnable() {
            @Override
            public void run() {
                discardableAction.call();
            }
        });

        return discardableAction;

    }
}
