package rx.internal.schedulers;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.reflect.Modifier.FINAL;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
        public void setRemoveOnCancelPolicy(@SuppressWarnings("UnusedParameters") boolean value) {}
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
