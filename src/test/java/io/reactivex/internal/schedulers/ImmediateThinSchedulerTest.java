/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.internal.schedulers;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.Scheduler.Worker;
import io.reactivex.internal.functions.Functions;

public class ImmediateThinSchedulerTest {

    @Test
    public void scheduleDirect() {
        final int[] count = { 0 };

        ImmediateThinScheduler.INSTANCE.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                count[0]++;
            }
        });

        assertEquals(1, count[0]);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void scheduleDirectTimed() {
        ImmediateThinScheduler.INSTANCE.scheduleDirect(Functions.EMPTY_RUNNABLE, 1, TimeUnit.SECONDS);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void scheduleDirectPeriodic() {
        ImmediateThinScheduler.INSTANCE.schedulePeriodicallyDirect(Functions.EMPTY_RUNNABLE, 1, 1, TimeUnit.SECONDS);
    }
    @Test
    public void schedule() {
        final int[] count = { 0 };

        Worker w = ImmediateThinScheduler.INSTANCE.createWorker();

        assertFalse(w.isDisposed());

        w.schedule(new Runnable() {
            @Override
            public void run() {
                count[0]++;
            }
        });

        assertEquals(1, count[0]);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void scheduleTimed() {
        ImmediateThinScheduler.INSTANCE.createWorker().schedule(Functions.EMPTY_RUNNABLE, 1, TimeUnit.SECONDS);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void schedulePeriodic() {
        ImmediateThinScheduler.INSTANCE.createWorker().schedulePeriodically(Functions.EMPTY_RUNNABLE, 1, 1, TimeUnit.SECONDS);
    }
}
