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
package rx.schedulers;

import rx.Scheduler;
import rx.internal.schedulers.NewThreadWorker;
import rx.internal.util.RxThreadFactory;

/**
 * Schedules work on a new thread.
 */
public final class NewThreadScheduler extends Scheduler {

    private static final String THREAD_NAME_PREFIX = "RxNewThreadScheduler-";
    private static final RxThreadFactory THREAD_FACTORY = new RxThreadFactory(THREAD_NAME_PREFIX);
    private static final NewThreadScheduler INSTANCE = new NewThreadScheduler();

    /* package */static NewThreadScheduler instance() {
        return INSTANCE;
    }

    private NewThreadScheduler() {

    }

    @Override
    public Worker createWorker() {
        return new NewThreadWorker(THREAD_FACTORY);
    }
}
