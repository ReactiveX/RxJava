package rx.singles;

import org.junit.Test;
import rx.Single;
import rx.SingleSubscriber;
import rx.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class BlockingSingleTest {

    @Test
    public void callToValueShouldReturnValueOfSynchronousSingle() {
        String value = Single
                .just("return me")
                .toBlocking()
                .value();

        assertEquals("return me", value);
    }

    @Test
    public void callToValueShouldThrowErrorOfSynchronousSingle() {
        BlockingSingle<Object> single = Single
                .error(new IllegalStateException("hello there"))
                .toBlocking();

        try {
            single.value();
            fail();
        } catch (IllegalStateException expected) {
            assertEquals("hello there", expected.getMessage());
        }
    }

    @Test
    public void callToValueShouldReturnValueOfAsyncSingle() {
        String value = Single
                .create(new Single.OnSubscribe<String>() {
                    @Override
                    public void call(SingleSubscriber<? super String> singleSubscriber) {
                        singleSubscriber.onSuccess("I am async");
                    }
                })
                .subscribeOn(Schedulers.computation())
                .toBlocking()
                .value();

        assertEquals("I am async", value);
    }

    @Test
    public void callToValueShouldThrowErrorOfAsyncSingle() {
        BlockingSingle<Object> single = Single
                .create(new Single.OnSubscribe<Object>() {
                    @Override
                    public void call(SingleSubscriber<? super Object> singleSubscriber) {
                        singleSubscriber.onError(new IllegalStateException("errors Async occur :("));
                    }
                })
                .subscribeOn(Schedulers.computation())
                .toBlocking();

        try {
            single.value();
            fail();
        } catch (IllegalStateException expected) {
            assertEquals("errors Async occur :(", expected.getMessage());
        }
    }
}