package io.reactivex.internal.observers;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public final class EmptyCompletableObserverTest {

    @Test
    public void defaultShouldReportNoCustomOnError() {
        EmptyCompletableObserver o = new EmptyCompletableObserver();

        assertFalse(o.hasCustomOnError());
    }
}
