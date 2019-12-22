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

package io.reactivex.rxjava3.internal.operators.completable;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class CompletableAndThenCompletableabTest extends RxJavaTest {
    @Test(expected = NullPointerException.class)
    public void andThenCompletableCompleteNull() {
        Completable.complete()
                .andThen((Completable) null);
    }

    @Test
    public void andThenCompletableCompleteComplete() {
        Completable.complete()
                .andThen(Completable.complete())
                .test()
                .assertComplete();
    }

    @Test
    public void andThenCompletableCompleteError() {
        Completable.complete()
                .andThen(Completable.error(new TestException("test")))
                .to(TestHelper.testConsumer())
                .assertNotComplete()
                .assertNoValues()
                .assertError(TestException.class)
                .assertErrorMessage("test");
    }

    @Test
    public void andThenCompletableCompleteNever() {
        Completable.complete()
                .andThen(Completable.never())
                .test()
                .assertNoValues()
                .assertNoErrors()
                .assertNotComplete();
    }

    @Test
    public void andThenCompletableErrorComplete() {
        Completable.error(new TestException("bla"))
                .andThen(Completable.complete())
                .to(TestHelper.testConsumer())
                .assertNotComplete()
                .assertNoValues()
                .assertError(TestException.class)
                .assertErrorMessage("bla");
    }

    @Test
    public void andThenCompletableErrorNever() {
        Completable.error(new TestException("bla"))
                .andThen(Completable.never())
                .to(TestHelper.testConsumer())
                .assertNotComplete()
                .assertNoValues()
                .assertError(TestException.class)
                .assertErrorMessage("bla");
    }

    @Test
    public void andThenCompletableErrorError() {
        Completable.error(new TestException("error1"))
                .andThen(Completable.error(new TestException("error2")))
                .to(TestHelper.testConsumer())
                .assertNotComplete()
                .assertNoValues()
                .assertError(TestException.class)
                .assertErrorMessage("error1");
    }

    @Test
    public void andThenCanceled() {
        final AtomicInteger completableRunCount = new AtomicInteger();
        Completable.fromRunnable(new Runnable() {
            @Override
            public void run() {
                completableRunCount.incrementAndGet();
            }
        })
                .andThen(Completable.complete())
                .test(true)
                .assertEmpty();
        assertEquals(1, completableRunCount.get());
    }

    @Test
    public void andThenFirstCancels() {
        final TestObserver<Void> to = new TestObserver<>();
        Completable.fromRunnable(new Runnable() {
            @Override
            public void run() {
                to.dispose();
            }
        })
                .andThen(Completable.complete())
                .subscribe(to);
        to
                .assertNotComplete()
                .assertNoErrors();
    }

    @Test
    public void andThenSecondCancels() {
        final TestObserver<Void> to = new TestObserver<>();
        Completable.complete()
                .andThen(Completable.fromRunnable(new Runnable() {
                    @Override
                    public void run() {
                        to.dispose();
                    }
                }))
                .subscribe(to);
        to
                .assertNotComplete()
                .assertNoErrors();
    }

    @Test
    public void andThenDisposed() {
        TestHelper.checkDisposed(Completable.complete()
                .andThen(Completable.complete()));
    }

    @Test
    public void andThenNoInterrupt() throws InterruptedException {
        for (int k = 0; k < 100; k++) {
            final int count = 10;
            final CountDownLatch latch = new CountDownLatch(count);
            final boolean[] interrupted = {false};

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
