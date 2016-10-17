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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

import org.junit.Test;

import co.touchlab.doppel.testing.MockGen;

@MockGen(classes = {"java.util.concurrent.ScheduledThreadPoolExecutor", "rx.internal.schedulers.NewThreadWorkerTest.ScheduledExecutorServiceWithSetRemoveOnCancelPolicy"})
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

    public static class ScheduledExecutorServiceWithSetRemoveOnCancelPolicy implements ScheduledExecutorService {
        // Just declaration of required method to allow run tests on JDK 6
        public void setRemoveOnCancelPolicy(boolean value) {}

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
        {
            return null;
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
        {
            return null;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
        {
            return null;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
        {
            return null;
        }

        @Override
        public void shutdown()
        {

        }

        @Override
        public List<Runnable> shutdownNow()
        {
            return null;
        }

        @Override
        public boolean isShutdown()
        {
            return false;
        }

        @Override
        public boolean isTerminated()
        {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
        {
            return false;
        }

        @Override
        public <T> Future<T> submit(Callable<T> task)
        {
            return null;
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result)
        {
            return null;
        }

        @Override
        public Future<?> submit(Runnable task)
        {
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException
        {
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException
        {
            return null;
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException
        {
            return null;
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
        {
            return null;
        }

        @Override
        public void execute(Runnable command)
        {

        }
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
