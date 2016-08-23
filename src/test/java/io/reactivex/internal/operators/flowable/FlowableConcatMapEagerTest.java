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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableConcatMapEagerTest {

    @Test
    public void normal() {
        Flowable.range(1, 5)
        .concatMapEager(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer t) {
                return Flowable.range(t, 2);
            }
        })
        .test()
        .assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }
    
    @Test
    public void normalBackpressured() {
        TestSubscriber<Integer> ts = Flowable.range(1, 5)
        .concatMapEager(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer t) {
                return Flowable.range(t, 2);
            }
        })
        .test(3);
        
        ts.assertValues(1, 2, 2);
        
        ts.request(1);
        
        ts.assertValues(1, 2, 2, 3);
        
        ts.request(1);

        ts.assertValues(1, 2, 2, 3, 3);

        ts.request(5);

        ts.assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }
    
    @Test
    public void normalDelayBoundary() {
        Flowable.range(1, 5)
        .concatMapEagerDelayError(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer t) {
                return Flowable.range(t, 2);
            }
        }, false)
        .test()
        .assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }
    
    @Test
    public void normalDelayBoundaryBackpressured() {
        TestSubscriber<Integer> ts = Flowable.range(1, 5)
        .concatMapEagerDelayError(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer t) {
                return Flowable.range(t, 2);
            }
        }, false)
        .test(3);
        
        ts.assertValues(1, 2, 2);
        
        ts.request(1);
        
        ts.assertValues(1, 2, 2, 3);
        
        ts.request(1);

        ts.assertValues(1, 2, 2, 3, 3);

        ts.request(5);

        ts.assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }
    
    @Test
    public void normalDelayEnd() {
        Flowable.range(1, 5)
        .concatMapEagerDelayError(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer t) {
                return Flowable.range(t, 2);
            }
        }, true)
        .test()
        .assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }
    
    @Test
    public void normalDelayEndBackpressured() {
        TestSubscriber<Integer> ts = Flowable.range(1, 5)
        .concatMapEagerDelayError(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer t) {
                return Flowable.range(t, 2);
            }
        }, true)
        .test(3);
        
        ts.assertValues(1, 2, 2);
        
        ts.request(1);
        
        ts.assertValues(1, 2, 2, 3);
        
        ts.request(1);

        ts.assertValues(1, 2, 2, 3, 3);

        ts.request(5);

        ts.assertResult(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
    }
    
    @Test
    public void mainErrorsDelayBoundary() {
        PublishProcessor<Integer> main = PublishProcessor.create();
        final PublishProcessor<Integer> inner = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = main.concatMapEagerDelayError(
                new Function<Integer, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Integer t) {
                        return inner;
                    }
                }, false).test();
        
        main.onNext(1);
        
        inner.onNext(2);
        
        ts.assertValue(2);
        
        main.onError(new TestException("Forced failure"));
        
        ts.assertNoErrors();
        
        inner.onNext(3);
        inner.onComplete();
        
        ts.assertFailureAndMessage(TestException.class, "Forced failure", 2, 3);
    }

    @Test
    public void mainErrorsDelayEnd() {
        PublishProcessor<Integer> main = PublishProcessor.create();
        final PublishProcessor<Integer> inner = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = main.concatMapEagerDelayError(
                new Function<Integer, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Integer t) {
                        return inner;
                    }
                }, true).test();
        
        main.onNext(1);
        main.onNext(2);
        
        inner.onNext(2);
        
        ts.assertValue(2);
        
        main.onError(new TestException("Forced failure"));
        
        ts.assertNoErrors();
        
        inner.onNext(3);
        inner.onComplete();
        
        ts.assertFailureAndMessage(TestException.class, "Forced failure", 2, 3, 2, 3);
    }
    
    @Test
    public void mainErrorsImmediate() {
        PublishProcessor<Integer> main = PublishProcessor.create();
        final PublishProcessor<Integer> inner = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = main.concatMapEager(
                new Function<Integer, Publisher<Integer>>() {
                    @Override
                    public Publisher<Integer> apply(Integer t) {
                        return inner;
                    }
                }).test();
        
        main.onNext(1);
        main.onNext(2);
        
        inner.onNext(2);
        
        ts.assertValue(2);
        
        main.onError(new TestException("Forced failure"));

        assertFalse("inner has subscribers?", inner.hasSubscribers());
        
        inner.onNext(3);
        inner.onComplete();
        
        ts.assertFailureAndMessage(TestException.class, "Forced failure", 2);
    }
    
    @Test
    public void longEager() {
        
        Flowable.range(1, 2 * Flowable.bufferSize())
        .concatMapEager(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) {
                return Flowable.just(1);
            }
        })
        .test()
        .assertValueCount(2 * Flowable.bufferSize())
        .assertNoErrors()
        .assertComplete();
    }
    
    TestSubscriber<Object> ts;
    TestSubscriber<Object> tsBp;
    
    Function<Integer, Flowable<Integer>> toJust = new Function<Integer, Flowable<Integer>>() {
        @Override
        public Flowable<Integer> apply(Integer t) {
            return Flowable.just(t);
        }
    };

    Function<Integer, Flowable<Integer>> toRange = new Function<Integer, Flowable<Integer>>() {
        @Override
        public Flowable<Integer> apply(Integer t) {
            return Flowable.range(t, 2);
        }
    };

    @Before
    public void before() {
        ts = new TestSubscriber<Object>();
        tsBp = new TestSubscriber<Object>(0L);
    }
    
    @Test
    public void testSimple() {
        Flowable.range(1, 100).concatMapEager(toJust).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertValueCount(100);
        ts.assertComplete();
    }

    @Test
    public void testSimple2() {
        Flowable.range(1, 100).concatMapEager(toRange).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertValueCount(200);
        ts.assertComplete();
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness2() {
        final AtomicInteger count = new AtomicInteger();
        Flowable<Integer> source = Flowable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();
        
        Flowable.concatArrayEager(source, source).subscribe(tsBp);
        
        Assert.assertEquals(2, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotComplete();
        tsBp.assertNoValues();
        
        tsBp.request(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertComplete();
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness3() {
        final AtomicInteger count = new AtomicInteger();
        Flowable<Integer> source = Flowable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();
        
        Flowable.concatArrayEager(source, source, source).subscribe(tsBp);
        
        Assert.assertEquals(3, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotComplete();
        tsBp.assertNoValues();
        
        tsBp.request(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness4() {
        final AtomicInteger count = new AtomicInteger();
        Flowable<Integer> source = Flowable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();
        
        Flowable.concatArrayEager(source, source, source, source).subscribe(tsBp);
        
        Assert.assertEquals(4, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotComplete();
        tsBp.assertNoValues();
        
        tsBp.request(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness5() {
        final AtomicInteger count = new AtomicInteger();
        Flowable<Integer> source = Flowable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();
        
        Flowable.concatArrayEager(source, source, source, source, source).subscribe(tsBp);
        
        Assert.assertEquals(5, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotComplete();
        tsBp.assertNoValues();
        
        tsBp.request(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness6() {
        final AtomicInteger count = new AtomicInteger();
        Flowable<Integer> source = Flowable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();
        
        Flowable.concatArrayEager(source, source, source, source, source, source).subscribe(tsBp);
        
        Assert.assertEquals(6, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotComplete();
        tsBp.assertNoValues();
        
        tsBp.request(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness7() {
        final AtomicInteger count = new AtomicInteger();
        Flowable<Integer> source = Flowable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();
        
        Flowable.concatArrayEager(source, source, source, source, source, source, source).subscribe(tsBp);
        
        Assert.assertEquals(7, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotComplete();
        tsBp.assertNoValues();
        
        tsBp.request(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness8() {
        final AtomicInteger count = new AtomicInteger();
        Flowable<Integer> source = Flowable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();
        
        Flowable.concatArrayEager(source, source, source, source, source, source, source, source).subscribe(tsBp);
        
        Assert.assertEquals(8, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotComplete();
        tsBp.assertNoValues();
        
        tsBp.request(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEagerness9() {
        final AtomicInteger count = new AtomicInteger();
        Flowable<Integer> source = Flowable.just(1).doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                count.getAndIncrement();
            }
        }).hide();
        
        Flowable.concatArrayEager(source, source, source, source, source, source, source, source, source).subscribe(tsBp);
        
        Assert.assertEquals(9, count.get());
        tsBp.assertNoErrors();
        tsBp.assertNotComplete();
        tsBp.assertNoValues();
        
        tsBp.request(Long.MAX_VALUE);
        
        tsBp.assertValueCount(count.get());
        tsBp.assertNoErrors();
        tsBp.assertComplete();
    }

    @Test
    public void testMainError() {
        Flowable.<Integer>error(new TestException()).concatMapEager(toJust).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testInnerError() {
        Flowable.concatArrayEager(Flowable.just(1), Flowable.error(new TestException())).subscribe(ts);
        
        ts.assertValue(1);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testInnerEmpty() {
        Flowable.concatArrayEager(Flowable.empty(), Flowable.empty()).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertComplete();
    }
    
    @Test
    public void testMapperThrows() {
        Flowable.just(1).concatMapEager(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t) {
                throw new TestException();
            } 
        }).subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCapacityHint() {
        Flowable.just(1).concatMapEager(toJust, 0, Flowable.bufferSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMaxConcurrent() {
        Flowable.just(1).concatMapEager(toJust, Flowable.bufferSize(), 0);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testBackpressure() {
        Flowable.concatArrayEager(Flowable.just(1), Flowable.just(1)).subscribe(tsBp);

        tsBp.assertNoErrors();
        tsBp.assertNoValues();
        tsBp.assertNotComplete();
        
        tsBp.request(1);
        tsBp.assertValue(1);
        tsBp.assertNoErrors();
        tsBp.assertNotComplete();
        
        tsBp.request(1);
        tsBp.assertValues(1, 1);
        tsBp.assertNoErrors();
        tsBp.assertComplete();
    }
    
    @Test
    public void testAsynchronousRun() {
        Flowable.range(1, 2).concatMapEager(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t) {
                return Flowable.range(1, 1000).subscribeOn(Schedulers.computation());
            }
        }).observeOn(Schedulers.newThread()).subscribe(ts);
        
        ts.awaitTerminalEvent(5, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertValueCount(2000);
    }
    
    @Test
    public void testReentrantWork() {
        final PublishProcessor<Integer> subject = PublishProcessor.create();
        
        final AtomicBoolean once = new AtomicBoolean();
        
        subject.concatMapEager(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t) {
                return Flowable.just(t);
            }
        })
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                if (once.compareAndSet(false, true)) {
                    subject.onNext(2);
                }
            }
        })
        .subscribe(ts);
        
        subject.onNext(1);
        
        ts.assertNoErrors();
        ts.assertNotComplete();
        ts.assertValues(1, 2);
    }
    
    @Test
    public void testPrefetchIsBounded() {
        final AtomicInteger count = new AtomicInteger();
        
        TestSubscriber<Object> ts = TestSubscriber.create(0);
        
        Flowable.just(1).concatMapEager(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t) {
                return Flowable.range(1, Flowable.bufferSize() * 2)
                        .doOnNext(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer t) {
                                count.getAndIncrement();
                            }
                        }).hide();
            }
        }).subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertNoValues();
        ts.assertNotComplete();
        Assert.assertEquals(Flowable.bufferSize(), count.get());
    }
    
    @Test
    @Ignore("Null values are not allowed in RS")
    public void testInnerNull() {
        Flowable.just(1).concatMapEager(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer t) {
                return Flowable.just(null);
            }
        }).subscribe(ts);

        ts.assertNoErrors();
        ts.assertComplete();
        ts.assertValue(null);
    }


    @Test
    public void testMaxConcurrent5() {
        final List<Long> requests = new ArrayList<Long>();
        Flowable.range(1, 100).doOnRequest(new LongConsumer() {
            @Override
            public void accept(long reqCount) {
                requests.add(reqCount);
            }
        }).concatMapEager(toJust, 5, Flowable.bufferSize()).subscribe(ts);

        ts.assertNoErrors();
        ts.assertValueCount(100);
        ts.assertComplete();

        Assert.assertEquals(5, (long) requests.get(0));
        Assert.assertEquals(1, (long) requests.get(1));
        Assert.assertEquals(1, (long) requests.get(2));
        Assert.assertEquals(1, (long) requests.get(3));
        Assert.assertEquals(1, (long) requests.get(4));
        Assert.assertEquals(1, (long) requests.get(5));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    @Ignore("Currently there are no 2-9 argument variants, use concatArrayEager()")
    public void many() throws Exception {
        for (int i = 2; i < 10; i++) {
            Class<?>[] clazz = new Class[i];
            Arrays.fill(clazz, Flowable.class);
            
            Flowable<Integer>[] obs = new Flowable[i];
            Arrays.fill(obs, Flowable.just(1));
            
            Integer[] expected = new Integer[i];
            Arrays.fill(expected, 1);
            
            Method m = Flowable.class.getMethod("concatEager", clazz);
            
            TestSubscriber<Integer> ts = TestSubscriber.create();
            
            ((Flowable<Integer>)m.invoke(null, (Object[])obs)).subscribe(ts);
            
            ts.assertValues(expected);
            ts.assertNoErrors();
            ts.assertComplete();
        }
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void capacityHint() {
        Flowable<Integer> source = Flowable.just(1);
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.concatEager(Arrays.asList(source, source, source), 1, 1).subscribe(ts);
        
        ts.assertValues(1, 1, 1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void flowable() {
        Flowable<Integer> source = Flowable.just(1);
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.concatEager(Flowable.just(source, source, source)).subscribe(ts);
        
        ts.assertValues(1, 1, 1);
        ts.assertNoErrors();
        ts.assertComplete();
    }
    
    @Test
    public void flowableCapacityHint() {
        Flowable<Integer> source = Flowable.just(1);
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.concatEager(Flowable.just(source, source, source), 1, 1).subscribe(ts);
        
        ts.assertValues(1, 1, 1);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void badCapacityHint() throws Exception {
        Flowable<Integer> source = Flowable.just(1);
        try {
            Flowable.concatEager(Arrays.asList(source, source, source), 1, -99);
        } catch (IllegalArgumentException ex) {
            assertEquals("prefetch > 0 required but it was -99", ex.getMessage());
        }
        
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void mappingBadCapacityHint() throws Exception {
        Flowable<Integer> source = Flowable.just(1);
        try {
            Flowable.just(source, source, source).concatMapEager((Function)Functions.identity(), 10, -99);
        } catch (IllegalArgumentException ex) {
            assertEquals("prefetch > 0 required but it was -99", ex.getMessage());
        }
        
    }
}
