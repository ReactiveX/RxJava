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
package rx.schedulers;

import org.junit.Test;

import rx.Scheduler;

public class ImmediateSchedulerTest extends AbstractSchedulerTests {

    @Override
    protected Scheduler getScheduler() {
        return ImmediateScheduler.getInstance();
    }

    @Override
    @Test
    public final void testNestedActions() {
        // ordering of nested actions will not match other schedulers
        // because there is no reordering or concurrency with ImmediateScheduler
    }

    @Override
    @Test
    public final void testSequenceOfDelayedActions() {
        // ordering of nested actions will not match other schedulers
        // because there is no reordering or concurrency with ImmediateScheduler
    }

    @Override
    @Test
    public final void testMixOfDelayedAndNonDelayedActions() {
        // ordering of nested actions will not match other schedulers
        // because there is no reordering or concurrency with ImmediateScheduler
    }

}
