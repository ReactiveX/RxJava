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

package io.reactivex.maybe;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subscribers.TestSubscriber;

public class MaybeTest {
    @Test
    public void fromFlowableEmpty() {
        
        Flowable.empty()
        .toMaybe()
        .test()
        .assertResult();
    }
    
    @Test
    public void fromFlowableJust() {
        
        Flowable.just(1)
        .toMaybe()
        .test()
        .assertResult(1);
    }

    @Test
    public void fromFlowableError() {
        
        Flowable.error(new TestException())
        .toMaybe()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fromFlowableValueAndError() {
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .toMaybe()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fromFlowableMany() {
        
        Flowable.range(1, 2)
        .toMaybe()
        .test()
        .assertFailure(IndexOutOfBoundsException.class);
    }
    
    @Test
    public void fromFlowableDisposeComposesThrough() {
        PublishProcessor<Integer> pp = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = pp.toMaybe().test();
        
        assertTrue(pp.hasSubscribers());
        
        ts.cancel();
        
        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void fromObservableEmpty() {
        
        Observable.empty()
        .toMaybe()
        .test()
        .assertResult();
    }
    
    @Test
    public void fromObservableJust() {
        
        Observable.just(1)
        .toMaybe()
        .test()
        .assertResult(1);
    }

    @Test
    public void fromObservableError() {
        
        Observable.error(new TestException())
        .toMaybe()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fromObservableValueAndError() {
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .toMaybe()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fromObservableMany() {
        
        Observable.range(1, 2)
        .toMaybe()
        .test()
        .assertFailure(IndexOutOfBoundsException.class);
    }
    
    @Test
    public void fromObservableDisposeComposesThrough() {
        PublishSubject<Integer> pp = PublishSubject.create();
        
        TestSubscriber<Integer> ts = pp.toMaybe().test(false);
        
        assertTrue(pp.hasObservers());
        
        ts.cancel();
        
        assertFalse(pp.hasObservers());
    }

    @Test
    public void fromObservableDisposeComposesThroughImmediatelyCancelled() {
        PublishSubject<Integer> pp = PublishSubject.create();
        
        pp.toMaybe().test(true);
        
        assertFalse(pp.hasObservers());
    }

    @Test
    public void just() {
        Maybe.just(1)
        .test()
        .assertResult(1);
    }
    
    @Test(expected = NullPointerException.class)
    public void justNull() {
        Maybe.just(null);
    }

    @Test
    public void empty() {
        Maybe.empty()
        .test()
        .assertResult();
    }

    @Test
    public void never() {
        Maybe.never()
        .test()
        .assertSubscribed()
        .assertNoValues()
        .assertNoErrors()
        .assertNotComplete();
    }

    @Test(expected = NullPointerException.class)
    public void errorNull() {
        Maybe.error((Throwable)null);
    }

    @Test(expected = NullPointerException.class)
    public void errorCallableNull() {
        Maybe.error((Callable<Throwable>)null);
    }

    @Test
    public void error() {
        Maybe.error(new TestException())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorCallable() {
        Maybe.error(Functions.justCallable(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorCallableReturnsNull() {
        Maybe.error(Functions.justCallable((Throwable)null))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void wrapCustom() {
        Maybe.wrap(new MaybeSource<Integer>() {
            @Override
            public void subscribe(MaybeObserver<? super Integer> observer) {
                observer.onSubscribe(Disposables.empty());
                observer.onSuccess(1);
            }
        })
        .test()
        .assertResult(1);
    }
    
    @Test
    public void wrapMaybe() {
        assertSame(Maybe.empty(), Maybe.wrap(Maybe.empty()));
    }

    @Test(expected = NullPointerException.class)
    public void wrapNull() {
        Maybe.wrap(null);
    }

    @Test
    public void emptySingleton() {
        assertSame(Maybe.empty(), Maybe.empty());
    }

    @Test
    public void neverSingleton() {
        assertSame(Maybe.never(), Maybe.never());
    }

    @Test(expected = NullPointerException.class)
    public void liftNull() {
        Maybe.just(1).lift(null);
    }
    
    @Test
    public void liftJust() {
        Maybe.just(1).lift(new MaybeOperator<Integer, Integer>() {
            @Override
            public MaybeObserver<? super Integer> apply(MaybeObserver<? super Integer> t) throws Exception {
                return t;
            }
        })
        .test()
        .assertResult(1);
    }
    
    @Test
    public void liftThrows() {
        Maybe.just(1).lift(new MaybeOperator<Integer, Integer>() {
            @Override
            public MaybeObserver<? super Integer> apply(MaybeObserver<? super Integer> t) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }
    
    @Test(expected = NullPointerException.class)
    public void deferNull() {
        Maybe.defer(null);
    }
    
    @Test
    public void deferThrows() {
        Maybe.defer(new Callable<Maybe<Integer>>() {
            @Override
            public Maybe<Integer> call() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }
    
    @Test
    public void deferReturnsNull() {
        Maybe.defer(new Callable<Maybe<Integer>>() {
            @Override
            public Maybe<Integer> call() throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }
    
    @Test
    public void defer() {
        Maybe<Integer> source = Maybe.defer(new Callable<Maybe<Integer>>() {
            int count;
            @Override
            public Maybe<Integer> call() throws Exception {
                return Maybe.just(count++);
            }
        });

        for (int i = 0; i < 128; i++) {
            source.test().assertResult(i);
        }
    }
    
    @Test
    public void flowableMaybeFlowable() {
        Flowable.just(1).toMaybe().toFlowable().test().assertResult(1);
    }

    @Test
    public void obervableMaybeobervable() {
        Observable.just(1).toMaybe().toObservable().test().assertResult(1);
    }

    @Test
    public void singleMaybeSingle() {
        Single.just(1).toMaybe().toSingle().test().assertResult(1);
    }

    @Test
    public void completableMaybeCompletable() {
        Completable.complete().toMaybe().toCompletable().test().assertResult();
    }


    @Test
    public void unsafeCreate() {
        Maybe.unsafeCreate(new MaybeSource<Integer>() {
            @Override
            public void subscribe(MaybeObserver<? super Integer> observer) {
                observer.onSubscribe(Disposables.empty());
                observer.onSuccess(1);
            }
        })
        .test()
        .assertResult(1);
    }
    
    @Test(expected = NullPointerException.class)
    public void unsafeCreateNull() {
        Maybe.unsafeCreate(null);
    }
    
    @Test
    public void to() {
        Maybe.just(1).to(new Function<Maybe<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Maybe<Integer> v) throws Exception {
                return v.toFlowable();
            }
        })
        .test()
        .assertResult(1);
    }
    
    @Test(expected = NullPointerException.class)
    public void toNull() {
        Maybe.just(1).to(null);
    }
    
    @Test
    public void compose() {
        Maybe.just(1).compose(new Function<Maybe<Integer>, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Maybe<Integer> m) throws Exception {
                return m.map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer w) throws Exception {
                        return w + 1;
                    }
                });
            }
        })
        .test()
        .assertResult(2);
    }

    @Test(expected = NullPointerException.class)
    public void composeNull() {
        Maybe.just(1).compose(null);
    }

    @Test(expected = NullPointerException.class)
    public void mapNull() {
        Maybe.just(1).map(null);
    }

    @Test
    public void mapReturnNull() {
        Maybe.just(1).map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                return null;
            }
        }).test().assertFailure(NullPointerException.class);
    }

    @Test
    public void mapThrows() {
        Maybe.just(1).map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                throw new IOException();
            }
        }).test().assertFailure(IOException.class);
    }

    @Test
    public void map() {
        Maybe.just(1).map(new Function<Integer, String>() {
            @Override
            public String apply(Integer v) throws Exception {
                return v.toString();
            }
        }).test().assertResult("1");
    }
    
    @Test(expected = NullPointerException.class)
    public void filterNull() {
        Maybe.just(1).filter(null);
    }

    @Test
    public void filterThrows() {
        Maybe.just(1).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                throw new IOException();
            }
        }).test().assertFailure(IOException.class);
    }

