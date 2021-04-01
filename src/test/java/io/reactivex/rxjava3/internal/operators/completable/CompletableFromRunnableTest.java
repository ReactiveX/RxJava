/*
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

package io.reactivex.rxjava3.internal.operators.completable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class CompletableFromRunnableTest extends RxJavaTest {
    @Test
    public void fromRunnable() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Completable.fromRunnable(new Runnable() {
            @Override
            public void run() {
                atomicInteger.incrementAndGet();
            }
        })
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromRunnableTwice() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Runnable run = new Runnable() {
            @Override
            public void run() {
                atomicInteger.incrementAndGet();
            }
        };

        Completable.fromRunnable(run)
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());

        Completable.fromRunnable(run)
            .test()
            .assertResult();

        assertEquals(2, atomicInteger.get());
    }

    @Test
    public void fromRunnableInvokesLazy() {
        final AtomicInteger atomicInteger = new AtomicInteger();

        Completable completable = Completable.fromRunnable(new Runnable() {
            @Override
            public void run() {
                atomicInteger.incrementAndGet();
            }
        });

        assertEquals(0, atomicInteger.get());

        completable
            .test()
            .assertResult();

        assertEquals(1, atomicInteger.get());
    }

    @Test
    public void fromRunnableThrows() {
        Completable.fromRunnable(new Runnable() {
            @Override
            public void run() {
                throw new UnsupportedOperationException();
            }
        })
            .test()
            .assertFailure(UnsupportedOperationException.class);
    }

    @Test
    public void fromRunnableDisposed() {
        final AtomicInteger calls = new AtomicInteger();
        Completable.fromRunnable(new Runnable() {
            @Override
            public void run() {
                calls.incrementAndGet();
            }
        })
        .test(true)
        .assertEmpty();

        assertEquals(0, calls.get());
    }

    @Test
    public void fromRunnableErrorsDisposed() {
        final AtomicInteger calls = new AtomicInteger();
        Completable.fromRunnable(new Runnable() {
            @Override
            public void run() {
                calls.incrementAndGet();
                throw new TestException();
            }
        })
        .test(true)
        .assertEmpty();

        assertEquals(0, calls.get());
    }

    @Test
    public void disposedUpfront() throws Throwable {
        Runnable run = mock(Runnable.class);

        Completable.fromRunnable(run)
        .test(true)
        .assertEmpty();

        verify(run, never()).run();
    }

    @Test
    public void disposeWhileRunningComplete() {
        TestObserver<Void> to = new TestObserver<>();

        Completable.fromRunnable(() -> {
            to.dispose();
        })
        .subscribeWith(to)
        .assertEmpty();
    }

    @Test
    public void disposeWhileRunningError() throws Throwable {
        TestHelper.withErrorTracking(errors -> {
            TestObserver<Void> to = new TestObserver<>();

            Completable.fromRunnable(() -> {
                to.dispose();
                throw new TestException();
            })
            .subscribeWith(to)
            .assertEmpty();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        });
    }
}
