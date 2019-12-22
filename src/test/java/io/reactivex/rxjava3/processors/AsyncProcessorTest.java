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

package io.reactivex.rxjava3.processors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.*;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class AsyncProcessorTest extends FlowableProcessorTest<Object> {

    private final Throwable testException = new Throwable();

    @Override
    protected FlowableProcessor<Object> create() {
        return AsyncProcessor.create();
    }

    @Test
    public void neverCompleted() {
        AsyncProcessor<String> processor = AsyncProcessor.create();

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        processor.subscribe(subscriber);

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");

        verify(subscriber, Mockito.never()).onNext(anyString());
        verify(subscriber, Mockito.never()).onError(testException);
        verify(subscriber, Mockito.never()).onComplete();
    }

    @Test
    public void completed() {
        AsyncProcessor<String> processor = AsyncProcessor.create();

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        processor.subscribe(subscriber);

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");
        processor.onComplete();

        verify(subscriber, times(1)).onNext("three");
        verify(subscriber, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void subscribeAfterCompleted() {
        AsyncProcessor<String> processor = AsyncProcessor.create();

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");
        processor.onComplete();

        processor.subscribe(subscriber);

        verify(subscriber, times(1)).onNext("three");
        verify(subscriber, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber, times(1)).onComplete();
    }

    @Test
    public void subscribeAfterError() {
        AsyncProcessor<String> processor = AsyncProcessor.create();

        Subscriber<String> subscriber = TestHelper.mockSubscriber();

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");

        RuntimeException re = new RuntimeException("failed");
        processor.onError(re);

        processor.subscribe(subscriber);

        verify(subscriber, times(1)).onError(re);
        verify(subscriber, Mockito.never()).onNext(any(String.class));
        verify(subscriber, Mockito.never()).onComplete();
    }

    @Test
    public void error() {
        AsyncProcessor<String> processor = AsyncProcessor.create();

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        processor.subscribe(subscriber);

        processor.onNext("one");
        processor.onNext("two");
        processor.onNext("three");
        processor.onError(testException);
        processor.onNext("four");
        processor.onError(new Throwable());
        processor.onComplete();

        verify(subscriber, Mockito.never()).onNext(anyString());
        verify(subscriber, times(1)).onError(testException);
        verify(subscriber, Mockito.never()).onComplete();
    }

    @Test
    public void unsubscribeBeforeCompleted() {
        AsyncProcessor<String> processor = AsyncProcessor.create();

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        TestSubscriber<String> ts = new TestSubscriber<>(subscriber);
        processor.subscribe(ts);

        processor.onNext("one");
        processor.onNext("two");

        ts.cancel();

        verify(subscriber, Mockito.never()).onNext(anyString());
        verify(subscriber, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber, Mockito.never()).onComplete();

        processor.onNext("three");
        processor.onComplete();

        verify(subscriber, Mockito.never()).onNext(anyString());
        verify(subscriber, Mockito.never()).onError(any(Throwable.class));
        verify(subscriber, Mockito.never()).onComplete();
    }

    @Test
    public void emptySubjectCompleted() {
        AsyncProcessor<String> processor = AsyncProcessor.create();

        Subscriber<String> subscriber = TestHelper.mockSubscriber();
        processor.subscribe(subscriber);

        processor.onComplete();

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, never()).onNext(null);
        inOrder.verify(subscriber, never()).onNext(any(String.class));
        inOrder.verify(subscriber, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Can receive timeout if subscribe never receives an onError/onComplete ... which reveals a race condition.
     */
    @Test
    public void subscribeCompletionRaceCondition() {
        /*
         * With non-threadsafe code this fails most of the time on my dev laptop and is non-deterministic enough
         * to act as a unit test to the race conditions.
         *
         * With the synchronization code in place I can not get this to fail on my laptop.
         */
        for (int i = 0; i < 50; i++) {
            final AsyncProcessor<String> processor = AsyncProcessor.create();
            final AtomicReference<String> value1 = new AtomicReference<>();

            processor.subscribe(new Consumer<String>() {

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
                    processor.onNext("value");
                    processor.onComplete();
                }
            });

            SubjectSubscriberThread t2 = new SubjectSubscriberThread(processor);
            SubjectSubscriberThread t3 = new SubjectSubscriberThread(processor);
            SubjectSubscriberThread t4 = new SubjectSubscriberThread(processor);
            SubjectSubscriberThread t5 = new SubjectSubscriberThread(processor);

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

    private static class SubjectSubscriberThread extends Thread {

        private final AsyncProcessor<String> processor;
        private final AtomicReference<String> value = new AtomicReference<>();

        SubjectSubscriberThread(AsyncProcessor<String> processor) {
            this.processor = processor;
        }

        @Override
        public void run() {
            try {
                // a timeout exception will happen if we don't get a terminal state
                String v = processor.timeout(2000, TimeUnit.MILLISECONDS).blockingSingle();
                value.set(v);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void currentStateMethodsNormal() {
        AsyncProcessor<Object> as = AsyncProcessor.create();

        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());

        as.onNext(1);

        assertFalse(as.hasValue()); // AP no longer reports it has a value until it is terminated
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getValue()); // AP no longer reports it has a value until it is terminated
        assertNull(as.getThrowable());

        as.onComplete();
        assertTrue(as.hasValue());
        assertFalse(as.hasThrowable());
        assertTrue(as.hasComplete());
        assertEquals(1, as.getValue());
        assertNull(as.getThrowable());
    }

    @Test
    public void currentStateMethodsEmpty() {
        AsyncProcessor<Object> as = AsyncProcessor.create();

        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());

        as.onComplete();

        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertTrue(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());
    }

    @Test
    public void currentStateMethodsError() {
        AsyncProcessor<Object> as = AsyncProcessor.create();

        assertFalse(as.hasValue());
        assertFalse(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getValue());
        assertNull(as.getThrowable());

        as.onError(new TestException());

        assertFalse(as.hasValue());
        assertTrue(as.hasThrowable());
        assertFalse(as.hasComplete());
        assertNull(as.getValue());
        assertTrue(as.getThrowable() instanceof TestException);
    }

    @Test
    public void fusionLive() {
        AsyncProcessor<Integer> ap = new AsyncProcessor<>();

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        ap.subscribe(ts);

        ts
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC);

        ts.assertNoValues().assertNoErrors().assertNotComplete();

        ap.onNext(1);

        ts.assertNoValues().assertNoErrors().assertNotComplete();

        ap.onComplete();

        ts.assertResult(1);
    }

    @Test
    public void fusionOfflie() {
        AsyncProcessor<Integer> ap = new AsyncProcessor<>();
        ap.onNext(1);
        ap.onComplete();

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        ap.subscribe(ts);

        ts
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1);
    }

    @Test
    public void onSubscribeAfterDone() {
        AsyncProcessor<Object> p = AsyncProcessor.create();

        BooleanSubscription bs = new BooleanSubscription();
        p.onSubscribe(bs);

        assertFalse(bs.isCancelled());

        p.onComplete();

        bs = new BooleanSubscription();
        p.onSubscribe(bs);

        assertTrue(bs.isCancelled());

        p.test().assertResult();
    }

    @Test
    public void cancelUpfront() {
        AsyncProcessor<Object> p = AsyncProcessor.create();

        assertFalse(p.hasSubscribers());

        p.test().assertEmpty();
        p.test().assertEmpty();

        p.test(0L, true).assertEmpty();

        assertTrue(p.hasSubscribers());
    }

    @Test
    public void cancelRace() {
        AsyncProcessor<Object> p = AsyncProcessor.create();

        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final TestSubscriber<Object> ts1 = p.test();
            final TestSubscriber<Object> ts2 = p.test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    ts2.cancel();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void onErrorCancelRace() {

        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final AsyncProcessor<Object> p = AsyncProcessor.create();

            final TestSubscriberEx<Object> ts1 = p.to(TestHelper.<Object>testConsumer());

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ts1.cancel();
                }
            };

            final TestException ex = new TestException();

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    p.onError(ex);
                }
            };

            TestHelper.race(r1, r2);

            if (ts1.errors().size() != 0) {
                ts1.assertFailure(TestException.class);
            } else {
                ts1.assertEmpty();
            }
        }
    }

    @Test
    public void onNextCrossCancel() {
        AsyncProcessor<Object> p = AsyncProcessor.create();

        final TestSubscriber<Object> ts2 = new TestSubscriber<>();
        TestSubscriber<Object> ts1 = new TestSubscriber<Object>() {
            @Override
            public void onNext(Object t) {
                ts2.cancel();
                super.onNext(t);
            }
        };

        p.subscribe(ts1);
        p.subscribe(ts2);

        p.onNext(1);
        p.onComplete();

        ts1.assertResult(1);
        ts2.assertEmpty();
    }

    @Test
    public void onErrorCrossCancel() {
        AsyncProcessor<Object> p = AsyncProcessor.create();

        final TestSubscriber<Object> ts2 = new TestSubscriber<>();
        TestSubscriber<Object> ts1 = new TestSubscriber<Object>() {
            @Override
            public void onError(Throwable t) {
                ts2.cancel();
                super.onError(t);
            }
        };

        p.subscribe(ts1);
        p.subscribe(ts2);

        p.onError(new TestException());

        ts1.assertFailure(TestException.class);
        ts2.assertEmpty();
    }

    @Test
    public void onCompleteCrossCancel() {
        AsyncProcessor<Object> p = AsyncProcessor.create();

        final TestSubscriber<Object> ts2 = new TestSubscriber<>();
        TestSubscriber<Object> ts1 = new TestSubscriber<Object>() {
            @Override
            public void onComplete() {
                ts2.cancel();
                super.onComplete();
            }
        };

        p.subscribe(ts1);
        p.subscribe(ts2);

        p.onComplete();

        ts1.assertResult();
        ts2.assertEmpty();
    }
}
