package io.reactivex.internal.observers;

import io.reactivex.internal.functions.Functions;
import org.junit.Test;

import static org.junit.Assert.*;

public final class CallbackCompletableObserverTest {

    @Test
    public void hasMissingErrorConsumer() {
        CallbackCompletableObserver o = new CallbackCompletableObserver(Functions.EMPTY_ACTION);

        assertTrue(o.onErrorImplemented());
    }

    @Test
    public void isNotMissingErrorConsumer() {
        CallbackCompletableObserver o = new CallbackCompletableObserver(Functions.<Throwable>emptyConsumer(),
                Functions.EMPTY_ACTION);

        assertFalse(o.onErrorImplemented());
    }

}
