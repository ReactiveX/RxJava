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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import io.reactivex.Flowable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableFlattenIterableTest {

    @Test
    public void normal0() {
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Flowable.range(1, 2)
        .reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) {
                return Math.max(a, b);
            }
        })
        .flatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) {
                return Arrays.asList(v, v + 1);
            }
        })
        .subscribe(ts);
        
        ts.assertValues(2, 3)
        .assertNoErrors()
        .assertComplete();
    }
    
    final Function<Integer, Iterable<Integer>> mapper = new Function<Integer, Iterable<Integer>>() {
        @Override
        public Iterable<Integer> apply(Integer v) {
            return Arrays.asList(v, v + 1);
        }
    };
    
    @Test
    public void normal() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Flowable.range(1, 5).concatMapIterable(mapper)
        .subscribe(ts);
        
        ts.assertValues(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void normalViaFlatMap() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Flowable.range(1, 5).flatMapIterable(mapper)
        .subscribe(ts);
        
        ts.assertValues(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
        ts.assertNoErrors();
        ts.assertComplete();
    }
    
    @Test
    public void normalBackpressured() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);
        
        Flowable.range(1, 5).concatMapIterable(mapper)
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        ts.request(1);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        ts.request(2);
        
        ts.assertValues(1, 2, 2);
        ts.assertNoErrors();
        ts.assertNotComplete();
        
        ts.request(7);
        
        ts.assertValues(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void longRunning() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        int n = 1000 * 1000;
        
        Flowable.range(1, n).concatMapIterable(mapper)
        .subscribe(ts);

        ts.assertValueCount(n * 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void asIntermediate() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        int n = 1000 * 1000;
        
        Flowable.range(1, n).concatMapIterable(mapper).concatMap(new Function<Integer, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Integer v) {
                return Flowable.just(v);
            }
        })
        .subscribe(ts);

        ts.assertValueCount(n * 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void just() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Flowable.just(1).concatMapIterable(mapper)
        .subscribe(ts);
        
        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void justHidden() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Flowable.just(1).hide().concatMapIterable(mapper)
        .subscribe(ts);
        
        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void empty() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Flowable.<Integer>empty().concatMapIterable(mapper)
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertComplete();
    }
    
    @Test
    public void error() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Flowable.<Integer>just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .concatMapIterable(mapper)
        .subscribe(ts);
        
        ts.assertValues(1, 2);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void iteratorHasNextThrowsImmediately() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        final Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        throw new TestException();
                    }
                    
                    @Override
                    public Integer next() {
                        return 1;
                    }
                    
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
        
        Flowable.range(1, 2)
        .concatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) {
                return it;
            }
        })
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void iteratorHasNextThrowsImmediatelyJust() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        final Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        throw new TestException();
                    }
                    
                    @Override
                    public Integer next() {
                        return 1;
                    }
                    
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
        
        Flowable.just(1)
        .concatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) {
                return it;
            }
        })
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void iteratorHasNextThrowsSecondCall() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        final Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int count;
                    @Override
                    public boolean hasNext() {
                        if (++count >= 2) {
                            throw new TestException();
                        }
                        return true;
                    }
                    
                    @Override
                    public Integer next() {
                        return 1;
                    }
                    
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
        
        Flowable.range(1, 2)
        .concatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) {
                return it;
            }
        })
        .subscribe(ts);
        
        ts.assertValue(1);
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void iteratorNextThrows() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        final Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }
                    
                    @Override
                    public Integer next() {
                        throw new TestException();
                    }
                    
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
        
        Flowable.range(1, 2)
        .concatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) {
                return it;
            }
        })
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void iteratorNextThrowsAndUnsubscribes() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        final Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }
                    
                    @Override
                    public Integer next() {
                        throw new TestException();
                    }
                    
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
        
        PublishProcessor<Integer> ps = PublishProcessor.create();
        
        ps
        .concatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) {
                return it;
            }
        })
        .subscribe(ts);
        
        ps.onNext(1);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
        
        Assert.assertFalse("PublishProcessor has Subscribers?!", ps.hasSubscribers());
    }

    @Test
    public void mixture() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Flowable.range(0, 1000)
        .concatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) {
                return (v % 2) == 0 ? Collections.singleton(1) : Collections.<Integer>emptySet();
            }
        })
        .subscribe(ts);
        
        ts.assertValueCount(500);
        ts.assertNoErrors();
        ts.assertComplete();
    }
    
    @Test
    public void emptyInnerThenSingleBackpressured() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1);
        
        Flowable.range(1, 2)
        .concatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) {
                return v == 2 ? Collections.singleton(1) : Collections.<Integer>emptySet();
            }
        })
        .subscribe(ts);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }
    
    @Test
    public void manyEmptyInnerThenSingleBackpressured() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1);
        
        Flowable.range(1, 1000)
        .concatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) {
                return v == 1000 ? Collections.singleton(1) : Collections.<Integer>emptySet();
            }
        })
        .subscribe(ts);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
    }
    
    @Test
    public void hasNextIsNotCalledAfterChildUnsubscribedOnNext() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        final AtomicInteger counter = new AtomicInteger();
        
        final Iterable<Integer> it = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        counter.getAndIncrement();
                        return true;
                    }
                    
                    @Override
                    public Integer next() {
                        return 1;
                    }
                    
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
        
        PublishProcessor<Integer> ps = PublishProcessor.create();
        
        ps
        .concatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) {
                return it;
            }
        })
        .take(1)
        .subscribe(ts);
        
        ps.onNext(1);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertComplete();
        
        Assert.assertFalse("PublishProcessor has Subscribers?!", ps.hasSubscribers());
        Assert.assertEquals(1, counter.get());
    }
    
    @Test
    public void normalPrefetchViaFlatMap() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Flowable.range(1, 5).flatMapIterable(mapper, 2)
        .subscribe(ts);
        
        ts.assertValues(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
        ts.assertNoErrors();
        ts.assertComplete();
    }
    
    @Test
    public void withResultSelectorMaxConcurrent() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Flowable.range(1, 5)
        .flatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) {
                return Collections.singletonList(1);
            }
        }, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer a, Integer b) {
                return a * 10 + b;
            }
        }, 2)
        .subscribe(ts)
        ;
        
        ts.assertValues(11, 21, 31, 41, 51);
        ts.assertNoErrors();
        ts.assertComplete();
    }
}
