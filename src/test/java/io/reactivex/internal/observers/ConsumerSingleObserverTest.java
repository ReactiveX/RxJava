package io.reactivex.internal.observers;

import io.reactivex.internal.functions.Functions;
import org.junit.Test;

import static org.junit.Assert.*;

public final class ConsumerSingleObserverTest {

    @Test
    public void hasMissingErrorConsumer() {
        ConsumerSingleObserver<Integer> o = new ConsumerSingleObserver<Integer>(Functions.<Integer>emptyConsumer(),
                Functions.ON_ERROR_MISSING);

        assertTrue(o.hasCustomOnError());
    }

    @Test
    public void isNotMissingErrorConsumer() {
        ConsumerSingleObserver<Integer> o = new ConsumerSingleObserver<Integer>(Functions.<Integer>emptyConsumer(),
                Functions.<Throwable>emptyConsumer());

        assertFalse(o.hasCustomOnError());
    }

}