    @Test
    public void filterTrue() {
        Maybe.just(1).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == 1;
            }
        }).test().assertResult(1);
    }

    @Test
    public void filterFalse() {
        Maybe.just(2).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == 1;
            }
        }).test().assertResult();
    }

    @Test(expected = NullPointerException.class)
    public void singleFilterNull() {
        Single.just(1).filter(null);
    }

    @Test
    public void singleFilterThrows() {
        Single.just(1).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                throw new IOException();
            }
        }).test().assertFailure(IOException.class);
    }

    @Test
    public void singleFilterTrue() {
        Single.just(1).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == 1;
            }
        }).test().assertResult(1);
    }

    @Test
    public void singleFilterFalse() {
        Single.just(2).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == 1;
            }
        }).test().assertResult();
    }

    @Test
    public void cast() {
        TestSubscriber<Number> ts = Maybe.just(1).cast(Number.class).test();
        // don'n inline this due to the generic type
        ts.assertResult((Number)1);
    }
    
    @Test(expected = NullPointerException.class)
    public void observeOnNull() {
        Maybe.just(1).observeOn(null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnNull() {
        Maybe.just(1).observeOn(null);
    }

    @Test
    public void observeOnSuccess() {
        String main = Thread.currentThread().getName();
        Maybe.just(1)
        .observeOn(Schedulers.single())
        .map(new Function<Integer, String>() {
            @Override
            public String apply(Integer v) throws Exception {
                return v + ": " + Thread.currentThread().getName();
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertOf(TestHelper.subscriberSingleNot("1: " + main))
        ;
    }

    @Test
    public void observeOnError() {
        Maybe.error(new TestException())
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class)
        ;
    }

    @Test
    public void observeOnComplete() {
        Maybe.empty()
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult()
        ;
    }

    @Test
    public void subscribeOnSuccess() {
        String main = Thread.currentThread().getName();
        Maybe.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return Thread.currentThread().getName();
            }
        })
        .subscribeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertOf(TestHelper.subscriberSingleNot(main))
        ;
    }
    
    @Test
    public void observeOnErrorThread() {
        String main = Thread.currentThread().getName();
        
        final String[] name = { null };
        
        Maybe.error(new TestException()).observeOn(Schedulers.single())
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception { 
                name[0] = Thread.currentThread().getName(); 
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class)
        ;
        
        assertNotEquals(main, name[0]);
    }

    
    @Test
    public void observeOnCompleteThread() {
        String main = Thread.currentThread().getName();
        
        final String[] name = { null };
        
        Maybe.empty().observeOn(Schedulers.single())
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception { 
                name[0] = Thread.currentThread().getName(); 
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult()
        ;
        
        assertNotEquals(main, name[0]);
    }

    @Test
    public void subscribeOnError() {
        Maybe.error(new TestException())
        .subscribeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class)
        ;
    }

    @Test
    public void subscribeOnComplete() {
        Maybe.empty()
        .subscribeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult()
        ;
    }


    @Test
    public void fromAction() {
        final int[] call = { 0 };
        
        Maybe.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                call[0]++;
            }
        })
        .test()
        .assertResult();
        
        assertEquals(1, call[0]);
    }
    
    @Test
    public void fromActionThrows() {
        Maybe.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }
    
    @Test
    public void fromRunnable() {
        final int[] call = { 0 };
        
        Maybe.fromRunnable(new Runnable() {
            @Override
            public void run() {
                call[0]++;
            }
        })
        .test()
        .assertResult();
        
        assertEquals(1, call[0]);
    }
    
    @Test
    public void fromRunnableThrows() {
        Maybe.fromRunnable(new Runnable() {
            @Override
            public void run() {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }
    
    @Test
    public void fromCallableThrows() {
        Maybe.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }
    
    @Test
    public void doOnSuccess() {
        final Integer[] value = { null };
        
        Maybe.just(1).doOnSuccess(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception { 
                value[0] = v; 
            }
        })
        .test()
        .assertResult(1);
        
        assertEquals(1, value[0].intValue());
    }
    
    @Test
    public void doOnSuccessEmpty() {
        final Integer[] value = { null };
        
        Maybe.<Integer>empty().doOnSuccess(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception { 
                value[0] = v; 
            }
        })
        .test()
        .assertResult();
        
        assertNull(value[0]);
    }
    
    @Test
    public void doOnSuccessThrows() {
        Maybe.just(1).doOnSuccess(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception { 
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    
    @Test
    public void doOnSubscribe() {
        final Disposable[] value = { null };
        
        Maybe.just(1).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable v) throws Exception { 
                value[0] = v; 
            }
        })
        .test()
        .assertResult(1);
        
        assertNotNull(value[0]);
    }
    
    @Test
    public void doOnSubscribeThrows() {
        Maybe.just(1).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable v) throws Exception { 
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    
    @Test
    public void doOnCompleteThrows() {
        Maybe.empty().doOnComplete(new Action() {
            @Override
            public void run() throws Exception { 
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnDispose() {
        final int[] call = { 0 };
        
        Maybe.just(1).doOnDispose(new Action() {
            @Override
            public void run() throws Exception { 
                call[0]++; 
            }
        })
        .test(true)
        .assertSubscribed()
        .assertNoValues()
        .assertNoErrors()
        .assertNotComplete();
        
        assertEquals(1, call[0]);
    }


    @Test
    public void doOnDisposeThrows() {
        List<Throwable> list = TestHelper.trackPluginErrors();
        
        try {
            PublishProcessor<Integer> pp = PublishProcessor.create();
            
            TestSubscriber<Integer> ts = pp.toMaybe().doOnDispose(new Action() {
                @Override
                public void run() throws Exception {
                    throw new TestException();
                }
            })
            .test();
            
            assertTrue(pp.hasSubscribers());
            
            ts.cancel();

            assertFalse(pp.hasSubscribers());

            ts.assertSubscribed()
            .assertNoValues()
            .assertNoErrors()
            .assertNotComplete();
            
            TestHelper.assertError(list, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void observeOnDispose() throws Exception {
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        final CountDownLatch cdl = new CountDownLatch(1);
        
        Maybe.just(1)
        .observeOn(Schedulers.single())
        .doOnSuccess(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (!cdl.await(5, TimeUnit.SECONDS)) {
                    throw new TimeoutException();
                }
            }
        })
        .toFlowable().subscribe(ts);
        
        Thread.sleep(250);
        
        ts.cancel();
        
        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(InterruptedException.class);
    }
    
    @Test
    public void doAfterTerminateSuccess() {
        final int[] call = { 0 };
        
        Maybe.just(1)
        .doOnSuccess(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                call[0]++; 
            }
        })
        .doAfterTerminate(new Action() {
            @Override
            public void run() throws Exception {
                if (call[0] == 1) {
                    call[0] = -1;
                }
            }
        })
        .test()
        .assertResult(1);
        
        assertEquals(-1, call[0]);
    }

    @Test
    public void doAfterTerminateError() {
        final int[] call = { 0 };
        
        Maybe.error(new TestException())
        .doOnError(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                call[0]++; 
            }
        })
        .doAfterTerminate(new Action() {
            @Override
            public void run() throws Exception {
                if (call[0] == 1) {
                    call[0] = -1;
                }
            }
        })
        .test()
        .assertFailure(TestException.class);
        
        assertEquals(-1, call[0]);
    }


    @Test
    public void doAfterTerminateComplete() {
        final int[] call = { 0 };
        
        Maybe.empty()
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                call[0]++; 
            }
        })
        .doAfterTerminate(new Action() {
            @Override
            public void run() throws Exception {
                if (call[0] == 1) {
                    call[0] = -1;
                }
            }
        })
        .test()
        .assertResult();
        
        assertEquals(-1, call[0]);
    }
    
    @Test
    public void sourceThrowsNPE() {
        try {
            Maybe.unsafeCreate(new MaybeSource<Object>() {
                @Override
                public void subscribe(MaybeObserver<? super Object> s) {
                    throw new NullPointerException("Forced failure");
                }
            }).test();
            
            fail("Should have thrown!");
        } catch (NullPointerException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    
    @Test
    public void sourceThrowsIAE() {
        try {
            Maybe.unsafeCreate(new MaybeSource<Object>() {
                @Override
                public void subscribe(MaybeObserver<? super Object> s) {
                    throw new IllegalArgumentException("Forced failure");
                }
            }).test();
            
            fail("Should have thrown!");
        } catch (NullPointerException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof IllegalArgumentException);
            assertEquals("Forced failure", ex.getCause().getMessage());
        }
    }

    @Test
    public void flatMap() {
        Maybe.just(1).flatMap(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v * 10);
            }
        })
        .test()
        .assertResult(10);
    }

    @Test
    public void flatMapEmpty() {
        Maybe.just(1).flatMap(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.empty();
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void flatMapError() {
        Maybe.just(1).flatMap(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.error(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void flatMapNotifySuccess() {
        Maybe.just(1)
        .flatMap(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v * 10);
            }
        },
        new Function<Throwable, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Throwable v) throws Exception {
                return Maybe.just(100);
            }
        },
        
        new Callable<MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> call() throws Exception {
                return Maybe.just(200);
            }
        })
        .test()
        .assertResult(10);
    }
    
    @Test
    public void flatMapNotifyError() {
        Maybe.<Integer>error(new TestException())
        .flatMap(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v * 10);
            }
        },
        new Function<Throwable, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Throwable v) throws Exception {
                return Maybe.just(100);
            }
        },
        
        new Callable<MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> call() throws Exception {
                return Maybe.just(200);
            }
        })
        .test()
        .assertResult(100);
    }
    
    @Test
    public void flatMapNotifyComplete() {
        Maybe.<Integer>empty()
        .flatMap(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v * 10);
            }
        },
        new Function<Throwable, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Throwable v) throws Exception {
                return Maybe.just(100);
            }
        },
        
        new Callable<MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> call() throws Exception {
                return Maybe.just(200);
            }
        })
        .test()
        .assertResult(200);
    }
    
    @Test
    public void ignoreElementSuccess() {
        Maybe.just(1)
        .ignoreElement()
        .test()
        .assertResult();
    }

    @Test
    public void ignoreElementError() {
        Maybe.error(new TestException())
        .ignoreElement()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void ignoreElementComplete() {
        Maybe.empty()
        .ignoreElement()
        .test()
        .assertResult();
    }
    
    @Test
    public void singleToMaybe() {
        Single.just(1)
        .toMaybe()
        .test()
        .assertResult(1);
    }

    @Test
    public void singleToMaybeError() {
        Single.error(new TestException())
        .toMaybe()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void completableToMaybe() {
        Completable.complete()
        .toMaybe()
        .test()
        .assertResult();
    }

    @Test
    public void completableToMaybeError() {
        Completable.error(new TestException())
        .toMaybe()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyToSingle() {
        Maybe.empty()
        .toSingle()
        .test()
        .assertFailure(NoSuchElementException.class);
    }

    @Test
    public void errorToSingle() {
        Maybe.error(new TestException())
        .toSingle()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyToCompletable() {
        Maybe.empty()
        .toCompletable()
        .test()
        .assertResult();
    }

    @Test
    public void errorToCompletable() {
        Maybe.error(new TestException())
        .toCompletable()
        .test()
        .assertFailure(TestException.class);
    }

}
