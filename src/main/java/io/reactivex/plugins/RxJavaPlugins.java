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
package io.reactivex.plugins;

import org.reactivestreams.*;

import io.reactivex.Scheduler;

public final class RxJavaPlugins {
    private RxJavaPlugins() {
        throw new IllegalStateException("No instances!");
    }
    
    /**
     * Called when an undeliverable error occurs.
     * @param error the error to report
     */
    public static void onError(Throwable error) {
        // TODO dispatch to the appropriate plugin
        if (error != null) {
            error.printStackTrace();
        } else {
            new NullPointerException().printStackTrace();
        }
    }
    
    /**
     * Called when a subscriber subscribes to an observable.
     * @param subscriber
     * @return
     */
    public static <T> Subscriber<T> onSubscribe(Subscriber<T> subscriber) {
        // TODO dispatch to the appropriate plugin
        return subscriber;
    }
    
    /**
     * Called when an Observable is created.
     * @param publisher
     * @return
     */
    public static <T> Publisher<T> onCreate(Publisher<T> publisher) {
        // TODO dispatch to the appropriate plugin
        return publisher;
    }

    /**
     * Called when a task is scheduled.
     * @param run
     * @return
     */
    public static Runnable onSchedule(Runnable run) {
        // TODO dispatch to the appropriate plugin
        return run;
    }
    
    public static Scheduler initComputationScheduler(Scheduler defaultScheduler) {
     // TODO dispatch to the appropriate plugin
        return defaultScheduler;
    }

    public static Scheduler initIOScheduler(Scheduler defaultScheduler) {
     // TODO dispatch to the appropriate plugin
        return defaultScheduler;
    }

    public static Scheduler initSingleScheduler(Scheduler defaultScheduler) {
     // TODO dispatch to the appropriate plugin
        return defaultScheduler;
    }
    
    public static Scheduler onComputationScheduler(Scheduler defaultScheduler) {
        // TODO dispatch to the appropriate plugin
        return defaultScheduler;
    }

    public static Scheduler onIOScheduler(Scheduler defaultScheduler) {
        // TODO dispatch to the appropriate plugin
        return defaultScheduler;
    }

    public static Scheduler onSingleScheduler(Scheduler defaultScheduler) {
        // TODO dispatch to the appropriate plugin
        return defaultScheduler;
    }

}
