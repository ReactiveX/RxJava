/**
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

import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.*;

import rx.*;
import rx.functions.*;
import rx.schedulers.Schedulers;

public class CompletableConcatTest {

    @Test
    public void asyncObservables() {

        final int[] calls = { 0 };

        Completable.concat(Observable.range(1, 5).map(new Func1<Integer, Completable>() {
            @Override
            public Completable call(final Integer v) {
                System.out.println("Mapping " + v);
                return Completable.fromAction(new Action0() {
                    @Override
                    public void call() {
                        System.out.println("Processing " + (calls[0] + 1));
                        calls[0]++;
                    }
                })
                .subscribeOn(Schedulers.io())
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        System.out.println("Inner complete " + v);
                    }
                })
                .observeOn(Schedulers.computation());
            }
        })
        ).test()
        .awaitTerminalEventAndUnsubscribeOnTimeout(5, TimeUnit.SECONDS)
        .assertResult();

        Assert.assertEquals(5, calls[0]);
    }

    @Test
    public void andThenNoInterrupt() throws InterruptedException {
        for (int k = 0; k < 100; k++) {
            final int count = 10;
            final CountDownLatch latch = new CountDownLatch(count);
            final AtomicBoolean interrupted = new AtomicBoolean();

            for (int i = 0; i < count; i++) {
                Completable.complete()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .andThen(Completable.fromAction(new Action0() {
                    @Override
                    public void call() {
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException e) {
                            System.out.println("Interrupted! " + Thread.currentThread());
                            interrupted.set(true);
                        }
                    }
                }))
                .subscribe(new Action0() {
                    @Override
                    public void call() {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            assertFalse("The second Completable was interrupted!", interrupted.get());
        }
    }

    @Test
    public void noInterrupt() throws InterruptedException {
        for (int k = 0; k < 100; k++) {
            final int count = 10;
            final CountDownLatch latch = new CountDownLatch(count);
            final AtomicBoolean interrupted = new AtomicBoolean();

            for (int i = 0; i < count; i++) {
                Completable c0 = Completable.fromAction(new Action0() {
                    @Override
                    public void call() {
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException e) {
                            System.out.println("Interrupted! " + Thread.currentThread());
                            interrupted.set(true);
                        }
                    }
                });
                Completable.concat(Arrays.asList(Completable.complete()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io()),
                    c0)
                )
                .subscribe(new Action0() {
                    @Override
                    public void call() {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            assertFalse("The second Completable was interrupted!", interrupted.get());
        }
    }

}
