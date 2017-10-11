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
package rx.internal.operators;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.exceptions.TestException;
import rx.functions.*;
import rx.observers.*;
import rx.schedulers.*;
import rx.subjects.*;

public class OperatorTimeoutWithSelectorTest {
    @Test(timeout = 2000)
    public void testTimeoutSelectorNormal1() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return timeout;
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return timeout;
            }
        };

        Observable<Integer> other = Observable.from(Arrays.asList(100));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        timeout.onNext(1);

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onNext(100);
        inOrder.verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testTimeoutSelectorTimeoutFirst() throws InterruptedException {
        Observable<Integer> source = Observable.<Integer>never();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return timeout;
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return timeout;
            }
        };

        Observable<Integer> other = Observable.from(Arrays.asList(100));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);

        timeout.onNext(1);

        inOrder.verify(o).onNext(100);
        inOrder.verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testTimeoutSelectorFirstThrows() {
        Observable<Integer> source = Observable.<Integer>never();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return timeout;
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                throw new TestException();
            }
        };

        Observable<Integer> other = Observable.from(Arrays.asList(100));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();

    }

    @Test
    public void testTimeoutSelectorSubsequentThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                throw new TestException();
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return timeout;
            }
        };

        Observable<Integer> other = Observable.from(Arrays.asList(100));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);

        source.onNext(1);

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(any(TestException.class));
        verify(o, never()).onCompleted();

    }

    @Test
    public void testTimeoutSelectorFirstObservableThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return timeout;
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return Observable.<Integer> error(new TestException());
            }
        };

        Observable<Integer> other = Observable.from(Arrays.asList(100));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();

    }

    @Test
    public void testTimeoutSelectorSubsequentObservableThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return Observable.<Integer> error(new TestException());
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return timeout;
            }
        };

        Observable<Integer> other = Observable.from(Arrays.asList(100));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);

        source.onNext(1);

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(any(TestException.class));
        verify(o, never()).onCompleted();

    }

    @Test
    public void testTimeoutSelectorWithFirstTimeoutFirstAndNoOtherObservable() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return timeout;
            }
        };

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return PublishSubject.create();
            }
        };

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        source.timeout(firstTimeoutFunc, timeoutFunc).subscribe(o);

        timeout.onNext(1);

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onError(isA(TimeoutException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTimeoutSelectorWithTimeoutFirstAndNoOtherObservable() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return PublishSubject.create();
            }
        };

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return timeout;
            }
        };

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        source.timeout(firstTimeoutFunc, timeoutFunc).subscribe(o);
        source.onNext(1);

        timeout.onNext(1);

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(isA(TimeoutException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTimeoutSelectorWithTimeoutAndOnNextRaceCondition() throws InterruptedException {
        // Thread 1                                    Thread 2
        //
        // observer.onNext(1)
        // start timeout
        // unsubscribe timeout in thread 2          start to do some long-time work in "unsubscribe"
        // observer.onNext(2)
        // timeout.onNext(1)
        //                                          "unsubscribe" done
        //
        //
        // In the above case, the timeout operator should ignore "timeout.onNext(1)"
        // since "observer" has already seen 2.
        final CountDownLatch observerReceivedTwo = new CountDownLatch(1);
        final CountDownLatch timeoutEmittedOne = new CountDownLatch(1);
        final CountDownLatch observerCompleted = new CountDownLatch(1);
        final CountDownLatch enteredTimeoutOne = new CountDownLatch(1);
        final AtomicBoolean latchTimeout = new AtomicBoolean(false);

        final Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                if (t1 == 1) {
                    // Force "unsubscribe" run on another thread
                    return Observable.unsafeCreate(new OnSubscribe<Integer>() {
                        @Override
                        public void call(Subscriber<? super Integer> subscriber) {
                            enteredTimeoutOne.countDown();
                            // force the timeout message be sent after observer.onNext(2)
                            while (true) {
                                try {
                                    if (!observerReceivedTwo.await(30, TimeUnit.SECONDS)) {
                                        // CountDownLatch timeout
                                        // There should be something wrong
                                        latchTimeout.set(true);
                                    }
                                    break;
                                } catch (InterruptedException e) {
                                    // Since we just want to emulate a busy method,
                                    // we ignore the interrupt signal from Scheduler.
                                }
                            }
                            subscriber.onNext(1);
                            timeoutEmittedOne.countDown();
                        }
                    }).subscribeOn(Schedulers.newThread());
                } else {
                    return PublishSubject.create();
                }
            }
        };

        @SuppressWarnings("unchecked")
        final Observer<Integer> o = mock(Observer.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                observerReceivedTwo.countDown();
                return null;
            }

        }).when(o).onNext(2);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                observerCompleted.countDown();
                return null;
            }

        }).when(o).onCompleted();

        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>(o);

        new Thread(new Runnable() {

            @Override
            public void run() {
                PublishSubject<Integer> source = PublishSubject.create();
                source.timeout(timeoutFunc, Observable.just(3)).subscribe(ts);
                source.onNext(1); // start timeout
                try {
                    if (!enteredTimeoutOne.await(30, TimeUnit.SECONDS)) {
                        latchTimeout.set(true);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                source.onNext(2); // disable timeout
                try {
                    if (!timeoutEmittedOne.await(30, TimeUnit.SECONDS)) {
                        latchTimeout.set(true);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                source.onCompleted();
            }

        }).start();

        if (!observerCompleted.await(30, TimeUnit.SECONDS)) {
            latchTimeout.set(true);
        }

        assertFalse("CountDownLatch timeout", latchTimeout.get());

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o, never()).onNext(3);
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void selectorNull() {
        try {
            Observable.never().timeout(new Func0<Observable<Object>>() {
                @Override
                public Observable<Object> call() {
                    return Observable.never();
                }
            }, null, Observable.empty());
        } catch (NullPointerException ex) {
            assertEquals("timeoutSelector is null", ex.getMessage());
        }
    }

    @Test
    public void disconnectOnTimeout() {
        final List<String> list = Collections.synchronizedList(new ArrayList<String>());

        final TestScheduler sch = new TestScheduler();

        Subject<Long, Long> subject = PublishSubject.create();
        Observable<Long> initialObservable = subject.share()
        .map(new Func1<Long, Long>() {
            @Override
            public Long call(Long value) {
                list.add("Received value " + value);
                return value;
            }
        });

        Observable<Long> timeoutObservable = initialObservable
        .map(new Func1<Long, Long>() {
            @Override
            public Long call(Long value) {
               list.add("Timeout received value " + value);
               return value;
            }
        });

        TestSubscriber<Long> subscriber = new TestSubscriber<Long>();
        initialObservable
        .doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                list.add("Unsubscribed");
            }
        })
        .timeout(
                new Func0<Observable<Long>>() {
                    @Override
                    public Observable<Long> call() {
                        return Observable.timer(1, TimeUnit.SECONDS, sch);
                    }
                },
                new Func1<Long, Observable<Long>>() {
                    @Override
                    public Observable<Long> call(Long v) {
                        return Observable.timer(1, TimeUnit.SECONDS, sch);
                    }
                },
                timeoutObservable).subscribe(subscriber);

        subject.onNext(5L);

        sch.advanceTimeBy(2, TimeUnit.SECONDS);

        subject.onNext(10L);
        subject.onCompleted();

        subscriber.awaitTerminalEvent();
        subscriber.assertNoErrors();
        subscriber.assertValues(5L, 10L);

        assertEquals(Arrays.asList(
                "Received value 5",
                "Unsubscribed",
                "Received value 10",
                "Timeout received value 10"
        ), list);
    }

    @Test
    public void fallbackIsError() {
        final TestScheduler sch = new TestScheduler();

        AssertableSubscriber<Object> as = Observable.never()
                .timeout(new Func0<Observable<Long>>() {
                    @Override
                    public Observable<Long> call() {
                        return Observable.timer(1, TimeUnit.SECONDS, sch);
                    }
                },
                new Func1<Object, Observable<Long>>() {
                    @Override
                    public Observable<Long> call(Object v) {
                        return Observable.timer(1, TimeUnit.SECONDS, sch);
                    }
                }, Observable.error(new TestException()))
        .test();

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        as.assertFailure(TestException.class);
    }

    @Test
    public void mainErrors() {
        final TestScheduler sch = new TestScheduler();

        AssertableSubscriber<Object> as = Observable.error(new IOException())
                .timeout(new Func0<Observable<Long>>() {
                    @Override
                    public Observable<Long> call() {
                        return Observable.timer(1, TimeUnit.SECONDS, sch);
                    }
                },
                new Func1<Object, Observable<Long>>() {
                    @Override
                    public Observable<Long> call(Object v) {
                        return Observable.timer(1, TimeUnit.SECONDS, sch);
                    }
                }, Observable.error(new TestException()))
        .test();

        as.assertFailure(IOException.class);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        as.assertFailure(IOException.class);
    }

    @Test
    public void timeoutCompletesWithFallback() {
        final TestScheduler sch = new TestScheduler();

        AssertableSubscriber<Object> as = Observable.never()
                .timeout(new Func0<Observable<Long>>() {
                    @Override
                    public Observable<Long> call() {
                        return Observable.timer(1, TimeUnit.SECONDS, sch).ignoreElements();
                    }
                },
                new Func1<Object, Observable<Long>>() {
                    @Override
                    public Observable<Long> call(Object v) {
                        return Observable.timer(1, TimeUnit.SECONDS, sch);
                    }
                }, Observable.just(1))
        .test();

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        as.assertResult(1);
    }

    @Test
    public void nullItemTimeout() {
        final TestScheduler sch = new TestScheduler();

        AssertableSubscriber<Integer> as = Observable.just(1).concatWith(Observable.<Integer>never())
                .timeout(new Func0<Observable<Long>>() {
                    @Override
                    public Observable<Long> call() {
                        return Observable.timer(1, TimeUnit.SECONDS, sch).ignoreElements();
                    }
                },
                new Func1<Object, Observable<Long>>() {
                    @Override
                    public Observable<Long> call(Object v) {
                        return null;
                    }
                }, Observable.just(1))
        .test();

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        as.assertFailureAndMessage(NullPointerException.class, "The itemTimeoutIndicator returned a null Observable", 1);
    }
}
