/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.completable;

import io.reactivex.Completable;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CompletableFromCallableTest {
    @Test(expected = NullPointerException.class)
    public void fromCallableNull() {
        Completable.fromCallable(null);
    }

    @Test
    public void fromCallable() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                atomicInteger.incrementAndGet();
                return null;
            }
        })
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromCallableTwice() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Callable<Object> callable = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                atomicInteger.incrementAndGet();
                return null;
            }
        };

        Completable.fromCallable(callable)
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());

        Completable.fromCallable(callable)
            .test()
            .assertResult();

        assertEquals(2, atomicInteger.get());
    }

    @Test
    public void fromCallableInvokesLazy() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Completable completable = Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                atomicInteger.incrementAndGet();
                return null;
            }
        });

        assertEquals(0, atomicInteger.get());

        completable
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromCallableThrows() {
        Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new UnsupportedOperationException();
            }
        })
            .test()
            .assertFailure(UnsupportedOperationException.class);
    }
}
