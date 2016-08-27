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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class ObservableTakeTest {

    @Test
    public void testTake1() {
        Observable<String> w = Observable.fromIterable(Arrays.asList("one", "two", "three"));
        Observable<String> take = w.take(2);

        Observer<String> NbpObserver = TestHelper.mockObserver();
        take.subscribe(NbpObserver);
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, times(1)).onNext("two");
        verify(NbpObserver, never()).onNext("three");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testTake2() {
        Observable<String> w = Observable.fromIterable(Arrays.asList("one", "two", "three"));
        Observable<String> take = w.take(1);

        Observer<String> NbpObserver = TestHelper.mockObserver();
        take.subscribe(NbpObserver);
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, never()).onNext("two");
        verify(NbpObserver, never()).onNext("three");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTakeWithError() {
        Observable.fromIterable(Arrays.asList(1, 2, 3)).take(1)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t1) {
                throw new IllegalArgumentException("some error");
            }
        }).blockingSingle();
    }

    @Test
    public void testTakeWithErrorHappeningInOnNext() {
        Observable<Integer> w = Observable.fromIterable(Arrays.asList(1, 2, 3))
                .take(2).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t1) {
                throw new IllegalArgumentException("some error");
            }
        });

        Observer<Integer> NbpObserver = TestHelper.mockObserver();
        w.subscribe(NbpObserver);
        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(any(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTakeWithErrorHappeningInTheLastOnNext() {
        Observable<Integer> w = Observable.fromIterable(Arrays.asList(1, 2, 3)).take(1).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t1) {
                throw new IllegalArgumentException("some error");
            }
        });

        Observer<Integer> NbpObserver = TestHelper.mockObserver();
        w.subscribe(NbpObserver);
        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(any(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTakeDoesntLeakErrors() {
        Observable<String> source = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> NbpObserver) {
                NbpObserver.onSubscribe(Disposables.empty());
                NbpObserver.onNext("one");
                NbpObserver.onError(new Throwable("test failed"));
            }
        });

        Observer<String> NbpObserver = TestHelper.mockObserver();

        source.take(1).subscribe(NbpObserver);

        verify(NbpObserver).onSubscribe((Disposable)notNull());
        verify(NbpObserver, times(1)).onNext("one");
        // even though onError is called we take(1) so shouldn't see it
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verifyNoMoreInteractions(NbpObserver);
    }

    @Test
    @Ignore("take(0) is now empty() and doesn't even subscribe to the original source")
    public void testTakeZeroDoesntLeakError() {
        final AtomicBoolean subscribed = new AtomicBoolean(false);
        final Disposable bs = Disposables.empty();
        Observable<String> source = Observable.unsafeCreate(new ObservableSource<String>() {
            @Override
            public void subscribe(Observer<? super String> NbpObserver) {
                subscribed.set(true);
                NbpObserver.onSubscribe(bs);
                NbpObserver.onError(new Throwable("test failed"));
            }
        });

        Observer<String> NbpObserver = TestHelper.mockObserver();

        source.take(0).subscribe(NbpObserver);
        assertTrue("source subscribed", subscribed.get());
        assertTrue("source unsubscribed", bs.isDisposed());

        verify(NbpObserver, never()).onNext(anyString());
        // even though onError is called we take(0) so shouldn't see it
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
        verifyNoMoreInteractions(NbpObserver);
    }

    @Test
    public void testUnsubscribeAfterTake() {
        TestObservableFunc f = new TestObservableFunc("one", "two", "three");
        Observable<String> w = Observable.unsafeCreate(f);

        Observer<String> NbpObserver = TestHelper.mockObserver();
        
        Observable<String> take = w.take(1);
        take.subscribe(NbpObserver);

        // wait for the NbpObservable to complete
        try {
            f.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        System.out.println("TestObservable thread finished");
        verify(NbpObserver).onSubscribe((Disposable)notNull());
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, never()).onNext("two");
        verify(NbpObserver, never()).onNext("three");
        verify(NbpObserver, times(1)).onComplete();
        // FIXME no longer assertable
//        verify(s, times(1)).unsubscribe();
        verifyNoMoreInteractions(NbpObserver);
    }

    @Test(timeout = 2000)
    public void testUnsubscribeFromSynchronousInfiniteObservable() {
        final AtomicLong count = new AtomicLong();
        INFINITE_OBSERVABLE.take(10).subscribe(new Consumer<Long>() {

            @Override
            public void accept(Long l) {
                count.set(l);
            }

        });
        assertEquals(10, count.get());
    }

    @Test(timeout = 2000)
    public void testMultiTake() {
        final AtomicInteger count = new AtomicInteger();
        Observable.unsafeCreate(new ObservableSource<Integer>() {

            @Override
            public void subscribe(Observer<? super Integer> s) {
                Disposable bs = Disposables.empty();
                s.onSubscribe(bs);
                for (int i = 0; !bs.isDisposed(); i++) {
                    System.out.println("Emit: " + i);
                    count.incrementAndGet();
                    s.onNext(i);
                }
            }

        }).take(100).take(1).blockingForEach(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                System.out.println("Receive: " + t1);

            }

        });

        assertEquals(1, count.get());
    }

    private static class TestObservableFunc implements ObservableSource<String> {

        final String[] values;
        Thread t = null;

        public TestObservableFunc(String... values) {
            this.values = values;
        }

        @Override
        public void subscribe(final Observer<? super String> NbpObserver) {
            NbpObserver.onSubscribe(Disposables.empty());
            System.out.println("TestObservable subscribed to ...");
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            System.out.println("TestObservable onNext: " + s);
                            NbpObserver.onNext(s);
                        }
                        NbpObserver.onComplete();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }

            });
            System.out.println("starting TestObservable thread");
            t.start();
            System.out.println("done starting TestObservable thread");
        }
    }

    private static Observable<Long> INFINITE_OBSERVABLE = Observable.unsafeCreate(new ObservableSource<Long>() {

        @Override
        public void subscribe(Observer<? super Long> op) {
            Disposable d = Disposables.empty();
            op.onSubscribe(d);
            long l = 1;
            while (!d.isDisposed()) {
                op.onNext(l++);
            }
            op.onComplete();
        }

    });
    
    @Test(timeout = 2000)
    public void testTakeObserveOn() {
        Observer<Object> o = TestHelper.mockObserver();
        TestObserver<Object> ts = new TestObserver<Object>(o);
        
        INFINITE_OBSERVABLE
        .observeOn(Schedulers.newThread()).take(1).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        
        verify(o).onNext(1L);
        verify(o, never()).onNext(2L);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testInterrupt() throws InterruptedException {
        final AtomicReference<Object> exception = new AtomicReference<Object>();
        final CountDownLatch latch = new CountDownLatch(1);
        Observable.just(1).subscribeOn(Schedulers.computation()).take(1)
        .subscribe(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    exception.set(e);
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }

        });

        latch.await();
        assertNull(exception.get());
    }
    
    @Test
    public void takeFinalValueThrows() {
        Observable<Integer> source = Observable.just(1).take(1);
        
        TestObserver<Integer> ts = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }
        };
        
        source.safeSubscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }
    
    @Test
    public void testReentrantTake() {
        final PublishSubject<Integer> source = PublishSubject.create();
        
        TestObserver<Integer> ts = new TestObserver<Integer>();
        
        source.take(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) {
                source.onNext(2);
            }
        }).subscribe(ts);
        
        source.onNext(1);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }
    
    @Test
    public void takeNegative() {
        try {
            Observable.just(1).take(-99);
            fail("Should have thrown");
        } catch (IllegalArgumentException ex) {
            assertEquals("count >= 0 required but it was -99", ex.getMessage());
        }
    }
}
