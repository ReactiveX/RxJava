package io.reactivex.processors;

import org.junit.Test;

public abstract class FlowableProcessorTest<T> {

    protected abstract FlowableProcessor<T> create();

    @Test
    public void onNextNull() {
        final FlowableProcessor<T> p = create();

        p.onNext(null);

        p.test()
                .assertNoValues()
                .assertError(NullPointerException.class)
                .assertErrorMessage("onNext called with null. Null values are generally not allowed in 2.x operators and sources.");
    }

    @Test
    public void onErrorNull() {
        final FlowableProcessor<T> p = create();

        p.onError(null);

        p.test()
                .assertNoValues()
                .assertError(NullPointerException.class)
                .assertErrorMessage("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
    }
}
