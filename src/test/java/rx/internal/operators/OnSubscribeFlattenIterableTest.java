/**
 * Copyright 2016 Netflix, Inc.
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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import rx.Observable;
import rx.exceptions.TestException;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OnSubscribeFlattenIterableTest {
    
    final Func1<Integer, Iterable<Integer>> mapper = new Func1<Integer, Iterable<Integer>>() {
        @Override
        public Iterable<Integer> call(Integer v) {
            return Arrays.asList(v, v + 1);
        }
    };
    
    @Test
    public void normal() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable.range(1, 5).concatMapIterable(mapper)
        .subscribe(ts);
        
        ts.assertValues(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void normalBackpressured() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0);
        
        Observable.range(1, 5).concatMapIterable(mapper)
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(1);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(2);
        
        ts.assertValues(1, 2, 2);
        ts.assertNoErrors();
        ts.assertNotCompleted();
        
        ts.requestMore(7);
        
        ts.assertValues(1, 2, 2, 3, 3, 4, 4, 5, 5, 6);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void longRunning() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        int n = 1000 * 1000;
        
        Observable.range(1, n).concatMapIterable(mapper)
        .subscribe(ts);

        ts.assertValueCount(n * 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void asIntermediate() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        int n = 1000 * 1000;
        
        Observable.range(1, n).concatMapIterable(mapper).concatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.just(v);
            }
        })
        .subscribe(ts);

        ts.assertValueCount(n * 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void just() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable.just(1).concatMapIterable(mapper)
        .subscribe(ts);
        
        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void justHidden() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable.just(1).asObservable().concatMapIterable(mapper)
        .subscribe(ts);
        
        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void empty() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable.<Integer>empty().concatMapIterable(mapper)
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void error() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable.<Integer>just(1).concatWith(Observable.<Integer>error(new TestException()))
        .concatMapIterable(mapper)
        .subscribe(ts);
        
        ts.assertValues(1, 2);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
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
        
        Observable.range(1, 2)
        .concatMapIterable(new Func1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> call(Integer v) {
                return it;
            }
        })
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
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
        
        Observable.just(1)
        .concatMapIterable(new Func1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> call(Integer v) {
                return it;
            }
        })
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
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
        
        Observable.range(1, 2)
        .concatMapIterable(new Func1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> call(Integer v) {
                return it;
            }
        })
        .subscribe(ts);
        
        ts.assertValue(1);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
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
        
        Observable.range(1, 2)
        .concatMapIterable(new Func1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> call(Integer v) {
                return it;
            }
        })
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
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
        
        PublishSubject<Integer> ps = PublishSubject.create();
        
        ps
        .concatMapIterable(new Func1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> call(Integer v) {
                return it;
            }
        })
        .unsafeSubscribe(ts);
        
        ps.onNext(1);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
        
        Assert.assertFalse("PublishSubject has Observers?!", ps.hasObservers());
    }

    @Test
    public void mixture() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        Observable.range(0, 1000)
        .concatMapIterable(new Func1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> call(Integer v) {
                return (v % 2) == 0 ? Collections.singleton(1) : Collections.<Integer>emptySet();
            }
        })
        .subscribe(ts);
        
        ts.assertValueCount(500);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void emptyInnerThenSingleBackpressured() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1);
        
        Observable.range(1, 2)
        .concatMapIterable(new Func1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> call(Integer v) {
                return v == 2 ? Collections.singleton(1) : Collections.<Integer>emptySet();
            }
        })
        .subscribe(ts);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
    
    @Test
    public void manyEmptyInnerThenSingleBackpressured() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(1);
        
        Observable.range(1, 1000)
        .concatMapIterable(new Func1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> call(Integer v) {
                return v == 1000 ? Collections.singleton(1) : Collections.<Integer>emptySet();
            }
        })
        .subscribe(ts);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
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
        
        PublishSubject<Integer> ps = PublishSubject.create();
        
        ps
        .concatMapIterable(new Func1<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> call(Integer v) {
                return it;
            }
        })
        .take(1)
        .unsafeSubscribe(ts);
        
        ps.onNext(1);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
        
        Assert.assertFalse("PublishSubject has Observers?!", ps.hasObservers());
        Assert.assertEquals(1, counter.get());
    }
}
