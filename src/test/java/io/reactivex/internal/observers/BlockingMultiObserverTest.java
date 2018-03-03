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

package io.reactivex.internal.observers;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.schedulers.Schedulers;

public class BlockingMultiObserverTest {

    @Test
    public void dispose() {
        BlockingMultiObserver<Integer> bmo = new BlockingMultiObserver<Integer>();
        bmo.dispose();

        Disposable d = Disposables.empty();

        bmo.onSubscribe(d);
    }

    @Test
    public void blockingGetDefault() {
        final BlockingMultiObserver<Integer> bmo = new BlockingMultiObserver<Integer>();

        Schedulers.single().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                bmo.onSuccess(1);
            }
        }, 100, TimeUnit.MILLISECONDS);

        assertEquals(1, bmo.blockingGet(0).intValue());
    }

    @Test
    public void blockingAwait() {
        final BlockingMultiObserver<Integer> bmo = new BlockingMultiObserver<Integer>();

        Schedulers.single().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                bmo.onSuccess(1);
            }
        }, 100, TimeUnit.MILLISECONDS);

        assertTrue(bmo.blockingAwait(1, TimeUnit.MINUTES));
    }

    @Test
    public void blockingGetDefaultInterrupt() {
        final BlockingMultiObserver<Integer> bmo = new BlockingMultiObserver<Integer>();

        Thread.currentThread().interrupt();
        try {
            bmo.blockingGet(0);
            fail("Should have thrown");
        } catch (RuntimeException ex) {
            assertTrue(ex.getCause() instanceof InterruptedException);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void blockingGetErrorInterrupt() {
        final BlockingMultiObserver<Integer> bmo = new BlockingMultiObserver<Integer>();

        Thread.currentThread().interrupt();
        try {
            assertTrue(bmo.blockingGetError() instanceof InterruptedException);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void blockingGetErrorTimeoutInterrupt() {
        final BlockingMultiObserver<Integer> bmo = new BlockingMultiObserver<Integer>();

        Thread.currentThread().interrupt();
        try {
            bmo.blockingGetError(1, TimeUnit.MINUTES);
            fail("Should have thrown");
        } catch (RuntimeException ex) {
            assertTrue(ex.getCause() instanceof InterruptedException);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void blockingGetErrorDelayed() {
        final BlockingMultiObserver<Integer> bmo = new BlockingMultiObserver<Integer>();

        Schedulers.single().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                bmo.onError(new TestException());
            }
        }, 100, TimeUnit.MILLISECONDS);

        assertTrue(bmo.blockingGetError() instanceof TestException);
    }

    @Test
    public void blockingGetErrorTimeoutDelayed() {
        final BlockingMultiObserver<Integer> bmo = new BlockingMultiObserver<Integer>();

        Schedulers.single().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                bmo.onError(new TestException());
            }
        }, 100, TimeUnit.MILLISECONDS);

        assertTrue(bmo.blockingGetError(1, TimeUnit.MINUTES) instanceof TestException);
    }
}
