package io.reactivex.processors;

import io.reactivex.subscribers.TestSubscriber;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public abstract class DelayedFlowableProcessorTest<T> extends FlowableProcessorTest<T> {

    @Test
    public void onNextNullDelayed() {
        final FlowableProcessor<T> p = create();

        TestSubscriber<T> ts = p.test();

        p.onNext(null);



        ts
                .assertNoValues()
                .assertError(NullPointerException.class)
                .assertErrorMessage("onNext called with null. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void onErrorNullDelayed() {
        final FlowableProcessor<T> p = create();

        TestSubscriber<T> ts = p.test();

        p.onError(null);
        assertFalse(p.hasSubscribers());

        ts
                .assertNoValues()
                .assertError(NullPointerException.class)
                .assertErrorMessage("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
    }
}
