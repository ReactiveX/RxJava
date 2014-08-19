/**
 * Copyright 2014 Netflix, Inc.
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
package rx.subjects;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class ReplaySubjectConcurrencyTest {

    public static void main(String args[]) {
        try {
            for (int i = 0; i < 100; i++) {
                new ReplaySubjectConcurrencyTest().testSubscribeCompletionRaceCondition();
                new ReplaySubjectConcurrencyTest().testReplaySubjectConcurrentSubscriptions();
                new ReplaySubjectConcurrencyTest().testReplaySubjectConcurrentSubscribersDoingReplayDontBlockEachOther();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test(timeout = 4000)
    public void testReplaySubjectConcurrentSubscribersDoingReplayDontBlockEachOther() throws InterruptedException {
        final ReplaySubject<Long> replay = ReplaySubject.create();
        Thread source = new Thread(new Runnable() {

            @Override
            public void run() {
                Observable.create(new OnSubscribe<Long>() {

                    @Override
                    public void call(Subscriber<? super Long> o) {
                        System.out.println("********* Start Source Data ***********");
                        for (long l = 1; l <= 10000; l++) {
                            o.onNext(l);
                        }
                        System.out.println("********* Finished Source Data ***********");
                        o.onCompleted();
                    }
                }).subscribe(replay);
            }
        });
        source.start();

        long v = replay.toBlocking().last();
        assertEquals(10000, v);

        // it's been played through once so now it will all be replays
        final CountDownLatch slowLatch = new CountDownLatch(1);
        Thread slowThread = new Thread(new Runnable() {

            @Override
            public void run() {
                Subscriber<Long> slow = new Subscriber<Long>() {

                    @Override
                    public void onCompleted() {
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
                Subscriber<Long> fast = new Subscriber<Long>() {

                    @Override
                    public void onCompleted() {
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
        final ReplaySubject<Long> replay = ReplaySubject.create();
        Thread source = new Thread(new Runnable() {

            @Override
            public void run() {
                Observable.create(new OnSubscribe<Long>() {

                    @Override
                    public void call(Subscriber<? super Long> o) {
                        System.out.println("********* Start Source Data ***********");
                        for (long l = 1; l <= 10000; l++) {
                            o.onNext(l);
                        }
                        System.out.println("********* Finished Source Data ***********");
                        o.onCompleted();
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
                    List<Long> values = replay.toList().toBlocking().last();
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
     * Can receive timeout if subscribe never receives an onError/onCompleted ... which reveals a race condition.
     */
    @Test(timeout = 10000)
    public void testSubscribeCompletionRaceCondition() {
        for (int i = 0; i < 50; i++) {
            final ReplaySubject<String> subject = ReplaySubject.create();
            final AtomicReference<String> value1 = new AtomicReference<String>();

            subject.subscribe(new Action1<String>() {

                @Override
                public void call(String t1) {
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
                    subject.onCompleted();
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
     * https://github.com/Netflix/RxJava/issues/1147
     */
    @Test
    public void testRaceForTerminalState() {
        final List<Integer> expected = Arrays.asList(1);
        for (int i = 0; i < 100000; i++) {
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            Observable.just(1).subscribeOn(Schedulers.computation()).cache().subscribe(ts);
            ts.awaitTerminalEvent();
            ts.assertReceivedOnNext(expected);
            ts.assertTerminalEvent();
        }
    }

    private static class SubjectObserverThread extends Thread {

        private final ReplaySubject<String> subject;
        private final AtomicReference<String> value = new AtomicReference<String>();

        public SubjectObserverThread(ReplaySubject<String> subject) {
            this.subject = subject;
        }

        @Override
        public void run() {
            try {
                // a timeout exception will happen if we don't get a terminal state 
                String v = subject.timeout(2000, TimeUnit.MILLISECONDS).toBlocking().single();
                value.set(v);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
