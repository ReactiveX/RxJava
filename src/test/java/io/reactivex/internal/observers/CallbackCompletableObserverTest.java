package io.reactivex.internal.observers;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.Functions;
import org.junit.Test;

import static org.junit.Assert.*;

public final class CallbackCompletableObserverTest {

    @Test
    public void emptyActionShouldReportNoCustomOnError() {
        CallbackCompletableObserver o = new CallbackCompletableObserver(Functions.ON_ERROR_MISSING,
                Functions.EMPTY_ACTION,
                Functions.<Disposable>emptyConsumer());

        assertFalse(o.hasCustomOnError());
    }

    @Test
    public void customOnErrorShouldReportCustomOnError() {
        CallbackCompletableObserver o = new CallbackCompletableObserver(Functions.<Throwable>emptyConsumer(),
                Functions.EMPTY_ACTION, Functions.<Disposable>emptyConsumer());

        assertTrue(o.hasCustomOnError());
    }

}
