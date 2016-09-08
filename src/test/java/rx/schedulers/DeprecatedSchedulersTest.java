/**
 * Copyright 2016 Netflix, Inc.
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
import static org.junit.Assert.*;

import rx.*;
import rx.internal.util.SuppressAnimalSniffer;
import rx.internal.util.unsafe.UnsafeAccess;

@SuppressWarnings("deprecation")
@SuppressAnimalSniffer
public class DeprecatedSchedulersTest {

    @Test
    public void immediate() {
        TestUtil.checkUtilityClass(ImmediateScheduler.class);
    }

    @Test
    public void newThread() {
        TestUtil.checkUtilityClass(NewThreadScheduler.class);
    }

    @Test
    public void trampoline() {
        TestUtil.checkUtilityClass(TrampolineScheduler.class);
    }

    void checkWorker(Class<?> schedulerClass) {
        if (UnsafeAccess.isUnsafeAvailable()) {
            try {
                Scheduler s =  (Scheduler)UnsafeAccess.UNSAFE.allocateInstance(schedulerClass);

                assertNull(s.createWorker());
            } catch (InstantiationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Test
    public void immediateWorker() {
        checkWorker(ImmediateScheduler.class);
    }

    @Test
    public void newThreadWorker() {
        checkWorker(NewThreadScheduler.class);
    }

    @Test
    public void trampolineWorker() {
        checkWorker(TrampolineScheduler.class);
    }


}
