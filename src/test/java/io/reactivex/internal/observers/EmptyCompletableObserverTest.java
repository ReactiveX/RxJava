package io.reactivex.internal.observers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public final class EmptyCompletableObserverTest {

    @Test
    public void hasMissingErrorConsumer() {
        EmptyCompletableObserver o = new EmptyCompletableObserver();

        assertTrue(o.hasMissingErrorConsumer());
    }
}
