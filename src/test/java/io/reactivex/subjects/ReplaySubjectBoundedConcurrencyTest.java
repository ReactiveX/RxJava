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

package io.reactivex.subjects;

import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.*;
import io.reactivex.schedulers.Schedulers;

public class ReplaySubjectBoundedConcurrencyTest {

    @Test(timeout = 4000)
    public void testReplaySubjectConcurrentSubscribersDoingReplayDontBlockEachOther() throws InterruptedException {
        final ReplaySubject<Long> replay = ReplaySubject.createUnbounded();
        Thread source = new Thread(new Runnable() {

            @Override
            public void run() {
                Observable.unsafeCreate(new ObservableSource<Long>() {

                    @Override
                    public void subscribe(Observer<? super Long> o) {
                        o.onSubscribe(Disposables.empty());
                        System.out.println("********* Start Source Data ***********");
                        for (long l = 1; l <= 10000; l++) {
                            o.onNext(l);
                        }
                        System.out.println("********* Finished Source Data ***********");
                        o.onComplete();
                    }
                }).subscribe(replay);
            }
        });
        source.start();

        long v = replay.blockingLast();
        assertEquals(10000, v);

        // it's been played through once so now it will all be replays
        final CountDownLatch slowLatch = new CountDownLatch(1);
        Thread slowThread = new Thread(new Runnable() {

            @Override
            public void run() {
                Observer<Long> slow = new DefaultObserver<Long>() {

                    @Override
                    public void onComplete() {
                        System.out.println("*** Slow Observer completed");
                        slowLatch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(Long args) {
                        if (args == 1) {
                            System.out.println("*** Slow Observer STARTED");
                        }
                        try {
                            if (args % 10 == 0) {
                                Thread.sleep(1);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                replay.subscribe(slow);
                try {
                    slowLatch.await();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        });
        slowThread.start();

        Thread fastThread = new Thread(new Runnable() {

            @Override
            public void run() {
                final CountDownLatch fastLatch = new CountDownLatch(1);
                Observer<Long> fast = new DefaultObserver<Long>() {

                    @Override
                    public void onComplete() {
                        System.out.println("*** Fast Observer completed");
                        fastLatch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(Long args) {
                        if (args == 1) {
                            System.out.println("*** Fast Observer STARTED");
                        }
                    }
                };
                replay.subscribe(fast);
                try {
                    fastLatch.await();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        });
        fastThread.start();
        fastThread.join();

        // slow should not yet be completed when fast completes
        assertEquals(1, slowLatch.getCount());

        slowThread.join();
    }

    @Test
    public void testReplaySubjectConcurrentSubscriptions() throws InterruptedException {
        final ReplaySubject<Long> replay = ReplaySubject.createUnbounded();
        Thread source = new Thread(new Runnable() {

            @Override
            public void run() {
                Observable.unsafeCreate(new ObservableSource<Long>() {

                    @Override
                    public void subscribe(Observer<? super Long> o) {
                        o.onSubscribe(Disposables.empty());
                        System.out.println("********* Start Source Data ***********");
                        for (long l = 1; l <= 10000; l++) {
                            o.onNext(l);
                        }
                        System.out.println("********* Finished Source Data ***********");
                        o.onComplete();
                    }
                }).subscribe(replay);
            }
        });

        // used to collect results of each thread
        final List<List<Long>> listOfListsOfValues = Collections.synchronizedList(new ArrayList<List<Long>>());
        final List<Thread> threads = Collections.synchronizedList(new ArrayList<Thread>());

        for (int i = 1; i <= 200; i++) {
            final int count = i;
            if (count == 20) {
                // start source data after we have some already subscribed
                // and while others are in process of subscribing
                source.start();
            }
            if (count == 100) {
                // wait for source to finish then keep adding after it's done
                source.join();
            }
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    List<Long> values = replay.toList().blockingGet();
                    listOfListsOfValues.add(values);
                    System.out.println("Finished thread: " + count);
                }
            });
            t.start();
            System.out.println("Started thread: " + i);
            threads.add(t);
        }

        // wait for all threads to complete
        for (Thread t : threads) {
            t.join();
        }

        // assert all threads got the same results
        List<Long> sums = new ArrayList<Long>();
        for (List<Long> values : listOfListsOfValues) {
            long v = 0;
            for (long l : values) {
                v += l;
            }
            sums.add(v);
        }

        long expected = sums.get(0);
        boolean success = true;
        for (long l : sums) {
            if (l != expected) {
                success = false;
                System.out.println("FAILURE => Expected " + expected + " but got: " + l);
            }
        }

        if (success) {
            System.out.println("Success! " + sums.size() + " each had the same sum of " + expected);
        } else {
            throw new RuntimeException("Concurrency Bug");
        }

    }

    /**
     * Can receive timeout if subscribe never receives an onError/onComplete ... which reveals a race condition.
     */
    @Test(timeout = 10000)
    public void testSubscribeCompletionRaceCondition() {
        for (int i = 0; i < 50; i++) {
            final ReplaySubject<String> subject = ReplaySubject.createUnbounded();
            final AtomicReference<String> value1 = new AtomicReference<String>();

            subject.subscribe(new Consumer<String>() {

                @Override
                public void accept(String t1) {
                    try {
                        // simulate a slow observer
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    value1.set(t1);
                }

            });

            Thread t1 = new Thread(new Runnable() {

                @Override
                public void run() {
                    subject.onNext("value");
                    subject.onComplete();
                }
            });

            SubjectObserverThread t2 = new SubjectObserverThread(subject);
            SubjectObserverThread t3 = new SubjectObserverThread(subject);
            SubjectObserverThread t4 = new SubjectObserverThread(subject);
            SubjectObserverThread t5 = new SubjectObserverThread(subject);

            t2.start();
            t3.start();
            t1.start();
            t4.start();
            t5.start();
            try {
                t1.join();
                t2.join();
                t3.join();
                t4.join();
                t5.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            assertEquals("value", value1.get());
            assertEquals("value", t2.value.get());
            assertEquals("value", t3.value.get());
            assertEquals("value", t4.value.get());
            assertEquals("value", t5.value.get());
        }

    }

    /**
     * https://github.com/ReactiveX/RxJava/issues/1147
     */
    @Test
    public void testRaceForTerminalState() {
        final List<Integer> expected = Arrays.asList(1);
        for (int i = 0; i < 100000; i++) {
            TestObserver<Integer> ts = new TestObserver<Integer>();
            Observable.just(1).subscribeOn(Schedulers.computation()).cache().subscribe(ts);
            ts.awaitTerminalEvent();
            ts.assertValueSequence(expected);
            ts.assertTerminated();
        }
    }

    static class SubjectObserverThread extends Thread {

        private final ReplaySubject<String> subject;
        private final AtomicReference<String> value = new AtomicReference<String>();

        SubjectObserverThread(ReplaySubject<String> subject) {
            this.subject = subject;
        }

        @Override
        public void run() {
            try {
                // a timeout exception will happen if we don't get a terminal state
                String v = subject.timeout(2000, TimeUnit.MILLISECONDS).blockingSingle();
                value.set(v);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Test
    public void testReplaySubjectEmissionSubscriptionRace() throws Exception {
        Scheduler s = Schedulers.io();
        Scheduler.Worker worker = Schedulers.io().createWorker();
        try {
            for (int i = 0; i < 50000; i++) {
                if (i % 1000 == 0) {
                    System.out.println(i);
                }
                final ReplaySubject<Object> rs = ReplaySubject.createWithSize(2);

                final CountDownLatch finish = new CountDownLatch(1);
                final CountDownLatch start = new CountDownLatch(1);

//                int j = i;

                worker.schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            start.await();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
//                        System.out.println("> " + j);
                        rs.onNext(1);
                    }
                });

                final AtomicReference<Object> o = new AtomicReference<Object>();

                rs
//                .doOnSubscribe(v -> System.out.println("!! " + j))
//                .doOnNext(e -> System.out.println(">> " + j))
                .subscribeOn(s)
                .observeOn(Schedulers.io())
//                .doOnNext(e -> System.out.println(">>> " + j))
                .subscribe(new DefaultObserver<Object>() {

                    @Override
                    protected void onStart() {
                        super.onStart();
                    }

                    @Override
                    public void onComplete() {
                        o.set(-1);
                        finish.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        o.set(e);
                        finish.countDown();
                    }

                    @Override
                    public void onNext(Object t) {
                        o.set(t);
                        finish.countDown();
                    }

                });
                start.countDown();

                if (!finish.await(5, TimeUnit.SECONDS)) {
                    System.out.println(o.get());
                    System.out.println(rs.hasObservers());
                    rs.onComplete();
                    Assert.fail("Timeout @ " + i);
                    break;
                } else {
                    Assert.assertEquals(1, o.get());
                    worker.schedule(new Runnable() {
                        @Override
                        public void run() {
                            rs.onComplete();
                        }
                    });
                }
            }
        } finally {
            worker.dispose();
        }
    }
    @Test(timeout = 5000)
    public void testConcurrentSizeAndHasAnyValue() throws InterruptedException {
        final ReplaySubject<Object> rs = ReplaySubject.createUnbounded();
        final CyclicBarrier cb = new CyclicBarrier(2);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    cb.await();
                } catch (InterruptedException e) {
                    return;
                } catch (BrokenBarrierException e) {
                    return;
                }
                for (int i = 0; i < 1000000; i++) {
                    rs.onNext(i);
                }
                rs.onComplete();
                System.out.println("Replay fill Thread finished!");
            }
        });
        t.start();
        try {
            cb.await();
        } catch (InterruptedException e) {
            return;
        } catch (BrokenBarrierException e) {
            return;
        }
        int lastSize = 0;
        for (; !rs.hasThrowable() && !rs.hasComplete();) {
            int size = rs.size();
            boolean hasAny = rs.hasValue();
            Object[] values = rs.getValues();
            if (size < lastSize) {
                Assert.fail("Size decreased! " + lastSize + " -> " + size);
            }
            if ((size > 0) && !hasAny) {
                Assert.fail("hasAnyValue reports emptyness but size doesn't");
            }
            if (size > values.length) {
                Assert.fail("Got fewer values than size! " + size + " -> " + values.length);
            }
            for (int i = 0; i < values.length - 1; i++) {
                Integer v1 = (Integer)values[i];
                Integer v2 = (Integer)values[i + 1];
                assertEquals(1, v2 - v1);
            }
            lastSize = size;
        }

        t.join();
    }
    @Test(timeout = 5000)
    public void testConcurrentSizeAndHasAnyValueBounded() throws InterruptedException {
        final ReplaySubject<Object> rs = ReplaySubject.createWithSize(3);
        final CyclicBarrier cb = new CyclicBarrier(2);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    cb.await();
                } catch (InterruptedException e) {
                    return;
                } catch (BrokenBarrierException e) {
                    return;
                }
                for (int i = 0; i < 1000000; i++) {
                    rs.onNext(i);
                }
                rs.onComplete();
                System.out.println("Replay fill Thread finished!");
            }
        });
        t.start();
        try {
            cb.await();
        } catch (InterruptedException e) {
            return;
        } catch (BrokenBarrierException e) {
            return;
        }
        for (; !rs.hasThrowable() && !rs.hasComplete();) {
            rs.size(); // can't use value so just call to detect hangs
            rs.hasValue(); // can't use value so just call to detect hangs
            Object[] values = rs.getValues();
            for (int i = 0; i < values.length - 1; i++) {
                Integer v1 = (Integer)values[i];
                Integer v2 = (Integer)values[i + 1];
                assertEquals(1, v2 - v1);
            }
        }

        t.join();
    }
    @Test(timeout = 10000)
    public void testConcurrentSizeAndHasAnyValueTimeBounded() throws InterruptedException {
        final ReplaySubject<Object> rs = ReplaySubject.createWithTime(1, TimeUnit.MILLISECONDS, Schedulers.computation());
        final CyclicBarrier cb = new CyclicBarrier(2);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    cb.await();
                } catch (InterruptedException e) {
                    return;
                } catch (BrokenBarrierException e) {
                    return;
                }
                for (int i = 0; i < 1000000; i++) {
                    rs.onNext(i);
                    if (i % 10000 == 0) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
                rs.onComplete();
                System.out.println("Replay fill Thread finished!");
            }
        });
        t.start();
        try {
            cb.await();
        } catch (InterruptedException e) {
            return;
        } catch (BrokenBarrierException e) {
            return;
        }
        for (; !rs.hasThrowable() && !rs.hasComplete();) {
            rs.size(); // can't use value so just call to detect hangs
            rs.hasValue(); // can't use value so just call to detect hangs
            Object[] values = rs.getValues();
            for (int i = 0; i < values.length - 1; i++) {
                Integer v1 = (Integer)values[i];
                Integer v2 = (Integer)values[i + 1];
                assertEquals(1, v2 - v1);
            }
        }

        t.join();
    }
}
