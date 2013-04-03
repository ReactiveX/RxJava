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

import rx.Scheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

public class Schedulers {
    private Schedulers() {

    }

    public static Scheduler immediate() {
        return ImmediateScheduler.getInstance();
    }

    public static Scheduler currentThread() {
        return CurrentThreadScheduler.getInstance();
    }

    public static Scheduler newThread() {
        return NewThreadScheduler.getInstance();
    }

    public static Scheduler executor(Executor executor) {
        return new ExecutorScheduler(executor);
    }

    public static Scheduler fromScheduledExecutorService(ScheduledExecutorService executor) {
        return new ScheduledExecutorServiceScheduler(executor);
    }

    public static Scheduler forwardingScheduler(Scheduler underlying) {
        return new ForwardingScheduler(underlying);
    }
}
