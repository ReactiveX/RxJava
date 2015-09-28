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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.nbp.NbpPublishSubject;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

public class NbpOperatorTakeTest {

    @Test
    public void testTake1() {
        NbpObservable<String> w = NbpObservable.fromIterable(Arrays.asList("one", "two", "three"));
        NbpObservable<String> take = w.take(2);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        take.subscribe(NbpObserver);
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, times(1)).onNext("two");
        verify(NbpObserver, never()).onNext("three");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testTake2() {
        NbpObservable<String> w = NbpObservable.fromIterable(Arrays.asList("one", "two", "three"));
        NbpObservable<String> take = w.take(1);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        take.subscribe(NbpObserver);
        verify(NbpObserver, times(1)).onNext("one");
        verify(NbpObserver, never()).onNext("two");
        verify(NbpObserver, never()).onNext("three");
        verify(NbpObserver, never()).onError(any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTakeWithError() {
        NbpObservable.fromIterable(Arrays.asList(1, 2, 3)).take(1)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t1) {
                throw new IllegalArgumentException("some error");
            }
        }).toBlocking().single();
    }

    @Test
    public void testTakeWithErrorHappeningInOnNext() {
        NbpObservable<Integer> w = NbpObservable.fromIterable(Arrays.asList(1, 2, 3))
                .take(2).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t1) {
                throw new IllegalArgumentException("some error");
            }
        });

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        w.subscribe(NbpObserver);
        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(any(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTakeWithErrorHappeningInTheLastOnNext() {
        NbpObservable<Integer> w = NbpObservable.fromIterable(Arrays.asList(1, 2, 3)).take(1).map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer t1) {
                throw new IllegalArgumentException("some error");
            }
        });

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        w.subscribe(NbpObserver);
        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(any(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTakeDoesntLeakErrors() {
        NbpObservable<String> source = NbpObservable.create(new NbpOnSubscribe<String>() {
            @Override
            public void accept(NbpSubscriber<? super String> NbpObserver) {
                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
                NbpObserver.onNext("one");
                NbpObserver.onError(new Throwable("test failed"));
            }
        });

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

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
        BooleanDisposable bs = new BooleanDisposable();
        NbpObservable<String> source = NbpObservable.create(new NbpOnSubscribe<String>() {
            @Override
            public void accept(NbpSubscriber<? super String> NbpObserver) {
                subscribed.set(true);
                NbpObserver.onSubscribe(bs);
                NbpObserver.onError(new Throwable("test failed"));
            }
        });

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();

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
        NbpObservable<String> w = NbpObservable.create(f);

        NbpSubscriber<String> NbpObserver = TestHelper.mockNbpSubscriber();
        
        NbpObservable<String> take = w.take(1);
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
        NbpObservable.create(new NbpOnSubscribe<Integer>() {

            @Override
            public void accept(NbpSubscriber<? super Integer> s) {
                BooleanDisposable bs = new BooleanDisposable();
                s.onSubscribe(bs);
                for (int i = 0; !bs.isDisposed(); i++) {
                    System.out.println("Emit: " + i);
                    count.incrementAndGet();
                    s.onNext(i);
                }
            }

        }).take(100).take(1).toBlocking().forEach(new Consumer<Integer>() {

            @Override
            public void accept(Integer t1) {
                System.out.println("Receive: " + t1);

            }

        });

        assertEquals(1, count.get());
    }

    private static class TestObservableFunc implements NbpOnSubscribe<String> {

        final String[] values;
        Thread t = null;

        public TestObservableFunc(String... values) {
            this.values = values;
        }

        @Override
        public void accept(final NbpSubscriber<? super String> NbpObserver) {
            NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
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

    private static NbpObservable<Long> INFINITE_OBSERVABLE = NbpObservable.create(new NbpOnSubscribe<Long>() {

        @Override
        public void accept(NbpSubscriber<? super Long> op) {
            BooleanDisposable bs = new BooleanDisposable();
            op.onSubscribe(bs);
            long l = 1;
            while (!bs.isDisposed()) {
                op.onNext(l++);
            }
            op.onComplete();
        }

    });
    
    @Test(timeout = 2000)
    public void testTakeObserveOn() {
        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        NbpTestSubscriber<Object> ts = new NbpTestSubscriber<>(o);
        
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
        final AtomicReference<Object> exception = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);
        NbpObservable.just(1).subscribeOn(Schedulers.computation()).take(1)
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
        NbpObservable<Integer> source = NbpObservable.just(1).take(1);
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<Integer>() {
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
        NbpPublishSubject<Integer> source = NbpPublishSubject.create();
        
        NbpTestSubscriber<Integer> ts = new NbpTestSubscriber<>();
        
        source.take(1).doOnNext(v -> source.onNext(2)).subscribe(ts);
        
        source.onNext(1);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }
}