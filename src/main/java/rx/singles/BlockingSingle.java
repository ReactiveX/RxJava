/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rx.singles;

import rx.Single;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.annotations.Experimental;
import rx.internal.operators.BlockingOperatorToFuture;
import rx.internal.util.BlockingUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@code BlockingSingle} is a blocking "version" of {@link Single} that provides blocking
 * operators.
 * <p/>
 * You construct a {@code BlockingSingle} from a {@code Single} with {@link #from(Single)}
 * or {@link Single#toBlocking()}.
 * 
 * @param <T> the value type of the sequence
 */
@Experimental
public class BlockingSingle<T> {
    private final Single<? extends T> single;

    private BlockingSingle(Single<? extends T> single) {
        this.single = single;
    }

    /**
     * Converts a {@link Single} into a {@code BlockingSingle}.
     *
     * @param <T> the value type of the sequence
     * @param single the {@link Single} you want to convert
     * @return a {@code BlockingSingle} version of {@code single}
     */
    @Experimental
    public static <T> BlockingSingle<T> from(Single<? extends T> single) {
        return new BlockingSingle<T>(single);
    }

    /**
     * Returns the item emitted by this {@code BlockingSingle}.
     * <p/>
     * If the underlying {@link Single} returns successfully, the value emitted
     * by the {@link Single} is returned. If the {@link Single} emits an error,
     * the throwable emitted ({@link SingleSubscriber#onError(Throwable)}) is
     * thrown.
     *
     * @return the value emitted by this {@code BlockingSingle}
     */
    @Experimental
    public T value() {
        final AtomicReference<T> returnItem = new AtomicReference<T>();
        final AtomicReference<Throwable> returnException = new AtomicReference<Throwable>();
        final CountDownLatch latch = new CountDownLatch(1);
        Subscription subscription = single.subscribe(new SingleSubscriber<T>() {
            @Override
            public void onSuccess(T value) {
                returnItem.set(value);
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                returnException.set(error);
                latch.countDown();
            }
        });

        BlockingUtils.awaitForComplete(latch, subscription);
        Throwable throwable = returnException.get();
        if (throwable != null) {
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            throw new RuntimeException(throwable);
        }
        return returnItem.get();
    }

    /**
     * Returns a {@link Future} representing the value emitted by this {@code BlockingSingle}.
     *
     * @return a {@link Future} that returns the value
     */
    @SuppressWarnings("unchecked")
    @Experimental
    public Future<T> toFuture() {
        return BlockingOperatorToFuture.toFuture(((Single<T>)single).toObservable());
    }
}

