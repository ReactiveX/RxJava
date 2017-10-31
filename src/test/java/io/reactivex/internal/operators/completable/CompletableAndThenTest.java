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
import io.reactivex.Maybe;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompletableAndThenTest {
    @Test(expected = NullPointerException.class)
    public void andThenMaybeNull() {
        Completable.complete()
            .andThen((Maybe<Object>) null);
    }

    @Test
    public void andThenMaybeCompleteValue() {
        Completable.complete()
            .andThen(Maybe.just(1))
            .test()
            .assertResult(1);
    }

    @Test
    public void andThenMaybeCompleteError() {
        Completable.complete()
            .andThen(Maybe.error(new RuntimeException("test")))
            .test()
            .assertNotComplete()
            .assertNoValues()
            .assertError(RuntimeException.class)
            .assertErrorMessage("test");
    }

    @Test
    public void andThenMaybeCompleteEmpty() {
        Completable.complete()
            .andThen(Maybe.empty())
            .test()
            .assertNoValues()
            .assertNoErrors()
            .assertComplete();
    }

    @Test
    public void andThenMaybeError() {
        Completable.error(new RuntimeException("bla"))
            .andThen(Maybe.empty())
            .test()
            .assertNotComplete()
            .assertNoValues()
            .assertError(RuntimeException.class)
            .assertErrorMessage("bla");
    }

    @Test
    public void andThenNoInterrupt() throws InterruptedException {
        for (int k = 0; k < 100; k++) {
            final int count = 10;
            final CountDownLatch latch = new CountDownLatch(count);
            final boolean[] interrupted = { false };

            for (int i = 0; i < count; i++) {
                Completable.complete()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .andThen(Completable.fromAction(new Action() {
                    @Override
                    public void run() throws Exception {
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException e) {
                            System.out.println("Interrupted! " + Thread.currentThread());
                            interrupted[0] = true;
                        }
                    }
                }))
                .subscribe(new Action() {
                    @Override
                    public void run() throws Exception {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            assertFalse("The second Completable was interrupted!", interrupted[0]);
        }
    }
}
