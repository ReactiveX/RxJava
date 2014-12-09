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
package rx.schedulers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func1;

public class TestSchedulerTest {

    @SuppressWarnings("unchecked")
    // mocking is unchecked, unfortunately
    @Test
    public final void testPeriodicScheduling() {
        final Func1<Long, Void> calledOp = mock(Func1.class);

        final TestScheduler scheduler = new TestScheduler();
        final Scheduler.Worker inner = scheduler.createWorker();
        
        try {
            inner.schedulePeriodically(new Action0() {
                @Override
                public void call() {
                    System.out.println(scheduler.now());
                    calledOp.call(scheduler.now());
                }
            }, 1, 2, TimeUnit.SECONDS);
    
            verify(calledOp, never()).call(anyLong());
    
            InOrder inOrder = Mockito.inOrder(calledOp);
    
            scheduler.advanceTimeBy(999L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, never()).call(anyLong());
    
            scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, times(1)).call(1000L);
    
            scheduler.advanceTimeBy(1999L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, never()).call(3000L);
    
            scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, times(1)).call(3000L);
    
            scheduler.advanceTimeBy(5L, TimeUnit.SECONDS);
            inOrder.verify(calledOp, times(1)).call(5000L);
            inOrder.verify(calledOp, times(1)).call(7000L);
    
            inner.unsubscribe();
            scheduler.advanceTimeBy(11L, TimeUnit.SECONDS);
            inOrder.verify(calledOp, never()).call(anyLong());
        } finally {
            inner.unsubscribe();
        }
    }

    @SuppressWarnings("unchecked")
    // mocking is unchecked, unfortunately
    @Test
    public final void testPeriodicSchedulingUnsubscription() {
        final Func1<Long, Void> calledOp = mock(Func1.class);

        final TestScheduler scheduler = new TestScheduler();
        final Scheduler.Worker inner = scheduler.createWorker();

        try {
            final Subscription subscription = inner.schedulePeriodically(new Action0() {
                @Override
                public void call() {
                    System.out.println(scheduler.now());
                    calledOp.call(scheduler.now());
                }
            }, 1, 2, TimeUnit.SECONDS);
    
            verify(calledOp, never()).call(anyLong());
    
            InOrder inOrder = Mockito.inOrder(calledOp);
    
            scheduler.advanceTimeBy(999L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, never()).call(anyLong());
    
            scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, times(1)).call(1000L);
    
            scheduler.advanceTimeBy(1999L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, never()).call(3000L);
    
            scheduler.advanceTimeBy(1L, TimeUnit.MILLISECONDS);
            inOrder.verify(calledOp, times(1)).call(3000L);
    
            scheduler.advanceTimeBy(5L, TimeUnit.SECONDS);
            inOrder.verify(calledOp, times(1)).call(5000L);
            inOrder.verify(calledOp, times(1)).call(7000L);
    
            subscription.unsubscribe();
            scheduler.advanceTimeBy(11L, TimeUnit.SECONDS);
            inOrder.verify(calledOp, never()).call(anyLong());
        } finally {
            inner.unsubscribe();
        }
    }

    @Test
    public final void testImmediateUnsubscribes() {
        TestScheduler s = new TestScheduler();
        final Scheduler.Worker inner = s.createWorker();
        final AtomicInteger counter = new AtomicInteger(0);
        
        try {
            inner.schedule(new Action0() {
    
                @Override
                public void call() {
                    counter.incrementAndGet();
                    System.out.println("counter: " + counter.get());
                    inner.schedule(this);
                }
    
            });
            inner.unsubscribe();
            assertEquals(0, counter.get());
        } finally {
            inner.unsubscribe();
        }
    }

    @Test
    public final void testImmediateUnsubscribes2() {
        TestScheduler s = new TestScheduler();
        final Scheduler.Worker inner = s.createWorker();
        try {
            final AtomicInteger counter = new AtomicInteger(0);
    
            final Subscription subscription = inner.schedule(new Action0() {
    
                @Override
                public void call() {
                    counter.incrementAndGet();
                    System.out.println("counter: " + counter.get());
                    inner.schedule(this);
                }
    
            });
            subscription.unsubscribe();
            assertEquals(0, counter.get());
        } finally {
            inner.unsubscribe();
        }
    }

    @Test
    public final void testNestedSchedule() {
        final TestScheduler scheduler = new TestScheduler();
        final Scheduler.Worker inner = scheduler.createWorker();
        
        try {
            final Action0 calledOp = mock(Action0.class);
    
            Observable<Object> poller;
            poller = Observable.create(new OnSubscribe<Object>() {
                @Override
                public void call(final Subscriber<? super Object> aSubscriber) {
                    inner.schedule(new Action0() {
                        @Override
                        public void call() {
                            if (!aSubscriber.isUnsubscribed()) {
                                calledOp.call();
                                inner.schedule(this, 5, TimeUnit.SECONDS);
                            }
                        }
                    });
                }
            });
    
            InOrder inOrder = Mockito.inOrder(calledOp);
    
            Subscription sub;
            sub = poller.subscribe();
    
            scheduler.advanceTimeTo(6, TimeUnit.SECONDS);
            inOrder.verify(calledOp, times(2)).call();
    
            sub.unsubscribe();
            scheduler.advanceTimeTo(11, TimeUnit.SECONDS);
            inOrder.verify(calledOp, never()).call();
    
            sub = poller.subscribe();
            scheduler.advanceTimeTo(12, TimeUnit.SECONDS);
            inOrder.verify(calledOp, times(1)).call();
        } finally {
            inner.unsubscribe();
        }
    }
}
