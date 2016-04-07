/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rx.internal.operators;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.*;

import org.junit.*;

import rx.Observable;
import rx.exceptions.TestException;
import rx.functions.*;
import rx.internal.util.RxRingBuffer;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class OperatorEagerConcatMapTest {
    TestSubscriber<Object> ts;
    TestSubscriber<Object> tsBp;
    
    Func1<Integer, Observable<Integer>> toJust = new Func1<Integer, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Integer t) {
            return Observable.just(t);
        }
    };

    Func1<Integer, Observable<Integer>> toRange = new Func1<Integer, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Integer t) {
            return Observable.range(t, 2);
        }
    };

    @Before
    public void before() {
        ts = new TestSubscriber<Object>();
        tsBp = new TestSubscriber<Object>(0L);
    }
    
    @Test
    public void testSimple() {
        Observable.range(1, 100).concatMapEager(toJust).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertValueCount(100);
        ts.assertCompleted();
    }

    @Test
    public void testSimple2() {
        Observable.range(1, 100).concatMapEager(toRange).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertValueCount(200);
        ts.assertCompleted();
    }
    
    @Test
    public void testEagerness2() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                count.getAndIncrement();
            }
        });
        
        Observable.concatEager(source, source).subscribe(tsBp);
        
        Assert.assertEquals(2, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotCompleted();
        tsBp.assertNoValues();
        
        tsBp.requestMore(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertCompleted();
    }
    
    @Test
    public void testEagerness3() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                count.getAndIncrement();
            }
        });
        
        Observable.concatEager(source, source, source).subscribe(tsBp);
        
        Assert.assertEquals(3, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotCompleted();
        tsBp.assertNoValues();
        
        tsBp.requestMore(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertCompleted();
    }

    @Test
    public void testEagerness4() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                count.getAndIncrement();
            }
        });
        
        Observable.concatEager(source, source, source, source).subscribe(tsBp);
        
        Assert.assertEquals(4, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotCompleted();
        tsBp.assertNoValues();
        
        tsBp.requestMore(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertCompleted();
    }

    @Test
    public void testEagerness5() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                count.getAndIncrement();
            }
        });
        
        Observable.concatEager(source, source, source, source, source).subscribe(tsBp);
        
        Assert.assertEquals(5, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotCompleted();
        tsBp.assertNoValues();
        
        tsBp.requestMore(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertCompleted();
    }

    @Test
    public void testEagerness6() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                count.getAndIncrement();
            }
        });
        
        Observable.concatEager(source, source, source, source, source, source).subscribe(tsBp);
        
        Assert.assertEquals(6, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotCompleted();
        tsBp.assertNoValues();
        
        tsBp.requestMore(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertCompleted();
    }

    @Test
    public void testEagerness7() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                count.getAndIncrement();
            }
        });
        
        Observable.concatEager(source, source, source, source, source, source, source).subscribe(tsBp);
        
        Assert.assertEquals(7, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotCompleted();
        tsBp.assertNoValues();
        
        tsBp.requestMore(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertCompleted();
    }

    @Test
    public void testEagerness8() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                count.getAndIncrement();
            }
        });
        
        Observable.concatEager(source, source, source, source, source, source, source, source).subscribe(tsBp);
        
        Assert.assertEquals(8, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotCompleted();
        tsBp.assertNoValues();
        
        tsBp.requestMore(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertCompleted();
    }

    @Test
    public void testEagerness9() {
        final AtomicInteger count = new AtomicInteger();
        Observable<Integer> source = Observable.just(1).doOnNext(new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                count.getAndIncrement();
            }
        });
        
        Observable.concatEager(source, source, source, source, source, source, source, source, source).subscribe(tsBp);
        
        Assert.assertEquals(9, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotCompleted();
        tsBp.assertNoValues();
        
        tsBp.requestMore(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertCompleted();
    }

    @Test
    public void testMainError() {
        Observable.<Integer>error(new TestException()).concatMapEager(toJust).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }
    
    @Test
    public void testInnerError() {
        Observable.concatEager(Observable.just(1), Observable.error(new TestException())).subscribe(ts);
        
        ts.assertValue(1);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }
    
    @Test
    public void testInnerEmpty() {
        Observable.concatEager(Observable.empty(), Observable.empty()).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void testMapperThrows() {
        Observable.just(1).concatMapEager(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t) {
                throw new TestException();
            } 
        }).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNotCompleted();
        ts.assertError(TestException.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCapacityHint() {
        Observable.just(1).concatMapEager(toJust, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMaxConcurrent() {
        Observable.just(1).concatMapEager(toJust, RxRingBuffer.SIZE, 0);
    }
    
    @Test
    public void testBackpressure() {
        Observable.concatEager(Observable.just(1), Observable.just(1)).subscribe(tsBp);

        tsBp.assertNoErrors();
        tsBp.assertNoValues();
        tsBp.assertNotCompleted();
        
        tsBp.requestMore(1);
        tsBp.assertValue(1);
        tsBp.assertNoErrors();
        tsBp.assertNotCompleted();
        
        tsBp.requestMore(1);
        tsBp.assertValues(1, 1);
        tsBp.assertNoErrors();
        tsBp.assertCompleted();
    }
    
    @Test
    public void testAsynchronousRun() {
        Observable.range(1, 2).concatMapEager(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t) {
                return Observable.range(1, 1000).subscribeOn(Schedulers.computation());
            }
        }).observeOn(Schedulers.newThread()).subscribe(ts);
        
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        ts.assertValueCount(2000);
    }
    
    @Test
    public void testReentrantWork() {
        final PublishSubject<Integer> subject = PublishSubject.create();
        
        final AtomicBoolean once = new AtomicBoolean();
        
        subject.concatMapEager(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t) {
                return Observable.just(t);
            }
        })
        .doOnNext(new Action1<Integer>() {
            @Override
            public void call(Integer t) {
                if (once.compareAndSet(false, true)) {
                    subject.onNext(2);
                }
            }
        })
        .subscribe(ts);
        
        subject.onNext(1);
        
        ts.assertNoErrors();
        ts.assertNotCompleted();
        ts.assertValues(1, 2);
    }
    
    @Test
    public void testPrefetchIsBounded() {
        final AtomicInteger count = new AtomicInteger();
        
        TestSubscriber<Object> ts = TestSubscriber.create(0);
        
        Observable.just(1).concatMapEager(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t) {
                return Observable.range(1, RxRingBuffer.SIZE * 2)
                        .doOnNext(new Action1<Integer>() {
                            @Override
                            public void call(Integer t) {
                                count.getAndIncrement();
                            }
                        });
            }
        }).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertNotCompleted();
        Assert.assertEquals(RxRingBuffer.SIZE, count.get());
    }
    
    @Test
    public void testInnerNull() {
        Observable.just(1).concatMapEager(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t) {
                return Observable.just(null);
            }
        }).subscribe(ts);

        ts.assertNoErrors();
        ts.assertCompleted();
        ts.assertValue(null);
    }


    @Test
    public void testMaxConcurrent5() {
        final List<Long> requests = new ArrayList<Long>();
        Observable.range(1, 100).doOnRequest(new Action1<Long>() {
            @Override
            public void call(Long reqCount) {
                requests.add(reqCount);
            }
        }).concatMapEager(toJust, RxRingBuffer.SIZE, 5).subscribe(ts);

        ts.assertNoErrors();
        ts.assertValueCount(100);
        ts.assertCompleted();

        Assert.assertEquals(5, (long) requests.get(0));
        Assert.assertEquals(1, (long) requests.get(1));
        Assert.assertEquals(1, (long) requests.get(2));
        Assert.assertEquals(1, (long) requests.get(3));
        Assert.assertEquals(1, (long) requests.get(4));
        Assert.assertEquals(1, (long) requests.get(5));
    }
}
