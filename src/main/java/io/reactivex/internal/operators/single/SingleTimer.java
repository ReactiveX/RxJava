/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.single;

import java.util.concurrent.TimeUnit;

import io.reactivex.*;
import io.reactivex.disposables.MultipleAssignmentDisposable;

public final class SingleTimer extends Single<Long> {

    final long delay;
    final TimeUnit unit;
    final Scheduler scheduler;
    
    public SingleTimer(long delay, TimeUnit unit, Scheduler scheduler) {
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(final SingleSubscriber<? super Long> s) {
        MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
        
        s.onSubscribe(mad);
        
        mad.set(scheduler.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                s.onSuccess(0L);
            }
        }, delay, unit));
    }

}
