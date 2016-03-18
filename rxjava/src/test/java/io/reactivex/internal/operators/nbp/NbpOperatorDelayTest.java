/**
 * Copyright 2016 Netflix, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.schedulers.*;
import io.reactivex.subjects.nbp.NbpPublishSubject;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;
import io.reactivex.Optional;
import io.reactivex.Observable;

public class NbpOperatorDelayTest {
    private NbpSubscriber<Long> NbpObserver;
    private NbpSubscriber<Long> observer2;

    private TestScheduler scheduler;

    @Before
    public void before() {
        NbpObserver = TestHelper.mockNbpSubscriber();
        observer2 = TestHelper.mockNbpSubscriber();
        
        scheduler = new TestScheduler();
    }

    @Test
    public void testDelay() {
        NbpObservable<Long> source = NbpObservable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        NbpObservable<Long> delayed = source.delay(500L, TimeUnit.MILLISECONDS, scheduler);
        delayed.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(NbpObserver, never()).onNext(anyLong());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext(0L);
        inOrder.verify(NbpObserver, never()).onNext(anyLong());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2400L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(anyLong());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext(1L);
        inOrder.verify(NbpObserver, never()).onNext(anyLong());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3400L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(anyLong());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext(2L);
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testLongDelay() {
        NbpObservable<Long> source = NbpObservable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        NbpObservable<Long> delayed = source.delay(5L, TimeUnit.SECONDS, scheduler);
        delayed.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);

        scheduler.advanceTimeTo(5999L, TimeUnit.MILLISECONDS);
        verify(NbpObserver, never()).onNext(anyLong());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(6000L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext(0L);
        scheduler.advanceTimeTo(6999L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(anyLong());
        scheduler.advanceTimeTo(7000L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext(1L);
        scheduler.advanceTimeTo(7999L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(anyLong());
        scheduler.advanceTimeTo(8000L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext(2L);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verify(NbpObserver, never()).onNext(anyLong());
        inOrder.verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithError() {
        NbpObservable<Long> source = NbpObservable.interval(1L, TimeUnit.SECONDS, scheduler)
        .map(value -> {
            if (value == 1L) {
                throw new RuntimeException("error!");
            }
            return value;
        });
        NbpObservable<Long> delayed = source.delay(1L, TimeUnit.SECONDS, scheduler);
        delayed.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);

        scheduler.advanceTimeTo(1999L, TimeUnit.MILLISECONDS);
        verify(NbpObserver, never()).onNext(anyLong());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2000L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onError(any(Throwable.class));
        inOrder.verify(NbpObserver, never()).onNext(anyLong());
        verify(NbpObserver, never()).onComplete();

        scheduler.advanceTimeTo(5000L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(anyLong());
        inOrder.verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, never()).onComplete();
    }

    @Test
    public void testDelayWithMultipleSubscriptions() {
        NbpObservable<Long> source = NbpObservable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        NbpObservable<Long> delayed = source.delay(500L, TimeUnit.MILLISECONDS, scheduler);
        delayed.subscribe(NbpObserver);
        delayed.subscribe(observer2);

        InOrder inOrder = inOrder(NbpObserver);
        InOrder inOrder2 = inOrder(observer2);

        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(NbpObserver, never()).onNext(anyLong());
        verify(observer2, never()).onNext(anyLong());

        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext(0L);
        inOrder2.verify(observer2, times(1)).onNext(0L);

        scheduler.advanceTimeTo(2499L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(anyLong());
        inOrder2.verify(observer2, never()).onNext(anyLong());

        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext(1L);
        inOrder2.verify(observer2, times(1)).onNext(1L);

        verify(NbpObserver, never()).onComplete();
        verify(observer2, never()).onComplete();

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext(2L);
        inOrder2.verify(observer2, times(1)).onNext(2L);
        inOrder.verify(NbpObserver, never()).onNext(anyLong());
        inOrder2.verify(observer2, never()).onNext(anyLong());
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder2.verify(observer2, times(1)).onComplete();

        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(observer2, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelaySubscription() {
        NbpObservable<Integer> result = NbpObservable.just(1, 2, 3).delaySubscription(100, TimeUnit.MILLISECONDS, scheduler);

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        inOrder.verify(o, never()).onNext(any());
        inOrder.verify(o, never()).onComplete();

        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        inOrder.verify(o, times(1)).onNext(1);
        inOrder.verify(o, times(1)).onNext(2);
        inOrder.verify(o, times(1)).onNext(3);
        inOrder.verify(o, times(1)).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelaySubscriptionCancelBeforeTime() {
        NbpObservable<Integer> result = NbpObservable.just(1, 2, 3).delaySubscription(100, TimeUnit.MILLISECONDS, scheduler);

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        NbpTestSubscriber<Object> ts = new NbpTestSubscriber<>(o);

        result.subscribe(ts);
        ts.dispose();
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);

        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithObservableNormal1() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final List<NbpPublishSubject<Integer>> delays = new ArrayList<>();
        final int n = 10;
        for (int i = 0; i < n; i++) {
            NbpPublishSubject<Integer> delay = NbpPublishSubject.create();
            delays.add(delay);
        }

        Function<Integer, NbpObservable<Integer>> delayFunc = delays::get;

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);

        for (int i = 0; i < n; i++) {
            source.onNext(i);
            delays.get(i).onNext(i);
            inOrder.verify(o).onNext(i);
        }
        source.onComplete();

        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithObservableSingleSend1() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> delay = NbpPublishSubject.create();

        Function<Integer, NbpObservable<Integer>> delayFunc = t1 -> delay;
        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);

        source.onNext(1);
        delay.onNext(1);
        delay.onNext(2);

        inOrder.verify(o).onNext(1);
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithObservableSourceThrows() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> delay = NbpPublishSubject.create();

        Function<Integer, NbpObservable<Integer>> delayFunc = t1 -> delay;
        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);
        source.onNext(1);
        source.onError(new TestException());
        delay.onNext(1);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithObservableDelayFunctionThrows() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();

        Function<Integer, NbpObservable<Integer>> delayFunc = t1 -> {
            throw new TestException();
        };
        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);
        source.onNext(1);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithObservableDelayThrows() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> delay = NbpPublishSubject.create();

        Function<Integer, NbpObservable<Integer>> delayFunc = t1 -> delay;
        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);
        source.onNext(1);
        delay.onError(new TestException());

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithObservableSubscriptionNormal() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> delay = NbpPublishSubject.create();
        Supplier<NbpObservable<Integer>> subFunc = () -> delay;
        Function<Integer, NbpObservable<Integer>> delayFunc = t1 -> delay;

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(subFunc, delayFunc).subscribe(o);

        source.onNext(1);
        delay.onNext(1);

        source.onNext(2);
        delay.onNext(2);

        inOrder.verify(o).onNext(2);
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithObservableSubscriptionFunctionThrows() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> delay = NbpPublishSubject.create();
        Supplier<NbpObservable<Integer>> subFunc = () -> {
            throw new TestException();
        };
        Function<Integer, NbpObservable<Integer>> delayFunc = t1 -> delay;

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(subFunc, delayFunc).subscribe(o);

        source.onNext(1);
        delay.onNext(1);

        source.onNext(2);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithObservableSubscriptionThrows() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> delay = NbpPublishSubject.create();
        Supplier<NbpObservable<Integer>> subFunc = () -> delay;
        Function<Integer, NbpObservable<Integer>> delayFunc = t1 -> delay;

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(subFunc, delayFunc).subscribe(o);

        source.onNext(1);
        delay.onError(new TestException());

        source.onNext(2);

        inOrder.verify(o).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithObservableEmptyDelayer() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();

        Function<Integer, NbpObservable<Integer>> delayFunc = t1 -> NbpObservable.empty();
        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(delayFunc).subscribe(o);

        source.onNext(1);
        source.onComplete();

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithObservableSubscriptionRunCompletion() {
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> sdelay = NbpPublishSubject.create();
        final NbpPublishSubject<Integer> delay = NbpPublishSubject.create();
        Supplier<NbpObservable<Integer>> subFunc = () -> sdelay;
        Function<Integer, NbpObservable<Integer>> delayFunc = t1 -> delay;

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        source.delay(subFunc, delayFunc).subscribe(o);

        source.onNext(1);
        sdelay.onComplete();

        source.onNext(2);
        delay.onNext(2);

        inOrder.verify(o).onNext(2);
        inOrder.verifyNoMoreInteractions();
        verify(o, never()).onError(any(Throwable.class));
        verify(o, never()).onComplete();
    }

    @Test
    public void testDelayWithObservableAsTimed() {
        NbpObservable<Long> source = NbpObservable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);

        final NbpObservable<Long> delayer = NbpObservable.timer(500L, TimeUnit.MILLISECONDS, scheduler);

        Function<Long, NbpObservable<Long>> delayFunc = t1 -> delayer;

        NbpObservable<Long> delayed = source.delay(delayFunc);
        delayed.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(NbpObserver, never()).onNext(anyLong());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext(0L);
        inOrder.verify(NbpObserver, never()).onNext(anyLong());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2400L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(anyLong());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext(1L);
        inOrder.verify(NbpObserver, never()).onNext(anyLong());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3400L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, never()).onNext(anyLong());
        verify(NbpObserver, never()).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(NbpObserver, times(1)).onNext(2L);
        verify(NbpObserver, times(1)).onComplete();
        verify(NbpObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayWithObservableReorder() {
        int n = 3;

        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        final List<NbpPublishSubject<Integer>> subjects = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            subjects.add(NbpPublishSubject.<Integer> create());
        }

        NbpObservable<Integer> result = source.delay(t1 -> subjects.get(t1));

        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        InOrder inOrder = inOrder(o);

        result.subscribe(o);

        for (int i = 0; i < n; i++) {
            source.onNext(i);
        }
        source.onComplete();

        inOrder.verify(o, never()).onNext(anyInt());
        inOrder.verify(o, never()).onComplete();

        for (int i = n - 1; i >= 0; i--) {
            subjects.get(i).onComplete();
            inOrder.verify(o).onNext(i);
        }

        inOrder.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDelayEmitsEverything() {
        NbpObservable<Integer> source = NbpObservable.range(1, 5);
        NbpObservable<Integer> delayed = source.delay(500L, TimeUnit.MILLISECONDS, scheduler);
        delayed = delayed.doOnEach(System.out::println);
        NbpTestSubscriber<Integer> NbpObserver = new NbpTestSubscriber<>();
        delayed.subscribe(NbpObserver);
        // all will be delivered after 500ms since range does not delay between them
        scheduler.advanceTimeBy(500L, TimeUnit.MILLISECONDS);
        NbpObserver.assertValues(1, 2, 3, 4, 5);
    }

    @Test
    public void testBackpressureWithTimedDelay() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpObservable.range(1, Observable.bufferSize() * 2)
                .delay(100, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {

                    int c = 0;

                    @Override
                    public Integer apply(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Observable.bufferSize() * 2, ts.valueCount());
    }
    
    @Test
    public void testBackpressureWithSubscriptionTimedDelay() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpObservable.range(1, Observable.bufferSize() * 2)
                .delaySubscription(100, TimeUnit.MILLISECONDS)
                .delay(100, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {

                    int c = 0;

                    @Override
                    public Integer apply(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Observable.bufferSize() * 2, ts.valueCount());
    }

    @Test
    public void testBackpressureWithSelectorDelay() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpObservable.range(1, Observable.bufferSize() * 2)
                .delay(i -> NbpObservable.timer(100, TimeUnit.MILLISECONDS))
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {

                    int c = 0;

                    @Override
                    public Integer apply(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Observable.bufferSize() * 2, ts.valueCount());
    }

    @Test
    public void testBackpressureWithSelectorDelayAndSubscriptionDelay() {
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        NbpObservable.range(1, Observable.bufferSize() * 2)
                .delay(() -> NbpObservable.timer(500, TimeUnit.MILLISECONDS), i -> NbpObservable.timer(100, TimeUnit.MILLISECONDS))
                .observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {

                    int c = 0;

                    @Override
                    public Integer apply(Integer t) {
                        if (c++ <= 0) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                            }
                        }
                        return t;
                    }

                }).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        assertEquals(Observable.bufferSize() * 2, ts.valueCount());
    }
    
    @Test
    public void testErrorRunsBeforeOnNext() {
        TestScheduler test = Schedulers.test();
        
        NbpPublishSubject<Integer> ps = NbpPublishSubject.create();
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
        ps.delay(1, TimeUnit.SECONDS, test).subscribe(ts);
        
        ps.onNext(1);
        
        test.advanceTimeBy(500, TimeUnit.MILLISECONDS);
        
        ps.onError(new TestException());
        
        test.advanceTimeBy(1, TimeUnit.SECONDS);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }
    
    public void testDelaySupplierSimple() {
        final NbpPublishSubject<Integer> ps = NbpPublishSubject.create();
        
        NbpObservable<Integer> source = NbpObservable.range(1, 5);
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
        source.delaySubscription(() -> ps).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        ps.onNext(1);
        
        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertComplete();
        ts.assertNoErrors();
    }
    
    @Test
    public void testDelaySupplierCompletes() {
        final NbpPublishSubject<Integer> ps = NbpPublishSubject.create();
        
        NbpObservable<Integer> source = NbpObservable.range(1, 5);
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
        source.delaySubscription(() -> ps).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        // FIXME should this complete the source instead of consuming it?
        ps.onComplete();
        
        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertComplete();
        ts.assertNoErrors();
    }
    
    @Test
    public void testDelaySupplierErrors() {
        final NbpPublishSubject<Integer> ps = NbpPublishSubject.create();
        
        NbpObservable<Integer> source = NbpObservable.range(1, 5);
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
        source.delaySubscription(() -> ps).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        ps.onError(new TestException());
        
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

}