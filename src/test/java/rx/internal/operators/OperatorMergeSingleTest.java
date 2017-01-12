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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.*;

import org.junit.*;
import org.mockito.*;

import rx.*;
import rx.Observable;
import rx.Observer;

public class OperatorMergeSingleTest {

    @Mock
    Observer<String> stringObserver;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMergeOfSyncSync() {
        final Single<String> o1 = Single.create(new TestSynchronousSingle());
        final Single<String> o2 = Single.create(new TestSynchronousSingle());

        Observable<Single<String>> observableOfObservables = Observable.just(o1, o2);
        Observable<String> m = Single.merge(observableOfObservables);
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onCompleted();
        verify(stringObserver, times(2)).onNext("hello");
    }

    @Test
    public void testMergeOfSyncSErr() {
        final Single<String> o1 = Single.create(new TestSynchronousSingle());
        final Single<String> o2 = Single.create(new TestSynchronousErrorSingle());

        Observable<Single<String>> observableOfObservables = Observable.just(o1, o2);
        Observable<String> m = Single.merge(observableOfObservables);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(Throwable.class));
        verify(stringObserver, times(0)).onCompleted();
        verify(stringObserver, times(1)).onNext("hello");
    }

    @Test
    public void testMergeOfAsyncSErr() throws Exception {
        CyclicBarrier b = new CyclicBarrier(2);
        CountDownLatch l = new CountDownLatch(1);
        final Single<String> o1 = Single.create(new TestAsynchronousSingle(b, l));
        final Single<String> o2 = Single.create(new TestSynchronousErrorSingle());

        Observable<Single<String>> observableOfObservables = Observable.just(o1, o2);
        Observable<String> m = Single.merge(observableOfObservables);
        m.subscribe(stringObserver);

        verify(stringObserver, times(1)).onError(any(Throwable.class));
        verify(stringObserver, times(0)).onCompleted();
        verify(stringObserver, times(0)).onNext("hello");

        // unblock the async
        b.await();
        l.await();

        // nothing should have changed.
        verify(stringObserver, times(1)).onError(any(Throwable.class));
        verify(stringObserver, times(0)).onCompleted();
        verify(stringObserver, times(0)).onNext("hello");
    }

    @Test
    public void testMergeOfAsyncAsync() throws Exception {
        CyclicBarrier b = new CyclicBarrier(3);
        CountDownLatch l = new CountDownLatch(2);
        final Single<String> o1 = Single.create(new TestAsynchronousSingle(b, l));
        final Single<String> o2 = Single.create(new TestAsynchronousSingle(b, l));

        Observable<Single<String>> observableOfObservables = Observable.just(o1, o2);
        Observable<String> m = Single.merge(observableOfObservables);
        m.subscribe(stringObserver);

        verify(stringObserver, times(0)).onError(any(Throwable.class));
        verify(stringObserver, times(0)).onCompleted();
        verify(stringObserver, times(0)).onNext("hello");

        b.await();
        l.await();

        verify(stringObserver, times(0)).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onCompleted();
        verify(stringObserver, times(2)).onNext("hello");
    }

    @Test
    public void testMergeOfAsyncAErr() throws Exception {
        CyclicBarrier bs = new CyclicBarrier(2);
        CyclicBarrier be = new CyclicBarrier(2);
        CountDownLatch ls = new CountDownLatch(1);
        CountDownLatch le = new CountDownLatch(1);
        final Single<String> o1 = Single.create(new TestAsynchronousSingle(bs, ls));
        final Single<String> o2 = Single.create(new TestAsynchronousErrorSingle(be, le));

        Observable<Single<String>> observableOfObservables = Observable.just(o1, o2);
        Observable<String> m = Single.merge(observableOfObservables);
        m.subscribe(stringObserver);

        verify(stringObserver, times(0)).onError(any(Throwable.class));
        verify(stringObserver, times(0)).onCompleted();
        verify(stringObserver, times(0)).onNext("hello");

        // unblock the error
        be.await();
        le.await();

        verify(stringObserver, times(1)).onError(any(Throwable.class));
        verify(stringObserver, times(0)).onCompleted();
        verify(stringObserver, times(0)).onNext("hello");

        // unblock the success
        bs.await();
        ls.await();

        verify(stringObserver, times(1)).onError(any(Throwable.class));
        verify(stringObserver, times(0)).onCompleted();
        verify(stringObserver, times(0)).onNext("hello");
    }

    @Test
    public void testMergeOfAErroAsync() throws Exception {
        CyclicBarrier bs = new CyclicBarrier(2);
        CyclicBarrier be = new CyclicBarrier(2);
        CountDownLatch ls = new CountDownLatch(1);
        CountDownLatch le = new CountDownLatch(1);
        final Single<String> o1 = Single.create(new TestAsynchronousSingle(bs, ls));
        final Single<String> o2 = Single.create(new TestAsynchronousErrorSingle(be, le));

        Observable<Single<String>> observableOfObservables = Observable.just(o1, o2);
        Observable<String> m = Single.merge(observableOfObservables);
        m.subscribe(stringObserver);

        verify(stringObserver, times(0)).onError(any(Throwable.class));
        verify(stringObserver, times(0)).onCompleted();
        verify(stringObserver, times(0)).onNext("hello");

        // unblock the success
        bs.await();
        ls.await();

        verify(stringObserver, times(0)).onError(any(Throwable.class));
        verify(stringObserver, times(0)).onCompleted();
        verify(stringObserver, times(1)).onNext("hello");

        // unblock the error
        be.await();
        le.await();

        verify(stringObserver, times(1)).onError(any(Throwable.class));
        verify(stringObserver, times(0)).onCompleted();
        verify(stringObserver, times(1)).onNext("hello");
    }

    @Test
    public void testMergeOfSyncNull() throws InterruptedException {
        final Single<String> o1 = Single.create(new TestSynchronousSingle());

        Observable<Single<String>> observableOfObservables = Observable.just(o1, null);
        Observable<String> m = Single.merge(observableOfObservables);
        m.subscribe(stringObserver);

        verify(stringObserver, times(0)).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onCompleted();
        verify(stringObserver, times(1)).onNext("hello");
    }

    @Test
    public void testMergeOfJustJust() {
        final Single<String> o1 = Single.just("hello");
        final Single<String> o2 = Single.just("hello");

        Observable<Single<String>> observableOfObservables = Observable.just(o1, o2);
        Observable<String> m = Single.merge(observableOfObservables);
        m.subscribe(stringObserver);

        verify(stringObserver, times(0)).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onCompleted();
        verify(stringObserver, times(2)).onNext("hello");
    }

    private static class TestSynchronousSingle implements Single.OnSubscribe<String> {
        @Override
        public void call(SingleSubscriber<? super String> observer) {
            observer.onSuccess("hello");
        }
    }

    private static class TestAsynchronousSingle implements Single.OnSubscribe<String> {
        Thread t;
        final CountDownLatch sent;
        final CyclicBarrier barrier;

        public TestAsynchronousSingle(CyclicBarrier b, CountDownLatch l) {
            this.barrier = b;
            sent = l;
        }

        @Override
        public void call(final SingleSubscriber<? super String> observer) {
            t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        barrier.await();
                    } catch (Exception e) {
                        observer.onError(e);
                    }
                    try {
                        observer.onSuccess("hello");
                        sent.countDown();
                        // I can't use a countDownLatch to prove we are actually sending 'onNext'
                        // since it will block if synchronized and I'll deadlock
                    } catch (Exception e) {
                        observer.onError(e);
                    }
                }
            });
            t.start();
        }
    }

    private static class TestSynchronousErrorSingle implements Single.OnSubscribe<String> {
        @Override
        public void call(SingleSubscriber<? super String> observer) {
            observer.onError(new NullPointerException());
        }
    }

    private static class TestAsynchronousErrorSingle implements Single.OnSubscribe<String> {
        Thread t;
        final CountDownLatch onErrorSent;
        final CyclicBarrier barrier;

        public TestAsynchronousErrorSingle(CyclicBarrier b, CountDownLatch l) {
            barrier = b;
            onErrorSent = l;
        }

        @Override
        public void call(final SingleSubscriber<? super String> observer) {
            t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        barrier.await();
                    } catch (Exception e) {
                        observer.onError(e);
                    }
                    observer.onError(new NullPointerException());
                    onErrorSent.countDown();
                }
            });
            t.start();
        }
    }
}
