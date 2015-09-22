/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.nbp.NbpPublishSubject;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorTimeoutWithSelectorTest {
    @Test(timeout = 2000)
    public void testTimeoutSelectorNormal1() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> timeout = NbpPublishSubject.create();

        Function<Integer, NbpObservable<Integer>> timeoutFunc = new Function<Integer, NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> apply(Integer t1) {
                return timeout;
            }
        };

        Supplier<NbpObservable<Integer>> firstTimeoutFunc = new Supplier<NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> get() {
                return timeout;
            }
        };

        NbpObservable<Integer> other = NbpObservable.fromIterable(Arrays.asList(100));

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
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
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testTimeoutSelectorTimeoutFirst() throws InterruptedException {
        NbpObservable<Integer> source = NbpObservable.<Integer>never();
        final NbpPublishSubject<Integer> timeout = NbpPublishSubject.create();

        Function<Integer, NbpObservable<Integer>> timeoutFunc = new Function<Integer, NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> apply(Integer t1) {
                return timeout;
            }
        };

        Supplier<NbpObservable<Integer>> firstTimeoutFunc = new Supplier<NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> get() {
                return timeout;
            }
        };

        NbpObservable<Integer> other = NbpObservable.fromIterable(Arrays.asList(100));

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);
        
        timeout.onNext(1);
        
        inOrder.verify(o).onNext(100);
        inOrder.verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testTimeoutSelectorFirstThrows() {
        NbpObservable<Integer> source = NbpObservable.<Integer>never();
        final NbpPublishSubject<Integer> timeout = NbpPublishSubject.create();

        Function<Integer, NbpObservable<Integer>> timeoutFunc = new Function<Integer, NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> apply(Integer t1) {
                return timeout;
            }
        };

        Supplier<NbpObservable<Integer>> firstTimeoutFunc = new Supplier<NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> get() {
                throw new TestException();
            }
        };

        NbpObservable<Integer> other = NbpObservable.fromIterable(Arrays.asList(100));

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();

    }

    @Test
    public void testTimeoutSelectorSubsequentThrows() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> timeout = NbpPublishSubject.create();

        Function<Integer, NbpObservable<Integer>> timeoutFunc = new Function<Integer, NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> apply(Integer t1) {
                throw new TestException();
            }
        };

        Supplier<NbpObservable<Integer>> firstTimeoutFunc = new Supplier<NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> get() {
                return timeout;
            }
        };

        NbpObservable<Integer> other = NbpObservable.fromIterable(Arrays.asList(100));

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);

        source.onNext(1);

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();

    }

    @Test
    public void testTimeoutSelectorFirstObservableThrows() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> timeout = NbpPublishSubject.create();

        Function<Integer, NbpObservable<Integer>> timeoutFunc = new Function<Integer, NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> apply(Integer t1) {
                return timeout;
            }
        };

        Supplier<NbpObservable<Integer>> firstTimeoutFunc = new Supplier<NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> get() {
                return NbpObservable.<Integer> error(new TestException());
            }
        };

        NbpObservable<Integer> other = NbpObservable.fromIterable(Arrays.asList(100));

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();

    }

    @Test
    public void testTimeoutSelectorSubsequentObservableThrows() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> timeout = NbpPublishSubject.create();

        Function<Integer, NbpObservable<Integer>> timeoutFunc = new Function<Integer, NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> apply(Integer t1) {
                return NbpObservable.<Integer> error(new TestException());
            }
        };

        Supplier<NbpObservable<Integer>> firstTimeoutFunc = new Supplier<NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> get() {
                return timeout;
            }
        };

        NbpObservable<Integer> other = NbpObservable.fromIterable(Arrays.asList(100));

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);

        source.onNext(1);

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();

    }

    @Test
    public void testTimeoutSelectorWithFirstTimeoutFirstAndNoOtherObservable() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> timeout = NbpPublishSubject.create();

        Supplier<NbpObservable<Integer>> firstTimeoutFunc = new Supplier<NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> get() {
                return timeout;
            }
        };

        Function<Integer, NbpObservable<Integer>> timeoutFunc = new Function<Integer, NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> apply(Integer t1) {
                return NbpPublishSubject.create();
            }
        };

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        source.timeout(firstTimeoutFunc, timeoutFunc).subscribe(o);

        timeout.onNext(1);

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onError(isA(TimeoutException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTimeoutSelectorWithTimeoutFirstAndNoOtherObservable() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> timeout = NbpPublishSubject.create();

        Supplier<NbpObservable<Integer>> firstTimeoutFunc = new Supplier<NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> get() {
                return NbpPublishSubject.create();
            }
        };

        Function<Integer, NbpObservable<Integer>> timeoutFunc = new Function<Integer, NbpObservable<Integer>>() {
            @Override
            public NbpObservable<Integer> apply(Integer t1) {
                return timeout;
            }
        };

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
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
        // NbpObserver.onNext(1)
        // start timeout
        // unsubscribe timeout in thread 2          start to do some long-time work in "unsubscribe"
        // NbpObserver.onNext(2)
        // timeout.onNext(1)
        //                                          "unsubscribe" done
        //
        //
        // In the above case, the timeout operator should ignore "timeout.onNext(1)"
        // since "NbpObserver" has already seen 2.
        final CountDownLatch observerReceivedTwo = new CountDownLatch(1);
        final CountDownLatch timeoutEmittedOne = new CountDownLatch(1);
        final CountDownLatch observerCompleted = new CountDownLatch(1);
        final CountDownLatch enteredTimeoutOne = new CountDownLatch(1);
        final AtomicBoolean latchTimeout = new AtomicBoolean(false);

        final Function<Integer, NbpObservable<Integer>> timeoutFunc = t1 -> {
            if (t1 == 1) {
                // Force "unsubscribe" run on another thread
                return NbpObservable.create(new NbpOnSubscribe<Integer>() {
                    @Override
                    public void accept(NbpSubscriber<? super Integer> NbpSubscriber) {
                        NbpSubscriber.onSubscribe(EmptyDisposable.INSTANCE);
                        enteredTimeoutOne.countDown();
                        // force the timeout message be sent after NbpObserver.onNext(2)
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
                        NbpSubscriber.onNext(1);
                        timeoutEmittedOne.countDown();
                    }
                }).subscribeOn(Schedulers.newThread());
            } else {
                return NbpPublishSubject.create();
            }
        };

        final NbpSubscriber<Integer> o = TestHelper.mockNbpSubscriber();
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

        }).when(o).onComplete();

        final NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>(o);

        new Thread(new Runnable() {

            @Override
            public void run() {
                NbpPublishSubject<Integer> source = NbpPublishSubject.create();
                source.timeout(timeoutFunc, NbpObservable.just(3)).subscribe(ts);
                source.onNext(1); // start timeout
                try {
                    if(!enteredTimeoutOne.await(30, TimeUnit.SECONDS)) {
                        latchTimeout.set(true);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                source.onNext(2); // disable timeout
                try {
                    if(!timeoutEmittedOne.await(30, TimeUnit.SECONDS)) {
                        latchTimeout.set(true);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                source.onComplete();
            }

        }).start();

        if(!observerCompleted.await(30, TimeUnit.SECONDS)) {
            latchTimeout.set(true);
        }

        assertFalse("CoundDownLatch timeout", latchTimeout.get());

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onSubscribe((Disposable)notNull());
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o, never()).onNext(3);
        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();
    }
}