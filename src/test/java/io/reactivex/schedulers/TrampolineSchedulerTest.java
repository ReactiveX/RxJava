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

package io.reactivex.schedulers;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.Scheduler.Worker;
import io.reactivex.TestHelper;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.subscribers.TestSubscriber;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TrampolineSchedulerTest extends AbstractSchedulerTests {

    @Override
    protected Scheduler getScheduler() {
        return Schedulers.trampoline();
    }

    @Test
    public final void testMergeWithCurrentThreadScheduler1() {

        final String currentThreadName = Thread.currentThread().getName();

        Flowable<Integer> o1 = Flowable.<Integer> just(1, 2, 3, 4, 5);
        Flowable<Integer> o2 = Flowable.<Integer> just(6, 7, 8, 9, 10);
        Flowable<String> o = Flowable.<Integer> merge(o1, o2).subscribeOn(Schedulers.trampoline()).map(new Function<Integer, String>() {

            @Override
            public String apply(Integer t) {
                assertTrue(Thread.currentThread().getName().equals(currentThreadName));
                return "Value_" + t + "_Thread_" + Thread.currentThread().getName();
            }
        });

        o.blockingForEach(new Consumer<String>() {

            @Override
            public void accept(String t) {
                System.out.println("t: " + t);
            }
        });
    }

    @Test
    public void testNestedTrampolineWithUnsubscribe() {
        final ArrayList<String> workDone = new ArrayList<String>();
        final CompositeDisposable workers = new CompositeDisposable();
        Worker worker = Schedulers.trampoline().createWorker();
        try {
            workers.add(worker);
            worker.schedule(new Runnable() {

                @Override
                public void run() {
                    workers.add(doWorkOnNewTrampoline("A", workDone));
                }

            });

            final Worker worker2 = Schedulers.trampoline().createWorker();
            workers.add(worker2);
            worker2.schedule(new Runnable() {

                @Override
                public void run() {
                    workers.add(doWorkOnNewTrampoline("B", workDone));
                    // we unsubscribe worker2 ... it should not affect work scheduled on a separate Trampline.Worker
                    worker2.dispose();
                }

            });

            assertEquals(6, workDone.size());
            assertEquals(Arrays.asList("A.1", "A.B.1", "A.B.2", "B.1", "B.B.1", "B.B.2"), workDone);
        } finally {
            workers.dispose();
        }
    }

    /**
     * This is a regression test for #1702. Concurrent work scheduling that is improperly synchronized can cause an
     * action to be added or removed onto the priority queue during a poll, which can result in NPEs during queue
     * sifting. While it is difficult to isolate the issue directly, we can easily trigger the behavior by spamming the
     * trampoline with enqueue requests from multiple threads concurrently.
     */
    @Test
    public void testTrampolineWorkerHandlesConcurrentScheduling() {
        final Worker trampolineWorker = Schedulers.trampoline().createWorker();
        final Subscriber<Object> observer = TestHelper.mockSubscriber();
        final TestSubscriber<Disposable> ts = new TestSubscriber<Disposable>(observer);

        // Spam the trampoline with actions.
        Flowable.range(0, 50)
                .flatMap(new Function<Integer, Publisher<Disposable>>() {
                    @Override
                    public Publisher<Disposable> apply(Integer count) {
                        return Flowable
                                .interval(1, TimeUnit.MICROSECONDS)
                                .map(new Function<Long, Disposable>() {
                                    @Override
                                    public Disposable apply(Long ount1) {
                                        return trampolineWorker.schedule(Functions.EMPTY_RUNNABLE);
                                    }
                                }).take(100);
                    }
                })
                .subscribeOn(Schedulers.computation())
                .subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
    }

    private static Worker doWorkOnNewTrampoline(final String key, final ArrayList<String> workDone) {
        Worker worker = Schedulers.trampoline().createWorker();
        worker.schedule(new Runnable() {

            @Override
            public void run() {
                String msg = key + ".1";
                workDone.add(msg);
                System.out.println(msg);
                Worker worker3 = Schedulers.trampoline().createWorker();
                worker3.schedule(createPrintAction(key + ".B.1", workDone));
                worker3.schedule(createPrintAction(key + ".B.2", workDone));
            }

        });
        return worker;
    }

    private static Runnable createPrintAction(final String message, final ArrayList<String> workDone) {
        return new Runnable() {

            @Override
            public void run() {
                System.out.println(message);
                workDone.add(message);
            }

        };
    }
}
