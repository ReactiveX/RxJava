/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.schedulers;

import java.util.concurrent.*;

import io.reactivex.Scheduler;
import io.reactivex.internal.schedulers.*;

public final class Schedulers {
    
    /*
     * TODO I started to like enums for singletons and non-instantiatable
     * utility classes, but since this is part of the public API,
     * that would act quite unorthodoxically.
     */
    private Schedulers() {
        throw new IllegalStateException("No instances");
    }

    static final Scheduler single;
    
    static {
        // TODO plugins and stuff
        single = new SingleScheduler();
    }
    
    public static Scheduler computation() {
        // TODO implement
        throw new UnsupportedOperationException();
    }
    
    public static Scheduler io() {
        // TODO implement
        throw new UnsupportedOperationException();
    }
    
    public static TestScheduler test() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public static Scheduler trampoline() {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    /*
     * TODO This is a deliberately single threaded scheduler.
     * I can see a few uses for such scheduler:
     * - main event loop
     * - support Schedulers.from(Executor) and from(ExecutorService) with delayed scheduling.
     * - support benchmarks that pipeline data from the main thread to some other thread and avoid core-bashing of computation's round-robin nature. 
     */
    public static Scheduler single() {
        return single;
    }
    
    // TODO I don't think immediate scheduler should be supported any further
    @Deprecated
    public static Scheduler immediate() {
        throw new UnsupportedOperationException();
    }
    
    public static Scheduler from(Executor executor) {
        return new ExecutorScheduler(executor);
    }
}
