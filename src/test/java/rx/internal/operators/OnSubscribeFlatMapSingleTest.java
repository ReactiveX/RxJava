/**
 * Copyright 2017 Netflix, Inc.
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import org.junit.Test;

import rx.*;
import rx.Observable;
import rx.exceptions.*;
import rx.functions.*;
import rx.observers.*;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class OnSubscribeFlatMapSingleTest implements Action0, Action1<Object> {

    final AtomicInteger calls = new AtomicInteger();

    final Action1<Throwable> errorConsumer = new Action1<Throwable>() {
        @Override
        public void call(Throwable e) {
            OnSubscribeFlatMapSingleTest.this.call(e);
        }
    };

    @Override
    public void call() {
        calls.getAndIncrement();
    }

    @Override
    public void call(Object t) {
        calls.getAndIncrement();
    }

    void assertCalls(int n) {
        assertEquals(n, calls.get());
    }

    @Test
    public void normal() {
        Observable.range(1, 10)
        .flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(v);
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void normalBackpressured() {
        Observable.range(1, 10)
        .flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(v);
            }
        })
        .test(0L)
        .assertNoValues()
        .requestMore(1)
        .assertValues(1)
        .requestMore(2)
        .assertValues(1, 2, 3)
        .requestMore(3)
        .assertValues(1, 2, 3, 4, 5, 6)
        .requestMore(4)
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void normalMaxConcurrencyBackpressured() {
        for (int i = 1; i < 16; i++) {
            Observable.range(1, 10)
            .flatMapSingle(new Func1<Integer, Single<Integer>>() {
                @Override
                public Single<Integer> call(Integer v) {
                    return Single.just(v);
                }
            }, false, i)
            .test(0L)
            .assertNoValues()
            .requestMore(1)
            .assertValues(1)
            .requestMore(2)
            .assertValues(1, 2, 3)
            .requestMore(3)
            .assertValues(1, 2, 3, 4, 5, 6)
            .requestMore(4)
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
    }

    @Test
    public void normalMaxConcurrent() {
        for (int i = 1; i < 16; i++) {
            Observable.range(1, 10)
            .flatMapSingle(new Func1<Integer, Single<Integer>>() {
                @Override
                public Single<Integer> call(Integer v) {
                    return Single.just(v);
                }
            }, false, i)
            .test()
            .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
    }

    @Test
    public void normalMaxConcurrentAsync() {
        for (int i = 1; i < 2; i++) {
            AssertableSubscriber<Integer> as = Observable.range(1, 10)
            .flatMapSingle(new Func1<Integer, Single<Integer>>() {
                @Override
                public Single<Integer> call(Integer v) {
                    return Single.just(v).observeOn(Schedulers.computation());
                }
            }, false, i)
            .test();

            as.awaitTerminalEvent(5, TimeUnit.SECONDS)
            .assertValueCount(10)
            .assertNoErrors()
            .assertCompleted();

            Set<Integer> set = new HashSet<Integer>(as.getOnNextEvents());

            for (int j = 1; j < 11; j++) {
                assertTrue("" + set, set.contains(j));
            }
        }
    }

    @Test
    public void justMaxConcurrentAsync() {
        AssertableSubscriber<Integer> as = Observable.just(1)
        .flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(v).observeOn(Schedulers.computation());
            }
        }, false, 1)
        .test();

        as.awaitTerminalEvent(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void error() {
        Observable.range(1, 10)
        .flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.<Integer>error(new TestException()).doOnError(errorConsumer);
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertCalls(1);
    }

    @Test
    public void errorDelayed() {
        AssertableSubscriber<Integer> as = Observable.range(1, 10)
        .flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.<Integer>error(new TestException()).doOnError(errorConsumer);
            }
        }, true)
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> onErrorEvents = as.getOnErrorEvents();

        assertEquals(onErrorEvents.toString(), 1, onErrorEvents.size());

        onErrorEvents = ((CompositeException)onErrorEvents.get(0)).getExceptions();

        assertEquals(onErrorEvents.toString(), 10, onErrorEvents.size());

        for (Throwable ex : onErrorEvents) {
            assertTrue(ex.toString(), ex instanceof TestException);
        }

        assertCalls(10);
    }

    @Test
    public void errorDelayedMaxConcurrency() {
        AssertableSubscriber<Integer> as = Observable.range(1, 10)
        .flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.<Integer>error(new TestException()).doOnError(errorConsumer);
            }
        }, true, 1)
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> onErrorEvents = as.getOnErrorEvents();

        assertEquals(onErrorEvents.toString(), 1, onErrorEvents.size());

        onErrorEvents = ((CompositeException)onErrorEvents.get(0)).getExceptions();

        assertEquals(onErrorEvents.toString(), 10, onErrorEvents.size());

        for (Throwable ex : onErrorEvents) {
            assertTrue(ex.toString(), ex instanceof TestException);
        }

        assertCalls(10);
    }

    @Test
    public void mapperThrows() {
        Observable.range(1, 10)
        .flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperNull() {
        Observable.range(1, 10)
        .flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void paramValidation() {
        try {
            Observable.range(1, 10)
            .flatMapSingle(null);
            fail("Should have thrown");
        } catch (NullPointerException ex) {
            assertEquals("mapper is null", ex.getMessage());
        }

        try {
            Observable.range(1, 10)
            .flatMapSingle(new Func1<Integer, Single<Integer>>() {
                @Override
                public Single<Integer> call(Integer v) {
                    return Single.just(v);
                }
            }, false, 0);
            fail("Should have thrown");
        } catch (IllegalArgumentException ex) {
            assertEquals("maxConcurrency > 0 required but it was 0", ex.getMessage());
        }

        try {
            Observable.range(1, 10)
            .flatMapSingle(new Func1<Integer, Single<Integer>>() {
                @Override
                public Single<Integer> call(Integer v) {
                    return Single.just(v);
                }
            }, true, -99);
            fail("Should have thrown");
        } catch (IllegalArgumentException ex) {
            assertEquals("maxConcurrency > 0 required but it was -99", ex.getMessage());
        }
    }

    @Test
    public void mainErrorDelayed() {
        Observable.range(1, 10).concatWith(Observable.<Integer>error(new TestException()))
        .flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(v).doOnSuccess(OnSubscribeFlatMapSingleTest.this);
            }
        }, true)
        .test()
        .assertFailure(TestException.class, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        assertCalls(10);
    }

    @Test
    public void mainErrorUnsubscribes() {
        PublishSubject<Integer> ps0 = PublishSubject.create();
        final PublishSubject<Integer> ps1 = PublishSubject.create();
        final PublishSubject<Integer> ps2 = PublishSubject.create();

        TestSubscriber<Integer> as = TestSubscriber.create();

        ps0.flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return v == 0 ? ps1.toSingle() : ps2.toSingle();
            }
        }).unsafeSubscribe(as);

        assertTrue(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(0);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(1);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onError(new TestException());

        assertFalse(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertError(TestException.class);
        as.assertNotCompleted();
    }

    @Test
    public void innerErrorUnsubscribes() {
        PublishSubject<Integer> ps0 = PublishSubject.create();
        final PublishSubject<Integer> ps1 = PublishSubject.create();
        final PublishSubject<Integer> ps2 = PublishSubject.create();

        TestSubscriber<Integer> as = TestSubscriber.create();

        ps0.flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return v == 0 ? ps1.toSingle() : ps2.toSingle();
            }
        }).unsafeSubscribe(as);

        assertTrue(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(0);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(1);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps1.onError(new TestException());

        assertFalse(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertError(TestException.class);
        as.assertNotCompleted();
    }

    @Test
    public void take() {
        Observable.range(1, 10)
        .flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(v);
            }
        })
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }


    @Test
    public void unsubscribe() {
        AssertableSubscriber<Integer> as = Observable.range(1, 10)
        .flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(v);
            }
        })
        .test(0)
        ;

        as.unsubscribe();

        as.assertNoValues().assertNoErrors().assertNotCompleted();
    }

    @Test
    public void mainErrorUnsubscribesNoRequest() {
        PublishSubject<Integer> ps0 = PublishSubject.create();
        final PublishSubject<Integer> ps1 = PublishSubject.create();
        final PublishSubject<Integer> ps2 = PublishSubject.create();

        TestSubscriber<Integer> as = TestSubscriber.create(0L);

        ps0.flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return v == 0 ? ps1.toSingle() : ps2.toSingle();
            }
        }).unsafeSubscribe(as);

        assertTrue(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(0);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(1);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onError(new TestException());

        assertFalse(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertError(TestException.class);
        as.assertNotCompleted();
    }

    @Test
    public void innerErrorUnsubscribesNoRequest() {
        PublishSubject<Integer> ps0 = PublishSubject.create();
        final PublishSubject<Integer> ps1 = PublishSubject.create();
        final PublishSubject<Integer> ps2 = PublishSubject.create();

        TestSubscriber<Integer> as = TestSubscriber.create(0L);

        ps0.flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return v == 0 ? ps1.toSingle() : ps2.toSingle();
            }
        }).unsafeSubscribe(as);

        assertTrue(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(0);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(1);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps1.onError(new TestException());

        assertFalse(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertError(TestException.class);
        as.assertNotCompleted();
    }
    @Test
    public void mainErrorUnsubscribesNoRequestDelayError() {
        PublishSubject<Integer> ps0 = PublishSubject.create();
        final PublishSubject<Integer> ps1 = PublishSubject.create();
        final PublishSubject<Integer> ps2 = PublishSubject.create();

        TestSubscriber<Integer> as = TestSubscriber.create(0L);

        ps0.flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return v == 0 ? ps1.toSingle() : ps2.toSingle();
            }
        }, true).unsafeSubscribe(as);

        assertTrue(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(0);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(1);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onError(new TestException());
        ps1.onNext(3);
        ps1.onCompleted();
        ps2.onNext(4);
        ps2.onCompleted();

        assertFalse(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());

        as.requestMore(2);
        as.assertValues(3, 4);
        as.assertError(TestException.class);
        as.assertNotCompleted();
    }

    @Test
    public void innerErrorUnsubscribesNoRequestDelayError() {
        PublishSubject<Integer> ps0 = PublishSubject.create();
        final PublishSubject<Integer> ps1 = PublishSubject.create();
        final PublishSubject<Integer> ps2 = PublishSubject.create();

        TestSubscriber<Integer> as = TestSubscriber.create(0L);

        ps0.flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return v == 0 ? ps1.toSingle() : ps2.toSingle();
            }
        }, true).unsafeSubscribe(as);

        assertTrue(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(0);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertFalse(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onNext(1);

        assertTrue(ps0.hasObservers());
        assertTrue(ps1.hasObservers());
        assertTrue(ps2.hasObservers());
        as.assertNoValues();
        as.assertNoErrors();
        as.assertNotCompleted();

        ps0.onCompleted();
        ps1.onError(new TestException());
        ps2.onNext(4);
        ps2.onCompleted();

        assertFalse(ps0.hasObservers());
        assertFalse(ps1.hasObservers());
        assertFalse(ps2.hasObservers());

        as.requestMore(1);
        as.assertValues(4);
        as.assertError(TestException.class);
        as.assertNotCompleted();
    }

    @Test
    public void justBackpressured() {
        Observable.just(1)
        .flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(v);
            }
        })
        .test(1L)
        .assertResult(1);
    }

    @Test
    public void justBackpressuredDelayError() {
        Observable.just(1)
        .flatMapSingle(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(v);
            }
        }, true)
        .test(1L)
        .assertResult(1);
    }

    @Test
    public void singleMerge() {
        Single.merge(Observable.range(1, 10).map(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(v);
            }
        }))
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void singleMergeMaxConcurrent() {
        AssertableSubscriber<Integer> as = Single.merge(Observable.range(1, 10).map(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(v).observeOn(Schedulers.computation());
            }
        }), 2)
        .test()
        .awaitTerminalEvent(5, TimeUnit.SECONDS);

        Set<Integer> set = new HashSet<Integer>(as.getOnNextEvents());

        assertEquals("" + set, 10, set.size());
        for (int j = 1; j < 11; j++) {
            assertTrue("" + set, set.contains(j));
        }
    }

    @Test
    public void singleMergeDelayError() {
        Single.mergeDelayError(Observable.range(1, 10).map(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(v);
            }
        }))
        .test()
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void singleMergeDelayErrorMaxConcurrent() {
        AssertableSubscriber<Integer> as = Single.mergeDelayError(Observable.range(1, 10).map(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(v).observeOn(Schedulers.computation());
            }
        }), 2)
        .test()
        .awaitTerminalEvent(5, TimeUnit.SECONDS);

        Set<Integer> set = new HashSet<Integer>(as.getOnNextEvents());

        assertEquals("" + set, 10, set.size());
        for (int j = 1; j < 11; j++) {
            assertTrue("" + set, set.contains(j));
        }
    }

    @Test
    public void singleMergeDelayErrorWithError() {
        Single.mergeDelayError(Observable.range(1, 10)
                .concatWith(Observable.<Integer>error(new TestException()))
                .map(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(v);
            }
        }))
        .test()
        .assertFailure(TestException.class, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @Test
    public void singleMergeDelayMaxConcurrentErrorWithError() {
        AssertableSubscriber<Integer> as = Single.mergeDelayError(Observable.range(1, 10)
                .concatWith(Observable.<Integer>error(new TestException()))
                .map(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(v).observeOn(Schedulers.computation());
            }
        }), 2)
        .test()
        .awaitTerminalEvent(5, TimeUnit.SECONDS);

        Set<Integer> set = new HashSet<Integer>(as.getOnNextEvents());

        assertEquals("" + set, 10, set.size());
        for (int j = 1; j < 11; j++) {
            assertTrue("" + set, set.contains(j));
        }
    }
}
