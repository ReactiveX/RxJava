package rx.singles;

import rx.Single;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.internal.util.UtilityFunctions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public final class BlockingSingle<T> {

    private final Single<T> single;

    private BlockingSingle(Single<T> single) {
        this.single = single;
    }

    public static <T> BlockingSingle<T> from(Single<T> single) {
        return new BlockingSingle<T>(single);
    }

    /**
     * Blocks until {@linkplain Single} completes and returns emitted value or throws occurred exception.
     *
     * @return the value emitted by the {@linkplain Single}
     */
    public T value() {
        return blockAndThrowErrorOrReturnValue();
    }

    private T blockAndThrowErrorOrReturnValue() {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<T> valueToReturn = new AtomicReference<T>();
        final AtomicReference<Throwable> errorToThrow = new AtomicReference<Throwable>();

        final Subscription subscription = single.subscribe(new SingleSubscriber<T>() {
            @Override
            public void onSuccess(T value) {
                valueToReturn.set(value);
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                errorToThrow.set(error);
                latch.countDown();
            }
        });

        UtilityFunctions.awaitForCompletion(latch, subscription);

        return UtilityFunctions.throwErrorOrReturnValue(valueToReturn.get(), errorToThrow.get());
    }

}
