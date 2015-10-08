package rx.internal.util;

import org.junit.Test;
import rx.Subscription;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class UtilityFunctionsTest {

    @Test
    public void awaitForCompletionShouldNotAwaitIfLatchGetCountAlreadyZero() {
        CountDownLatch latch = mock(CountDownLatch.class);
        when(latch.getCount()).thenReturn((long) 0);

        Subscription subscription = mock(Subscription.class);

        UtilityFunctions.awaitForCompletion(latch, subscription);

        verify(latch).getCount();
        verifyNoMoreInteractions(latch, subscription);
    }

    @Test
    public void awaitForCompletionShouldAwaitIfLatchGetCountIsNotZero() throws InterruptedException {
        CountDownLatch latch = mock(CountDownLatch.class);
        when(latch.getCount()).thenReturn((long) 1);

        Subscription subscription = mock(Subscription.class);

        UtilityFunctions.awaitForCompletion(latch, subscription);

        verify(latch).getCount();
        verify(latch).await();
        verifyNoMoreInteractions(latch, subscription);
    }

    @Test
    public void awaitForCompletionInCaseOfThreadInterruption() throws InterruptedException {
        CountDownLatch latch = mock(CountDownLatch.class);
        when(latch.getCount()).thenReturn((long) 1);

        InterruptedException exception = new InterruptedException();

        doThrow(exception).when(latch).await();

        Subscription subscription = mock(Subscription.class);

        try {
            UtilityFunctions.awaitForCompletion(latch, subscription);
            fail();
        } catch (RuntimeException expected) {
            assertSame(exception, expected.getCause());
        }

        verify(latch).getCount();
        verify(latch).await();
        verify(subscription).unsubscribe();
        verifyNoMoreInteractions(latch, subscription);
    }

    @Test
    public void throwErrorOrReturnValueShouldThrowRuntimeException() {
        Object value = new Object();
        RuntimeException error = new RuntimeException();

        try {
            UtilityFunctions.throwErrorOrReturnValue(value, error);
            fail();
        } catch (RuntimeException expected) {
            assertSame(error, expected);
        }
    }

    @Test
    public void throwErrorOrReturnValueShouldThrowCheckedException() {
        Object value = new Object();
        Exception error = new Exception();

        try {
            UtilityFunctions.throwErrorOrReturnValue(value, error);
            fail();
        } catch (RuntimeException expected) {
            assertSame(error, expected.getCause());
        }
    }

    @Test
    public void throwErrorOrReturnValueShouldReturnValue() {
        Object value = new Object();
        Throwable error = null;

        Object returnedValue = UtilityFunctions.throwErrorOrReturnValue(value, error);
        assertSame(value, returnedValue);
    }
}