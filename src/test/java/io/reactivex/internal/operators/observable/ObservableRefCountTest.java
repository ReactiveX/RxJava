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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.ReplaySubject;

public class ObservableRefCountTest {

    @Test
    public void testRefCountAsync() {
        final AtomicInteger subscribeCount = new AtomicInteger();
        final AtomicInteger nextCount = new AtomicInteger();
        Observable<Long> r = Observable.interval(0, 25, TimeUnit.MILLISECONDS)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable s) {
                        subscribeCount.incrementAndGet();
                    }
                })
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long l) {
                        nextCount.incrementAndGet();
                    }
                })
                .publish().refCount();

        final AtomicInteger receivedCount = new AtomicInteger();
        Disposable s1 = r.subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long l) {
                receivedCount.incrementAndGet();
            }
        });

        Disposable s2 = r.subscribe();

        // give time to emit
        try {
            Thread.sleep(260);
        } catch (InterruptedException e) {
        }

        // now unsubscribe
        s2.dispose(); // unsubscribe s2 first as we're counting in 1 and there can be a race between unsubscribe and one Observer getting a value but not the other
        s1.dispose();

        System.out.println("onNext: " + nextCount.get());

        // should emit once for both subscribers
        assertEquals(nextCount.get(), receivedCount.get());
        // only 1 subscribe
        assertEquals(1, subscribeCount.get());
    }

    @Test
    public void testRefCountSynchronous() {
        final AtomicInteger subscribeCount = new AtomicInteger();
        final AtomicInteger nextCount = new AtomicInteger();
        Observable<Integer> r = Observable.just(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable s) {
                        subscribeCount.incrementAndGet();
                    }
                })
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer l) {
                        nextCount.incrementAndGet();
                    }
                })
                .publish().refCount();

        final AtomicInteger receivedCount = new AtomicInteger();
        Disposable s1 = r.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer l) {
                receivedCount.incrementAndGet();
            }
        });

        Disposable s2 = r.subscribe();

        // give time to emit
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }

        // now unsubscribe
        s2.dispose(); // unsubscribe s2 first as we're counting in 1 and there can be a race between unsubscribe and one Observer getting a value but not the other
        s1.dispose();

        System.out.println("onNext Count: " + nextCount.get());

        // it will emit twice because it is synchronous
        assertEquals(nextCount.get(), receivedCount.get() * 2);
        // it will subscribe twice because it is synchronous
        assertEquals(2, subscribeCount.get());
    }

    @Test
    public void testRefCountSynchronousTake() {
        final AtomicInteger nextCount = new AtomicInteger();
        Observable<Integer> r = Observable.just(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer l) {
                            System.out.println("onNext --------> " + l);
                            nextCount.incrementAndGet();
                    }
                })
                .take(4)
                .publish().refCount();

        final AtomicInteger receivedCount = new AtomicInteger();
        r.subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer l) {
                receivedCount.incrementAndGet();
            }
        });

        System.out.println("onNext: " + nextCount.get());

        assertEquals(4, receivedCount.get());
        assertEquals(4, receivedCount.get());
    }

    @Test
    public void testRepeat() {
        final AtomicInteger subscribeCount = new AtomicInteger();
        final AtomicInteger unsubscribeCount = new AtomicInteger();
        Observable<Long> r = Observable.interval(0, 1, TimeUnit.MILLISECONDS)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable s) {
                            System.out.println("******************************* Subscribe received");
                            // when we are subscribed
                            subscribeCount.incrementAndGet();
                    }
                })
                .doOnDispose(new Action() {
                    @Override
                    public void run() {
                            System.out.println("******************************* Unsubscribe received");
                            // when we are unsubscribed
                            unsubscribeCount.incrementAndGet();
                    }
                })
                .publish().refCount();

        for (int i = 0; i < 10; i++) {
            TestObserver<Long> ts1 = new TestObserver<Long>();
            TestObserver<Long> ts2 = new TestObserver<Long>();
            r.subscribe(ts1);
            r.subscribe(ts2);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            ts1.dispose();
            ts2.dispose();
            ts1.assertNoErrors();
            ts2.assertNoErrors();
            assertTrue(ts1.valueCount() > 0);
            assertTrue(ts2.valueCount() > 0);
        }

        assertEquals(10, subscribeCount.get());
        assertEquals(10, unsubscribeCount.get());
    }

    @Test
    public void testConnectUnsubscribe() throws InterruptedException {
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final CountDownLatch subscribeLatch = new CountDownLatch(1);

        Observable<Long> o = synchronousInterval()
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable s) {
                            System.out.println("******************************* Subscribe received");
                            // when we are subscribed
                            subscribeLatch.countDown();
                    }
                })
                .doOnDispose(new Action() {
                    @Override
                    public void run() {
                            System.out.println("******************************* Unsubscribe received");
                            // when we are unsubscribed
                            unsubscribeLatch.countDown();
                    }
                });

        TestObserver<Long> s = new TestObserver<Long>();
        o.publish().refCount().subscribeOn(Schedulers.newThread()).subscribe(s);
        System.out.println("send unsubscribe");
        // wait until connected
        subscribeLatch.await();
        // now unsubscribe
        s.dispose();
        System.out.println("DONE sending unsubscribe ... now waiting");
        if (!unsubscribeLatch.await(3000, TimeUnit.MILLISECONDS)) {
            System.out.println("Errors: " + s.errors());
            if (s.errors().size() > 0) {
                s.errors().get(0).printStackTrace();
            }
            fail("timed out waiting for unsubscribe");
        }
        s.assertNoErrors();
    }

    @Test
    public void testConnectUnsubscribeRaceConditionLoop() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            testConnectUnsubscribeRaceCondition();
        }
    }

    @Test
    public void testConnectUnsubscribeRaceCondition() throws InterruptedException {
        final AtomicInteger subUnsubCount = new AtomicInteger();
        Observable<Long> o = synchronousInterval()
                .doOnDispose(new Action() {
                    @Override
                    public void run() {
                            System.out.println("******************************* Unsubscribe received");
                            // when we are unsubscribed
                            subUnsubCount.decrementAndGet();
                    }
                })
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable s) {
                            System.out.println("******************************* SUBSCRIBE received");
                            subUnsubCount.incrementAndGet();
                    }
                });

        TestObserver<Long> s = new TestObserver<Long>();

        o.publish().refCount().subscribeOn(Schedulers.computation()).subscribe(s);
        System.out.println("send unsubscribe");
        // now immediately unsubscribe while subscribeOn is racing to subscribe
        s.dispose();

        // this generally will mean it won't even subscribe as it is already unsubscribed by the time connect() gets scheduled
        // give time to the counter to update
        Thread.sleep(10);

        // make sure we wait a bit in case the counter is still nonzero
        int counter = 200;
        while (subUnsubCount.get() != 0 && counter-- != 0) {
            Thread.sleep(10);
        }
        // either we subscribed and then unsubscribed, or we didn't ever even subscribe
        assertEquals(0, subUnsubCount.get());

        System.out.println("DONE sending unsubscribe ... now waiting");
        System.out.println("Errors: " + s.errors());
        if (s.errors().size() > 0) {
            s.errors().get(0).printStackTrace();
        }
        s.assertNoErrors();
    }

    private Observable<Long> synchronousInterval() {
        return Observable.unsafeCreate(new ObservableSource<Long>() {
            @Override
            public void subscribe(Observer<? super Long> observer) {
                final AtomicBoolean cancel = new AtomicBoolean();
                observer.onSubscribe(Disposables.fromRunnable(new Runnable() {
                    @Override
                    public void run() {
                        cancel.set(true);
                    }
                }));
                for (;;) {
                    if (cancel.get()) {
                        break;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    observer.onNext(1L);
                }
            }
        });
    }

    @Test
    public void onlyFirstShouldSubscribeAndLastUnsubscribe() {
        final AtomicInteger subscriptionCount = new AtomicInteger();
        final AtomicInteger unsubscriptionCount = new AtomicInteger();
        Observable<Integer> o = Observable.unsafeCreate(new ObservableSource<Integer>() {
            @Override
            public void subscribe(Observer<? super Integer> observer) {
                subscriptionCount.incrementAndGet();
                observer.onSubscribe(Disposables.fromRunnable(new Runnable() {
                    @Override
                    public void run() {
                            unsubscriptionCount.incrementAndGet();
                    }
                }));
            }
        });
        Observable<Integer> refCounted = o.publish().refCount();

        Disposable first = refCounted.subscribe();
        assertEquals(1, subscriptionCount.get());

        Disposable second = refCounted.subscribe();
        assertEquals(1, subscriptionCount.get());

        first.dispose();
        assertEquals(0, unsubscriptionCount.get());

        second.dispose();
        assertEquals(1, unsubscriptionCount.get());
    }

    @Test
    public void testRefCount() {
        TestScheduler s = new TestScheduler();
        Observable<Long> interval = Observable.interval(100, TimeUnit.MILLISECONDS, s).publish().refCount();

        // subscribe list1
        final List<Long> list1 = new ArrayList<Long>();
        Disposable s1 = interval.subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long t1) {
                list1.add(t1);
            }
        });

        s.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        assertEquals(2, list1.size());
        assertEquals(0L, list1.get(0).longValue());
        assertEquals(1L, list1.get(1).longValue());

        // subscribe list2
        final List<Long> list2 = new ArrayList<Long>();
        Disposable s2 = interval.subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long t1) {
                list2.add(t1);
            }
        });

        s.advanceTimeBy(300, TimeUnit.MILLISECONDS);

        // list 1 should have 5 items
        assertEquals(5, list1.size());
        assertEquals(2L, list1.get(2).longValue());
        assertEquals(3L, list1.get(3).longValue());
        assertEquals(4L, list1.get(4).longValue());

        // list 2 should only have 3 items
        assertEquals(3, list2.size());
        assertEquals(2L, list2.get(0).longValue());
        assertEquals(3L, list2.get(1).longValue());
        assertEquals(4L, list2.get(2).longValue());

        // unsubscribe list1
        s1.dispose();

        // advance further
        s.advanceTimeBy(300, TimeUnit.MILLISECONDS);

        // list 1 should still have 5 items
        assertEquals(5, list1.size());

        // list 2 should have 6 items
        assertEquals(6, list2.size());
        assertEquals(5L, list2.get(3).longValue());
        assertEquals(6L, list2.get(4).longValue());
        assertEquals(7L, list2.get(5).longValue());

        // unsubscribe list2
        s2.dispose();

        // advance further
        s.advanceTimeBy(1000, TimeUnit.MILLISECONDS);

        // subscribing a new one should start over because the source should have been unsubscribed
        // subscribe list3
        final List<Long> list3 = new ArrayList<Long>();
        interval.subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long t1) {
                list3.add(t1);
            }
        });

        s.advanceTimeBy(200, TimeUnit.MILLISECONDS);

        assertEquals(2, list3.size());
        assertEquals(0L, list3.get(0).longValue());
        assertEquals(1L, list3.get(1).longValue());

    }

    @Test
    public void testAlreadyUnsubscribedClient() {
        Observer<Integer> done = DisposingObserver.INSTANCE;

        Observer<Integer> o = TestHelper.mockObserver();

        Observable<Integer> result = Observable.just(1).publish().refCount();

        result.subscribe(done);

        result.subscribe(o);

        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testAlreadyUnsubscribedInterleavesWithClient() {
        ReplaySubject<Integer> source = ReplaySubject.create();

        Observer<Integer> done = DisposingObserver.INSTANCE;

        Observer<Integer> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        Observable<Integer> result = source.publish().refCount();

        result.subscribe(o);

        source.onNext(1);

        result.subscribe(done);

        source.onNext(2);
        source.onComplete();

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testConnectDisconnectConnectAndSubjectState() {
        Observable<Integer> o1 = Observable.just(10);
        Observable<Integer> o2 = Observable.just(20);
        Observable<Integer> combined = Observable.combineLatest(o1, o2, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 + t2;
            }
        })
        .publish().refCount();

        TestObserver<Integer> ts1 = new TestObserver<Integer>();
        TestObserver<Integer> ts2 = new TestObserver<Integer>();

        combined.subscribe(ts1);
        combined.subscribe(ts2);

        ts1.assertTerminated();
        ts1.assertNoErrors();
        ts1.assertValue(30);

        ts2.assertTerminated();
        ts2.assertNoErrors();
        ts2.assertValue(30);
    }

    @Test(timeout = 10000)
    public void testUpstreamErrorAllowsRetry() throws InterruptedException {
        final AtomicInteger intervalSubscribed = new AtomicInteger();
        Observable<String> interval =
                Observable.interval(200,TimeUnit.MILLISECONDS)
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable s) {
                                            System.out.println("Subscribing to interval " + intervalSubscribed.incrementAndGet());
                                    }
                        }
                         )
                        .flatMap(new Function<Long, Observable<String>>() {
                            @Override
                            public Observable<String> apply(Long t1) {
                                    return Observable.defer(new Callable<Observable<String>>() {
                                        @Override
                                        public Observable<String> call() {
                                                return Observable.<String>error(new Exception("Some exception"));
                                        }
                                    });
                            }
                        })
                        .onErrorResumeNext(new Function<Throwable, Observable<String>>() {
                            @Override
                            public Observable<String> apply(Throwable t1) {
                                    return Observable.<String>error(t1);
                            }
                        })
                        .publish()
                        .refCount();

        interval
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable t1) {
                            System.out.println("Observer 1 onError: " + t1);
                    }
                })
                .retry(5)
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String t1) {
                            System.out.println("Observer 1: " + t1);
                    }
                });
        Thread.sleep(100);
        interval
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable t1) {
                    System.out.println("Observer 2 onError: " + t1);
            }
        })
        .retry(5)
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String t1) {
                            System.out.println("Observer 2: " + t1);
                    }
                });

        Thread.sleep(1300);

        System.out.println(intervalSubscribed.get());
        assertEquals(6, intervalSubscribed.get());
    }

    private enum DisposingObserver implements Observer<Integer> {
        INSTANCE;

        @Override
        public void onSubscribe(Disposable d) {
            d.dispose();
        }

        @Override
        public void onNext(Integer t) {
        }

        @Override
        public void onError(Throwable t) {
        }

        @Override
        public void onComplete() {
        }
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Observable.just(1).publish().refCount());
    }

    @Test
    public void noOpConnect() {
        final int[] calls = { 0 };
        Observable<Integer> o = new ConnectableObservable<Integer>() {
            @Override
            public void connect(Consumer<? super Disposable> connection) {
                calls[0]++;
            }

            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                observer.onSubscribe(Disposables.disposed());
            }
        }.refCount();

        o.test();
        o.test();

        assertEquals(1, calls[0]);
    }
    Observable<Object> source;

    @Test
    public void replayNoLeak() throws Exception {
        System.gc();
        Thread.sleep(100);

        long start = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source = Observable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return new byte[100 * 1000 * 1000];
            }
        })
        .replay(1)
        .refCount();

        source.subscribe();

        System.gc();
        Thread.sleep(100);

        long after = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source = null;
        assertTrue(String.format("%,3d -> %,3d%n", start, after), start + 20 * 1000 * 1000 > after);
    }

    @Test
    public void replayNoLeak2() throws Exception {
        System.gc();
        Thread.sleep(100);

        long start = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source = Observable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return new byte[100 * 1000 * 1000];
            }
        }).concatWith(Observable.never())
        .replay(1)
        .refCount();

        Disposable s1 = source.subscribe();
        Disposable s2 = source.subscribe();

        s1.dispose();
        s2.dispose();

        s1 = null;
        s2 = null;

        System.gc();
        Thread.sleep(100);

        long after = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source = null;
        assertTrue(String.format("%,3d -> %,3d%n", start, after), start + 20 * 1000 * 1000 > after);
    }

    static final class ExceptionData extends Exception {
        private static final long serialVersionUID = -6763898015338136119L;

        public final Object data;

        ExceptionData(Object data) {
            this.data = data;
        }
    }

    @Test
    public void publishNoLeak() throws Exception {
        System.gc();
        Thread.sleep(100);

        long start = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source = Observable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new ExceptionData(new byte[100 * 1000 * 1000]);
            }
        })
        .publish()
        .refCount();

        source.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer());

        System.gc();
        Thread.sleep(100);

        long after = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source = null;
        assertTrue(String.format("%,3d -> %,3d%n", start, after), start + 20 * 1000 * 1000 > after);
    }

    @Test
    public void publishNoLeak2() throws Exception {
        System.gc();
        Thread.sleep(100);

        long start = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source = Observable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return new byte[100 * 1000 * 1000];
            }
        }).concatWith(Observable.never())
        .publish()
        .refCount();

        Disposable s1 = source.test();
        Disposable s2 = source.test();

        s1.dispose();
        s2.dispose();

        s1 = null;
        s2 = null;

        System.gc();
        Thread.sleep(100);

        long after = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

        source = null;
        assertTrue(String.format("%,3d -> %,3d%n", start, after), start + 20 * 1000 * 1000 > after);
    }

    @Test
    public void replayIsUnsubscribed() {
        ConnectableObservable<Integer> co = Observable.just(1).concatWith(Observable.<Integer>never())
        .replay();

        assertTrue(((Disposable)co).isDisposed());

        Disposable s = co.connect();

        assertFalse(((Disposable)co).isDisposed());

        s.dispose();

        assertTrue(((Disposable)co).isDisposed());
    }
}
