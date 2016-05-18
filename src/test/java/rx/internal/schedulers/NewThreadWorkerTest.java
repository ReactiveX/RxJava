/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.schedulers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.concurrent.*;

import org.junit.Test;

public class NewThreadWorkerTest {

    @Test
    public void findSetRemoveOnCancelPolicyMethodShouldFindMethod() {
        ScheduledExecutorService executor = spy(new ScheduledThreadPoolExecutor(1));
        Method setRemoveOnCancelPolicyMethod = NewThreadWorker.findSetRemoveOnCancelPolicyMethod(executor);

        assertNotNull(setRemoveOnCancelPolicyMethod);
        assertEquals("setRemoveOnCancelPolicy", setRemoveOnCancelPolicyMethod.getName());
        assertEquals(1, setRemoveOnCancelPolicyMethod.getParameterTypes().length);
        assertEquals(Boolean.TYPE, setRemoveOnCancelPolicyMethod.getParameterTypes()[0]);
        verifyZeroInteractions(executor);
    }

    @Test
    public void findSetRemoveOnCancelPolicyMethodShouldNotFindMethod() {
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

        Method setRemoveOnCancelPolicyMethod = NewThreadWorker.findSetRemoveOnCancelPolicyMethod(executor);
        assertNull(setRemoveOnCancelPolicyMethod);
        verifyZeroInteractions(executor);
    }

    private static abstract class ScheduledExecutorServiceWithSetRemoveOnCancelPolicy implements ScheduledExecutorService {
        // Just declaration of required method to allow run tests on JDK 6
        public void setRemoveOnCancelPolicy(boolean value) {}
    }

    @Test
    public void tryEnableCancelPolicyShouldInvokeMethodOnExecutor() {
        ScheduledExecutorServiceWithSetRemoveOnCancelPolicy executor
                = mock(ScheduledExecutorServiceWithSetRemoveOnCancelPolicy.class);

        boolean result = NewThreadWorker.tryEnableCancelPolicy(executor);

        assertTrue(result);
        verify(executor).setRemoveOnCancelPolicy(true);
        verifyNoMoreInteractions(executor);
    }

    @Test
    public void tryEnableCancelPolicyShouldNotInvokeMethodOnExecutor() {
        // This executor does not have setRemoveOnCancelPolicy method
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

        boolean result = NewThreadWorker.tryEnableCancelPolicy(executor);

        assertFalse(result);
        verifyZeroInteractions(executor);
    }
}
