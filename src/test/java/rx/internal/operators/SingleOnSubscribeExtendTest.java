/*
 * Copyright 2016 Netflix, Inc.
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
package rx.internal.operators;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.*;
import rx.Single.OnSubscribe;
import rx.functions.Func1;

import static org.junit.Assert.*;

public final class SingleOnSubscribeExtendTest {
    @Test
    public void convertToEither() {
        Either<String> maybeHi = Single.just("Hi").extend(toEither());
        assertTrue(maybeHi.hasValue());
        assertFalse(maybeHi.hasThrowable());
        assertEquals("Hi", maybeHi.value());

        RuntimeException e = new RuntimeException();
        Either<String> maybeThrowable = Single.<String>error(e).extend(toEither());
        assertFalse(maybeThrowable.hasValue());
        assertTrue(maybeThrowable.hasThrowable());
        assertSame(e, maybeThrowable.throwable());
    }

    private Func1<OnSubscribe<String>, Either<String>> toEither() {
        return new Func1<OnSubscribe<String>, Either<String>>() {
            @Override
            public Either<String> call(OnSubscribe<String> onSubscribe) {
                final AtomicReference<Either<String>> eitherRef = new AtomicReference<Either<String>>();
                final CountDownLatch latch = new CountDownLatch(1);
                onSubscribe.call(new SingleSubscriber<String>() {
                    @Override
                    public void onSuccess(String value) {
                        eitherRef.set(Either.ofValue(value));
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable error) {
                        eitherRef.set(Either.<String>ofThrowable(error));
                        latch.countDown();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    return Either.ofThrowable(e);
                }
                return eitherRef.get();
            }
        };
    }

    static final class Either<T> {
        static <T> Either<T> ofValue(T value) {
            return new Either<T>(value, null);
        }

        static <T> Either<T> ofThrowable(Throwable throwable) {
            return new Either<T>(null, throwable);
        }

        private final T value;
        private final Throwable throwable;

        private Either(T value, Throwable throwable) {
            this.value = value;
            this.throwable = throwable;
        }

        public boolean hasValue() {
            return throwable == null; // Allows null as value.
        }

        public T value() {
            if (throwable != null) {
                throw new NullPointerException("No value.");
            }
            return value;
        }

        public boolean hasThrowable() {
            return throwable != null;
        }

        public Throwable throwable() {
            if (throwable == null) {
                throw new NullPointerException("No throwable.");
            }
            return throwable;
        }
    }
}
