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

package io.reactivex.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

import org.junit.*;
import org.reactivestreams.Subscriber;

import io.reactivex.Observable;
import io.reactivex.TestHelper;
import io.reactivex.exceptions.TestException;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorFlatMapTest {
    @Test
    public void testNormal() {
        Subscriber<Object> o = TestHelper.mockSubscriber();

        final List<Integer> list = Arrays.asList(1, 2, 3);

        Function<Integer, List<Integer>> func = new Function<Integer, List<Integer>>() {
            @Override
            public List<Integer> apply(Integer t1) {
                return list;
            }
        };
        BiFunction<Integer, Integer, Integer> resFunc = new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.fromIterable(source).flatMapIterable(func, resFunc).subscribe(o);

        for (Integer s : source) {
            for (Integer v : list) {
                verify(o).onNext(s | v);
            }
        }
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testCollectionFunctionThrows() {
        Subscriber<Object> o = TestHelper.mockSubscriber();

        Function<Integer, List<Integer>> func = new Function<Integer, List<Integer>>() {
            @Override
            public List<Integer> apply(Integer t1) {
                throw new TestException();
            }
        };
        BiFunction<Integer, Integer, Integer> resFunc = new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.fromIterable(source).flatMapIterable(func, resFunc).subscribe(o);

        verify(o, never()).onComplete();
        verify(o, never()).onNext(any());
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testResultFunctionThrows() {
        Subscriber<Object> o = TestHelper.mockSubscriber();

        final List<Integer> list = Arrays.asList(1, 2, 3);

        Function<Integer, List<Integer>> func = new Function<Integer, List<Integer>>() {
            @Override
            public List<Integer> apply(Integer t1) {
                return list;
            }
        };
        BiFunction<Integer, Integer, Integer> resFunc = new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                throw new TestException();
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.fromIterable(source).flatMapIterable(func, resFunc).subscribe(o);

        verify(o, never()).onComplete();
        verify(o, never()).onNext(any());
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testMergeError() {
        Subscriber<Object> o = TestHelper.mockSubscriber();

        Function<Integer, Observable<Integer>> func = new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t1) {
                return Observable.error(new TestException());
            }
        };
        BiFunction<Integer, Integer, Integer> resFunc = new BiFunction<Integer, Integer, Integer>() {

            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.fromIterable(source).flatMap(func, resFunc).subscribe(o);

        verify(o, never()).onComplete();
        verify(o, never()).onNext(any());
        verify(o).onError(any(TestException.class));
    }

    <T, R> Function<T, R> just(final R value) {
        return new Function<T, R>() {

            @Override
            public R apply(T t1) {
                return value;
            }
        };
    }

    <R> Supplier<R> just0(final R value) {
        return new Supplier<R>() {

            @Override
            public R get() {
                return value;
            }
        };
    }

    @Test
    public void testFlatMapTransformsNormal() {
        Observable<Integer> onNext = Observable.fromIterable(Arrays.asList(1, 2, 3));
        Observable<Integer> onCompleted = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.fromIterable(Arrays.asList(10, 20, 30));

        Subscriber<Object> o = TestHelper.mockSubscriber();

        source.flatMap(just(onNext), just(onError), just0(onCompleted)).subscribe(o);

        verify(o, times(3)).onNext(1);
        verify(o, times(3)).onNext(2);
        verify(o, times(3)).onNext(3);
        verify(o).onNext(4);
        verify(o).onComplete();

        verify(o, never()).onNext(5);
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testFlatMapTransformsException() {
        Observable<Integer> onNext = Observable.fromIterable(Arrays.asList(1, 2, 3));
        Observable<Integer> onCompleted = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.concat(
                Observable.fromIterable(Arrays.asList(10, 20, 30)),
                Observable.<Integer> error(new RuntimeException("Forced failure!"))
                );
        

        Subscriber<Object> o = TestHelper.mockSubscriber();

        source.flatMap(just(onNext), just(onError), just0(onCompleted)).subscribe(o);

        verify(o, times(3)).onNext(1);
        verify(o, times(3)).onNext(2);
        verify(o, times(3)).onNext(3);
        verify(o).onNext(5);
        verify(o).onComplete();
        verify(o, never()).onNext(4);

        verify(o, never()).onError(any(Throwable.class));
    }

    <R> Supplier<R> funcThrow0(R r) {
        return new Supplier<R>() {
            @Override
            public R get() {
                throw new TestException();
            }
        };
    }

    <T, R> Function<T, R> funcThrow(T t, R r) {
        return new Function<T, R>() {
            @Override
            public R apply(T t) {
                throw new TestException();
            }
        };
    }

    @Test
    public void testFlatMapTransformsOnNextFuncThrows() {
        Observable<Integer> onCompleted = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.fromIterable(Arrays.asList(10, 20, 30));

        Subscriber<Object> o = TestHelper.mockSubscriber();

        source.flatMap(funcThrow(1, onError), just(onError), just0(onCompleted)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testFlatMapTransformsOnErrorFuncThrows() {
        Observable<Integer> onNext = Observable.fromIterable(Arrays.asList(1, 2, 3));
        Observable<Integer> onCompleted = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.error(new TestException());

        Subscriber<Object> o = TestHelper.mockSubscriber();

        source.flatMap(just(onNext), funcThrow((Throwable) null, onError), just0(onCompleted)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testFlatMapTransformsOnCompletedFuncThrows() {
        Observable<Integer> onNext = Observable.fromIterable(Arrays.asList(1, 2, 3));
        Observable<Integer> onCompleted = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.fromIterable(Arrays.<Integer> asList());

        Subscriber<Object> o = TestHelper.mockSubscriber();

        source.flatMap(just(onNext), just(onError), funcThrow0(onCompleted)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    @Test
    public void testFlatMapTransformsMergeException() {
        Observable<Integer> onNext = Observable.error(new TestException());
        Observable<Integer> onCompleted = Observable.fromIterable(Arrays.asList(4));
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.fromIterable(Arrays.asList(10, 20, 30));

        Subscriber<Object> o = TestHelper.mockSubscriber();

        source.flatMap(just(onNext), just(onError), funcThrow0(onCompleted)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onComplete();
    }

    private static <T> Observable<T> composer(Observable<T> source, final AtomicInteger subscriptionCount, final int m) {
        return source.doOnSubscribe(s -> {
                int n = subscriptionCount.getAndIncrement();
                if (n >= m) {
                    Assert.fail("Too many subscriptions! " + (n + 1));
                }
        }).doOnComplete(() -> {
                int n = subscriptionCount.decrementAndGet();
                if (n < 0) {
                    Assert.fail("Too many unsubscriptions! " + (n - 1));
                }
        });
    }

    @Test
    public void testFlatMapMaxConcurrent() {
        final int m = 4;
        final AtomicInteger subscriptionCount = new AtomicInteger();
        Observable<Integer> source = Observable.range(1, 10)
        .flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t1) {
                return composer(Observable.range(t1 * 10, 2), subscriptionCount, m)
                        .subscribeOn(Schedulers.computation());
            }
        }, m);
        
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        
        source.subscribe(ts);
        
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        Set<Integer> expected = new HashSet<>(Arrays.asList(
                10, 11, 20, 21, 30, 31, 40, 41, 50, 51, 60, 61, 70, 71, 80, 81, 90, 91, 100, 101
        ));
        Assert.assertEquals(expected.size(), ts.valueCount());
        Assert.assertTrue(expected.containsAll(ts.values()));
    }
    @Test
    public void testFlatMapSelectorMaxConcurrent() {
        final int m = 4;
        final AtomicInteger subscriptionCount = new AtomicInteger();
        Observable<Integer> source = Observable.range(1, 10)
            .flatMap(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t1) {
                return composer(Observable.range(t1 * 10, 2), subscriptionCount, m)
                        .subscribeOn(Schedulers.computation());
            }
        }, new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer t1, Integer t2) {
                return t1 * 1000 + t2;
            }
        }, m);
        
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        
        source.subscribe(ts);
        
        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        Set<Integer> expected = new HashSet<>(Arrays.asList(
                1010, 1011, 2020, 2021, 3030, 3031, 4040, 4041, 5050, 5051, 
                6060, 6061, 7070, 7071, 8080, 8081, 9090, 9091, 10100, 10101
        ));
        Assert.assertEquals(expected.size(), ts.valueCount());
        System.out.println("--> testFlatMapSelectorMaxConcurrent: " + ts.values());
        Assert.assertTrue(expected.containsAll(ts.values()));
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
                composer(
                        Observable.fromIterable(Arrays.asList(1, 2, 3))
                        .observeOn(Schedulers.computation())
                        , 
                subscriptionCount, m)
                .subscribeOn(Schedulers.computation())
                ;
        
        Observable<Integer> onCompleted = composer(Observable.fromIterable(Arrays.asList(4)), subscriptionCount, m)
                .subscribeOn(Schedulers.computation());
        
        Observable<Integer> onError = Observable.fromIterable(Arrays.asList(5));

        Observable<Integer> source = Observable.fromIterable(Arrays.asList(10, 20, 30));

        Subscriber<Object> o = TestHelper.mockSubscriber();
        TestSubscriber<Object> ts = new TestSubscriber<>(o);

        source.flatMap(just(onNext), just(onError), just0(onCompleted), m).subscribe(ts);
        
        ts.awaitTerminalEvent(1, TimeUnit.SECONDS);
        ts.assertNoErrors();
        ts.assertTerminated();

        verify(o, times(3)).onNext(1);
        verify(o, times(3)).onNext(2);
        verify(o, times(3)).onNext(3);
        verify(o).onNext(4);
        verify(o).onComplete();

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
            TestSubscriber<Integer> ts = new TestSubscriber<>();
            Observable.range(0, 1000)
            .flatMap(new Function<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer t) {
                    return Observable.just(t);
                }
            })
            .observeOn(Schedulers.computation())
            .subscribe(ts);

            ts.awaitTerminalEvent(2500, TimeUnit.MILLISECONDS);
            if (ts.completions() == 0) {
                System.out.println(ts.valueCount());
            }
            ts.assertTerminated();
            ts.assertNoErrors();
            List<Integer> list = ts.values();
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
            TestSubscriber<Integer> ts = new TestSubscriber<>();
            Observable.range(0, 1000)
            .flatMap(new Function<Integer, Observable<Integer>>() {
                final Random rnd = new Random();
                @Override
                public Observable<Integer> apply(Integer t) {
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
            if (ts.completions() == 0) {
                System.out.println(ts.valueCount());
            }
            ts.assertTerminated();
            ts.assertNoErrors();
            List<Integer> list = ts.values();
            if (list.size() < 1000) {
                Set<Integer> set = new HashSet<>(list);
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
            TestSubscriber<Integer> ts = new TestSubscriber<>();
            
            Observable.range(1, 1000).flatMap(new Function<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer t) {
                    return Observable.just(1).subscribeOn(Schedulers.computation());
                }
            }).subscribe(ts);
            
            ts.awaitTerminalEvent(5, TimeUnit.SECONDS);
            ts.assertNoErrors();
            ts.assertComplete();
            ts.assertValueCount(1000);
        }
    }
    @Test
    public void flatMapTwoNestedSync() {
        for (final int n : new int[] { 1, 1000, 1000000 }) {
            TestSubscriber<Integer> ts = new TestSubscriber<>();
    
            Observable.just(1, 2).flatMap(new Function<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer t) {
                    return Observable.range(1, n);
                }
            }).subscribe(ts);
            
            System.out.println("flatMapTwoNestedSync >> @ " + n);
            ts.assertNoErrors();
            ts.assertComplete();
            ts.assertValueCount(n * 2);
        }
    }
}