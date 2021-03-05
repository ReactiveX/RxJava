/*
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

package io.reactivex.rxjava3.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class SchedulerTest {
    private static final String DRIFT_USE_NANOTIME = "rx3.scheduler.use-nanotime";

    @After
    public void cleanup() {
      // reset value to default in order to not influence other tests
      Scheduler.IS_DRIFT_USE_NANOTIME = false;
    }

    @Test
    public void driftUseNanoTimeNotSetByDefault() {
      assertFalse(Scheduler.IS_DRIFT_USE_NANOTIME);
      assertFalse(Boolean.getBoolean(DRIFT_USE_NANOTIME));
    }

    @Test
    public void computeNow_currentTimeMillis() {
      TimeUnit unit = TimeUnit.MILLISECONDS;
      assertTrue(isInRange(System.currentTimeMillis(), Scheduler.computeNow(unit), unit, 250, TimeUnit.MILLISECONDS));
    }

    @Test
    public void computeNow_nanoTime() {
      TimeUnit unit = TimeUnit.NANOSECONDS;
      Scheduler.IS_DRIFT_USE_NANOTIME = true;

      assertFalse(isInRange(System.currentTimeMillis(), Scheduler.computeNow(unit), unit, 250, TimeUnit.MILLISECONDS));
      assertTrue(isInRange(System.nanoTime(), Scheduler.computeNow(unit), TimeUnit.NANOSECONDS, 250, TimeUnit.MILLISECONDS));
    }

    private boolean isInRange(long start, long stop, TimeUnit source, long maxDiff, TimeUnit diffUnit) {
      long diff = Math.abs(stop - start);
      return diffUnit.convert(diff, source) <= maxDiff;
    }

    @Test
    public void clockDriftCalculation() {
        assertEquals(100_000_000L, Scheduler.computeClockDrift(100, "milliseconds"));

        assertEquals(2_000_000_000L, Scheduler.computeClockDrift(2, "seconds"));

        assertEquals(180_000_000_000L, Scheduler.computeClockDrift(3, "minutes"));

        assertEquals(240_000_000_000L, Scheduler.computeClockDrift(4, "random"));

        assertEquals(300_000_000_000L, Scheduler.computeClockDrift(5, null));
    }

}
