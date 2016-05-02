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
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;

import rx.Observable;
import rx.Observer;
import rx.exceptions.TestException;
import rx.functions.*;
import rx.internal.util.RxRingBuffer;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

public class OperatorFlatMapTest {
    @Test
    public void testNormal() {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        final List<Integer> list = Arrays.asList(1, 2, 3);

        Func1<Integer, List<Integer>> func = new Func1<Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer t1) {
                return list;
            }
        };
        Func2<Integer, Integer, Integer> resFunc = new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.from(source).flatMapIterable(func, resFunc).subscribe(o);

        for (Integer s : source) {
            for (Integer v : list) {
                verify(o).onNext(s | v);
            }
        }
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testCollectionFunctionThrows() {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Func1<Integer, List<Integer>> func = new Func1<Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer t1) {
                throw new TestException();
            }
        };
        Func2<Integer, Integer, Integer> resFunc = new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.from(source).flatMapIterable(func, resFunc).subscribe(o);

        verify(o, never()).onCompleted();
        verify(o, never()).onNext(any());
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testResultFunctionThrows() {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        final List<Integer> list = Arrays.asList(1, 2, 3);

        Func1<Integer, List<Integer>> func = new Func1<Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer t1) {
                return list;
            }
        };
        Func2<Integer, Integer, Integer> resFunc = new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                throw new TestException();
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.from(source).flatMapIterable(func, resFunc).subscribe(o);

        verify(o, never()).onCompleted();
        verify(o, never()).onNext(any());
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testMergeError() {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Func1<Integer, Observable<Integer>> func = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return Observable.error(new TestException());
            }
        };
        Func2<Integer, Integer, Integer> resFunc = new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.from(source).flatMap(func, resFunc).subscribe(o);

        verify(o, never()).onCompleted();
        verify(o, never()).onNext(any());
        verify(o).onError(any(TestException.class));
    }

    <T, R> Func1<T, R> just(final R value) {
        return new Func1<T, R>() {

            @Override
            public R call(T t1) {
                return value;
            }
        };
    }

    <R> Func0<R> just0(final R value) {
        return new Func0<R>() {

            @Override
            public R call() {
                return value;
            }
        };
    }

    @Test
    public void testFlatMapTransformsNormal() {
        Observable<Integer> onNext = Observable.from(Arrays.asList(1, 2, 3));
        Observable<Integer> onCompleted = Observable.from(Arrays.asList(4));
        Observable<Integer> onError = Observable.from(Arrays.asList(5));

        Observable<Integer> source = Observable.from(Arrays.asList(10, 20, 30));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.flatMap(just(onNext), just(onError), just0(onCompleted)).subscribe(o);

        verify(o, times(3)).onNext(1);
        verify(o, times(3)).onNext(2);
        verify(o, times(3)).onNext(3);
        verify(o).onNext(4);
        verify(o).onCompleted();

        verify(o, never()).onNext(5);
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testFlatMapTransformsException() {
        Observable<Integer> onNext = Observable.from(Arrays.asList(1, 2, 3));
        Observable<Integer> onCompleted = Observable.from(Arrays.asList(4));
        Observable<Integer> onError = Observable.from(Arrays.asList(5));

        Observable<Integer> source = Observable.concat(
                Observable.from(Arrays.asList(10, 20, 30)),
                Observable.<Integer> error(new RuntimeException("Forced failure!"))
                );
        

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.flatMap(just(onNext), just(onError), just0(onCompleted)).subscribe(o);

        verify(o, times(3)).onNext(1);
        verify(o, times(3)).onNext(2);
        verify(o, times(3)).onNext(3);
        verify(o).onNext(5);
        verify(o).onCompleted();
        verify(o, never()).onNext(4);

        verify(o, never()).onError(any(Throwable.class));
    }

    <R> Func0<R> funcThrow0(R r) {
        return new Func0<R>() {
            @Override
            public R call() {
                throw new TestException();
            }
        };
    }

    <T, R> Func1<T, R> funcThrow(T t, R r) {
        return new Func1<T, R>() {
            @Override
            public R call(T t) {
                throw new TestException();
            }
        };
    }

    @Test
    public void testFlatMapTransformsOnNextFuncThrows() {
        Observable<Integer> onCompleted = Observable.from(Arrays.asList(4));
        Observable<Integer> onError = Observable.from(Arrays.asList(5));

        Observable<Integer> source = Observable.from(Arrays.asList(10, 20, 30));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.flatMap(funcThrow(1, onError), just(onError), just0(onCompleted)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
    }

    @Test
    public void testFlatMapTransformsOnErrorFuncThrows() {
        Observable<Integer> onNext = Observable.from(Arrays.asList(1, 2, 3));
        Observable<Integer> onCompleted = Observable.from(Arrays.asList(4));
        Observable<Integer> onError = Observable.from(Arrays.asList(5));

        Observable<Integer> source = Observable.error(new TestException());

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.flatMap(just(onNext), funcThrow((Throwable) null, onError), just0(onCompleted)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
    }

    @Test
    public void testFlatMapTransformsOnCompletedFuncThrows() {
        Observable<Integer> onNext = Observable.from(Arrays.asList(1, 2, 3));
        Observable<Integer> onCompleted = Observable.from(Arrays.asList(4));
        Observable<Integer> onError = Observable.from(Arrays.asList(5));

        Observable<Integer> source = Observable.from(Arrays.<Integer> asList());

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.flatMap(just(onNext), just(onError), funcThrow0(onCompleted)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
    }

    @Test
    public void testFlatMapTransformsMergeException() {
        Observable<Integer> onNext = Observable.error(new TestException());
        Observable<Integer> onCompleted = Observable.from(Arrays.asList(4));
        Observable<Integer> onError = Observable.from(Arrays.asList(5));

        Observable<Integer> source = Observable.from(Arrays.asList(10, 20, 30));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.flatMap(just(onNext), just(onError), funcThrow0(onCompleted)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
    }

    private static <T> Observable<T> compose(Observable<T> source, final AtomicInteger subscriptionCount, final int m) {
        return source.doOnSubscribe(new Action0() {
            @Override
            public void call() {
                if (subscriptionCount.getAndIncrement() >= m) {
                    Assert.fail("Too many subscriptions! " + subscriptionCount.get());
                }
            }
        }).doOnCompleted(new Action0() {
            @Override
            public void call() {
                if (subscriptionCount.decrementAndGet() < 0) {
                    Assert.fail("Too many unsubscriptions! " + subscriptionCount.get());
                }
            }
        });
    }

    @Test
    public void testFlatMapMaxConcurrent() {
        final int m = 4;
        final AtomicInteger subscriptionCount = new AtomicInteger();
        Observable<Integer> source = Observable.range(1, 10).flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return compose(Observable.range(t1 * 10, 2), subscriptionCount, m)
                        .subscribeOn(Schedulers.computation());
            }
        }, m);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        source.subscribe(ts);
        
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        Set<Integer> expected = new HashSet<Integer>(Arrays.asList(
                10, 11, 20, 21, 30, 31, 40, 41, 50, 51, 60, 61, 70, 71, 80, 81, 90, 91, 100, 101
        ));
        Assert.assertEquals(expected.size(), ts.getOnNextEvents().size());
        Assert.assertTrue(expected.containsAll(ts.getOnNextEvents()));
    }
    @Test
    public void testFlatMapSelectorMaxConcurrent() {
        final int m = 4;
        final AtomicInteger subscriptionCount = new AtomicInteger();
        Observable<Integer> source = Observable.range(1, 10).flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return compose(Observable.range(t1 * 10, 2), subscriptionCount, m)
                        .subscribeOn(Schedulers.computation());
            }
        }, new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 * 1000 + t2;
            }
        }, m);
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        source.subscribe(ts);
        
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        Set<Integer> expected = new HashSet<Integer>(Arrays.asList(
                1010, 1011, 2020, 2021, 3030, 3031, 4040, 4041, 5050, 5051, 
                6060, 6061, 7070, 7071, 8080, 8081, 9090, 9091, 10100, 10101
        ));
        Assert.assertEquals(expected.size(), ts.getOnNextEvents().size());
        System.out.println("--> testFlatMapSelectorMaxConcurrent: " + ts.getOnNextEvents());
        Assert.assertTrue(expected.containsAll(ts.getOnNextEvents()));
    }
    
    @Test
    public void testFlatMapTransformsMaxConcurrentNormalLoop() {
        for (int i = 0; i < 1000; i++) {
            if (i % 100 == 0) {
                System.out.println("testFlatMapTransformsMaxConcurrentNormalLoop => " + i);
            }
            testFlatMapTransformsMaxConcurrentNormal();
        }
    }
    
    @Test
    public void testFlatMapTransformsMaxConcurrentNormal() {
        final int m = 2;
        final AtomicInteger subscriptionCount = new AtomicInteger();
        Observable<Integer> onNext = 
                compose(Observable.from(Arrays.asList(1, 2, 3)).observeOn(Schedulers.computation()), subscriptionCount, m)
                .subscribeOn(Schedulers.computation());
        Observable<Integer> onCompleted = compose(Observable.from(Arrays.asList(4)), subscriptionCount, m)
                .subscribeOn(Schedulers.computation());
        Observable<Integer> onError = Observable.from(Arrays.asList(5));

        Observable<Integer> source = Observable.from(Arrays.asList(10, 20, 30));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        TestSubscriber<Object> ts = new TestSubscriber<Object>(o);

        source.flatMap(just(onNext), just(onError), just0(onCompleted), m).subscribe(ts);
        
        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertTerminalEvent();

        verify(o, times(3)).onNext(1);
        verify(o, times(3)).onNext(2);
        verify(o, times(3)).onNext(3);
        verify(o).onNext(4);
        verify(o).onCompleted();

        verify(o, never()).onNext(5);
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Ignore // don't care for any reordering
    @Test(timeout = 10000)
    public void flatMapRangeAsyncLoop() {
        for (int i = 0; i < 2000; i++) {
            if (i % 10 == 0) {
                System.out.println("flatMapRangeAsyncLoop > " + i);
            }
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            Observable.range(0, 1000)
            .flatMap(new Func1<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> call(Integer t) {
                    return Observable.just(t);
                }
            })
            .observeOn(Schedulers.computation())
            .subscribe(ts);

            ts.awaitTerminalEvent(2500, TimeUnit.MILLISECONDS);
            if (ts.getOnCompletedEvents().isEmpty()) {
                System.out.println(ts.getOnNextEvents().size());
            }
            ts.assertTerminalEvent();
            ts.assertNoErrors();
            List<Integer> list = ts.getOnNextEvents();
            assertEquals(1000, list.size());
            boolean f = false;
            for (int j = 0; j < list.size(); j++) {
                if (list.get(j) != j) {
                    System.out.println(j + " " + list.get(j));
                    f = true;
                }
            }
            if (f) {
                Assert.fail("Results are out of order!");
            }
        }
    }
    @Test(timeout = 30000)
    public void flatMapRangeMixedAsyncLoop() {
        for (int i = 0; i < 2000; i++) {
            if (i % 10 == 0) {
                System.out.println("flatMapRangeAsyncLoop > " + i);
            }
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            Observable.range(0, 1000)
            .flatMap(new Func1<Integer, Observable<Integer>>() {
                final Random rnd = new Random();
                @Override
                public Observable<Integer> call(Integer t) {
                    Observable<Integer> r = Observable.just(t);
                    if (rnd.nextBoolean()) {
                        r = r.asObservable();
                    }
                    return r;
                }
            })
            .observeOn(Schedulers.computation())
            .subscribe(ts);

            ts.awaitTerminalEvent(2500, TimeUnit.MILLISECONDS);
            if (ts.getOnCompletedEvents().isEmpty()) {
                System.out.println(ts.getOnNextEvents().size());
            }
            ts.assertTerminalEvent();
            ts.assertNoErrors();
            List<Integer> list = ts.getOnNextEvents();
            if (list.size() < 1000) {
                Set<Integer> set = new HashSet<Integer>(list);
                for (int j = 0; j < 1000; j++) {
                    if (!set.contains(j)) {
                        System.out.println(j + " missing");
                    }
                }
            }
            assertEquals(1000, list.size());
        }
    }
    
    @Test
    public void flatMapIntPassthruAsync() {
        for (int i = 0;i < 1000; i++) {
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
            
            Observable.range(1, 1000).flatMap(new Func1<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> call(Integer t) {
                    return Observable.just(1).subscribeOn(Schedulers.computation());
                }
            }).subscribe(ts);
            
            ts.awaitTerminalEvent(5, TimeUnit.SECONDS);
            ts.assertNoErrors();
            ts.assertCompleted();
            ts.assertValueCount(1000);
        }
    }
    @Test
    public void flatMapTwoNestedSync() {
        for (final int n : new int[] { 1, 1000, 1000000 }) {
            TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
    
            Observable.just(1, 2).flatMap(new Func1<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> call(Integer t) {
                    return Observable.range(1, n);
                }
            }).subscribe(ts);
            
            System.out.println("flatMapTwoNestedSync >> @ " + n);
            ts.assertNoErrors();
            ts.assertCompleted();
            ts.assertValueCount(n * 2);
        }
    }
    
    @Test
    public void justEmptyMixture() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.range(0, 4 * RxRingBuffer.SIZE)
        .flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return (v & 1) == 0 ? Observable.<Integer>empty() : Observable.just(v);
            }
        })
        .subscribe(ts);
        
        ts.assertValueCount(2 * RxRingBuffer.SIZE);
        ts.assertNoErrors();
        ts.assertCompleted();
        
        int j = 1;
        for (Integer v : ts.getOnNextEvents()) {
            Assert.assertEquals(j, v.intValue());
            
            j += 2;
        }
    }

    @Test
    public void rangeEmptyMixture() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.range(0, 4 * RxRingBuffer.SIZE)
        .flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return (v & 1) == 0 ? Observable.<Integer>empty() : Observable.range(v, 2);
            }
        })
        .subscribe(ts);
        
        ts.assertValueCount(4 * RxRingBuffer.SIZE);
        ts.assertNoErrors();
        ts.assertCompleted();
        
        int j = 1;
        List<Integer> list = ts.getOnNextEvents();
        for (int i = 0; i < list.size(); i += 2) {
            Assert.assertEquals(j, list.get(i).intValue());
            Assert.assertEquals(j + 1, list.get(i + 1).intValue());
            
            j += 2;
        }
    }

    @Test
    public void justEmptyMixtureMaxConcurrent() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.range(0, 4 * RxRingBuffer.SIZE)
        .flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return (v & 1) == 0 ? Observable.<Integer>empty() : Observable.just(v);
            }
        }, 16)
        .subscribe(ts);
        
        ts.assertValueCount(2 * RxRingBuffer.SIZE);
        ts.assertNoErrors();
        ts.assertCompleted();
        
        int j = 1;
        for (Integer v : ts.getOnNextEvents()) {
            Assert.assertEquals(j, v.intValue());
            
            j += 2;
        }
    }

    @Test
    public void rangeEmptyMixtureMaxConcurrent() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        Observable.range(0, 4 * RxRingBuffer.SIZE)
        .flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return (v & 1) == 0 ? Observable.<Integer>empty() : Observable.range(v, 2);
            }
        }, 16)
        .subscribe(ts);
        
        ts.assertValueCount(4 * RxRingBuffer.SIZE);
        ts.assertNoErrors();
        ts.assertCompleted();
        
        int j = 1;
        List<Integer> list = ts.getOnNextEvents();
        for (int i = 0; i < list.size(); i += 2) {
            Assert.assertEquals(j, list.get(i).intValue());
            Assert.assertEquals(j + 1, list.get(i + 1).intValue());
            
            j += 2;
        }
    }

}
