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
package rx.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

public class OperatorRetryTest {

    @Test
    public void testOriginFails() {
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> origin = Observable.create(new FuncWithErrors(2));
        origin.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("beginningEveryTime");
        inOrder.verify(observer, times(1)).onError(any(RuntimeException.class));
        inOrder.verify(observer, never()).onNext("onSuccessOnly");
        inOrder.verify(observer, never()).onCompleted();
    }

    @Test
    public void testRetryFail() {
        int NUM_RETRIES = 1;
        int NUM_FAILURES = 2;
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> origin = Observable.create(new FuncWithErrors(NUM_FAILURES));
        origin.nest().lift(new OperatorRetry<String>(NUM_RETRIES)).subscribe(observer);

        InOrder inOrder = inOrder(observer);
        // should show 2 attempts (first time fail, second time (1st retry) fail)
        inOrder.verify(observer, times(1 + NUM_RETRIES)).onNext("beginningEveryTime");
        // should only retry once, fail again and emit onError
        inOrder.verify(observer, times(1)).onError(any(RuntimeException.class));
        // no success
        inOrder.verify(observer, never()).onNext("onSuccessOnly");
        inOrder.verify(observer, never()).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testRetrySuccess() {
        int NUM_RETRIES = 3;
        int NUM_FAILURES = 2;
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> origin = Observable.create(new FuncWithErrors(NUM_FAILURES));
        origin.nest().lift(new OperatorRetry<String>(NUM_RETRIES)).subscribe(observer);

        InOrder inOrder = inOrder(observer);
        // should show 3 attempts
        inOrder.verify(observer, times(1 + NUM_FAILURES)).onNext("beginningEveryTime");
        // should have no errors
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        // should have a single success
        inOrder.verify(observer, times(1)).onNext("onSuccessOnly");
        // should have a single successful onCompleted
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testInfiniteRetry() {
        int NUM_FAILURES = 20;
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);
        Observable<String> origin = Observable.create(new FuncWithErrors(NUM_FAILURES));
        origin.nest().lift(new OperatorRetry<String>()).subscribe(observer);

        InOrder inOrder = inOrder(observer);
        // should show 3 attempts
        inOrder.verify(observer, times(1 + NUM_FAILURES)).onNext("beginningEveryTime");
        // should have no errors
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        // should have a single success
        inOrder.verify(observer, times(1)).onNext("onSuccessOnly");
        // should have a single successful onCompleted
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }
    
    /**
     * Checks in a simple and synchronous way that retry resubscribes
     * after error. This test fails against 0.16.1-0.17.4, hangs on 0.17.5 and
     * passes in 0.17.6 thanks to fix for issue #1027.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testRetrySubscribesAgainAfterError() {

        // record emitted values with this action
        Action1<Integer> record = mock(Action1.class);
        InOrder inOrder = inOrder(record);

        // always throw an exception with this action
        Action1<Integer> throwException = mock(Action1.class);
        doThrow(new RuntimeException()).when(throwException).call(Mockito.anyInt());
        
        // create a retrying observable based on a PublishSubject
        PublishSubject<Integer> subject = PublishSubject.create();
        subject
        // record item
        .doOnNext(record)
        // throw a RuntimeException
                .doOnNext(throwException)
                // retry on error
                .retry()
                // subscribe and ignore
                .subscribe();

        inOrder.verifyNoMoreInteractions();

        subject.onNext(1);
        inOrder.verify(record).call(1);

        subject.onNext(2);
        inOrder.verify(record).call(2);

        subject.onNext(3);
        inOrder.verify(record).call(3);

        inOrder.verifyNoMoreInteractions();
    }


    public static class FuncWithErrors implements Observable.OnSubscribe<String> {

        private final int numFailures;
        private final AtomicInteger count = new AtomicInteger(0);

        FuncWithErrors(int count) {
            this.numFailures = count;
        }

        @Override
        public void call(Subscriber<? super String> o) {
            o.onNext("beginningEveryTime");
            if (count.incrementAndGet() <= numFailures) {
                o.onError(new RuntimeException("forced failure: " + count.get()));
            } else {
                o.onNext("onSuccessOnly");
                o.onCompleted();
            }
        }
    }

    @Test
    public void testUnsubscribeFromRetry() {
        PublishSubject<Integer> subject = PublishSubject.create();
        final AtomicInteger count = new AtomicInteger(0);
        Subscription sub = subject.retry().subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer n) {
                count.incrementAndGet();
            }
        });
        subject.onNext(1);
        sub.unsubscribe();
        subject.onNext(2);
        assertEquals(1, count.get());
    }

    @Test
    public void testRetryAllowsSubscriptionAfterAllSubscriptionsUnsubscribed() throws InterruptedException {
        final AtomicInteger subsCount = new AtomicInteger(0);
        OnSubscribe<String> onSubscribe = new OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> s) {
                subsCount.incrementAndGet();
                s.add(new Subscription() {
                    boolean unsubscribed = false;

                    @Override
                    public void unsubscribe() {
                        subsCount.decrementAndGet();
                        unsubscribed = true;
                    }

                    @Override
                    public boolean isUnsubscribed() {
                        return unsubscribed;
                    }
                });
            }
        };
        Observable<String> stream = Observable.create(onSubscribe);
        Observable<String> streamWithRetry = stream.retry();
        Subscription sub = streamWithRetry.subscribe();
        assertEquals(1, subsCount.get());
        sub.unsubscribe();
        assertEquals(0, subsCount.get());
        streamWithRetry.subscribe();
        assertEquals(1, subsCount.get());
    }

    @Test
    public void testSourceObservableCallsUnsubscribe() throws InterruptedException {
        final AtomicInteger subsCount = new AtomicInteger(0);

        final TestSubscriber<String> ts = new TestSubscriber<String>();

        OnSubscribe<String> onSubscribe = new OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> s) {
                // if isUnsubscribed is true that means we have a bug such as https://github.com/Netflix/RxJava/issues/1024
                if (!s.isUnsubscribed()) {
                    subsCount.incrementAndGet();
                    s.onError(new RuntimeException("failed"));
                    // it unsubscribes the child directly
                    // this simulates various error/completion scenarios that could occur
                    // or just a source that proactively triggers cleanup
                    s.unsubscribe();
                }
            }
        };

        Observable.create(onSubscribe).retry(3).subscribe(ts);
        assertEquals(4, subsCount.get()); // 1 + 3 retries
    }

    class SlowObservable implements Observable.OnSubscribe<Long> {

        private AtomicInteger efforts = new AtomicInteger(0);
        private AtomicInteger active = new AtomicInteger(0), maxActive = new AtomicInteger(0);
        private AtomicInteger nextBeforeFailure;

        private final int emitDelay;

        public SlowObservable(int emitDelay, int countNext) {
            this.emitDelay = emitDelay;
            this.nextBeforeFailure = new AtomicInteger(countNext);
        }

        public void call(final Subscriber<? super Long> subscriber) {
            final AtomicBoolean terminate = new AtomicBoolean(false);
            efforts.getAndIncrement();
            active.getAndIncrement();
            maxActive.set(Math.max(active.get(), maxActive.get()));
            final Thread thread = new Thread() {
                @Override
                public void run() {
                    long nr = 0;
                    try {
                        while (!terminate.get()) {
                            Thread.sleep(emitDelay);
                            if (nextBeforeFailure.getAndDecrement() > 0) {
                                subscriber.onNext(nr++);
                            }
                            else {
                                subscriber.onError(new RuntimeException("expected-failed"));
                            }
                        }
                    }
                    catch (InterruptedException t) {
                    }
                }
            };
            thread.start();
            subscriber.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    terminate.set(true);
                    active.decrementAndGet();
                }
            }));
        }
    }

    /** Observer for listener on seperate thread */
    class AsyncObserver<T> implements Observer<T> {

        protected CountDownLatch latch = new CountDownLatch(1);

        protected Observer<T> target;

        /** Wrap existing Observer */
        public AsyncObserver(Observer<T> target) {
            this.target = target;
        }

        /** Wait */
        public void await() {
            try {
                latch.await();
            } catch (InterruptedException e) {
                fail("Test interrupted");
            }
        }

        // Observer implementation

        @Override
        public void onCompleted() {
            target.onCompleted();
            latch.countDown();
        }

        @Override
        public void onError(Throwable t) {
            target.onError(t);
            latch.countDown();
        }

        @Override
        public void onNext(T v) {
            target.onNext(v);
        }
    }

    @Test(timeout = 1000)
    public void testUnsubscribeAfterError() {

        @SuppressWarnings("unchecked")
        Observer<Long> observer = mock(Observer.class);

        // Observable that always fails after 100ms
        SlowObservable so = new SlowObservable(100, 0);
        Observable<Long> o = Observable
                .create(so)
                .retry(5);

        AsyncObserver<Long> async = new AsyncObserver<Long>(observer);

        o.subscribe(async);

        async.await();

        InOrder inOrder = inOrder(observer);
        // Should fail once
        inOrder.verify(observer, times(1)).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onCompleted();

        assertEquals("Start 6 threads, retry 5 then fail on 6", 6, so.efforts.get());
        assertEquals("Only 1 active subscription", 1, so.maxActive.get());
    }

    @Test(timeout = 1000)
    public void testTimeoutWithRetry() {

        @SuppressWarnings("unchecked")
        Observer<Long> observer = mock(Observer.class);

        // Observable that sends every 100ms (timeout fails instead)
        SlowObservable so = new SlowObservable(100, 10);
        Observable<Long> o = Observable
                .create(so)
                .timeout(80, TimeUnit.MILLISECONDS)
                .retry(5);

        AsyncObserver<Long> async = new AsyncObserver<Long>(observer);

        o.subscribe(async);

        async.await();

        InOrder inOrder = inOrder(observer);
        // Should fail once
        inOrder.verify(observer, times(1)).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onCompleted();

        assertEquals("Start 6 threads, retry 5 then fail on 6", 6, so.efforts.get());
    }
    
}
