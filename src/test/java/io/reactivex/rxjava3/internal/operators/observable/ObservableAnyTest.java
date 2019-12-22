/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableAnyTest extends RxJavaTest {

    @Test
    public void anyWithTwoItemsObservable() {
        Observable<Integer> w = Observable.just(1, 2);
        Observable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        }).toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void isEmptyWithTwoItemsObservable() {
        Observable<Integer> w = Observable.just(1, 2);
        Observable<Boolean> observable = w.isEmpty().toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, never()).onNext(true);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void anyWithOneItemObservable() {
        Observable<Integer> w = Observable.just(1);
        Observable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        }).toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void isEmptyWithOneItemObservable() {
        Observable<Integer> w = Observable.just(1);
        Observable<Boolean> observable = w.isEmpty().toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, never()).onNext(true);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void anyWithEmptyObservable() {
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        }).toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void isEmptyWithEmptyObservable() {
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = w.isEmpty().toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onNext(false);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void anyWithPredicate1Observable() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        }).toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void exists1Observable() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        }).toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void anyWithPredicate2Observable() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 1;
            }
        }).toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void anyWithEmptyAndPredicateObservable() {
        // If the source is empty, always output false.
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t) {
                return true;
            }
        }).toObservable();

        Observer<Boolean> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void withFollowingFirstObservable() {
        Observable<Integer> o = Observable.fromArray(1, 3, 5, 6);
        Observable<Boolean> anyEven = o.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer i) {
                return i % 2 == 0;
            }
        }).toObservable();

        assertTrue(anyEven.blockingFirst());
    }

    @Test
    public void issue1935NoUnsubscribeDownstreamObservable() {
        Observable<Integer> source = Observable.just(1).isEmpty().toObservable()
            .flatMap(new Function<Boolean, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Boolean t1) {
                    return Observable.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
            });

        assertEquals((Object)2, source.blockingFirst());
    }

    @Test
    public void predicateThrowsExceptionAndValueInCauseMessageObservable() {
        TestObserverEx<Boolean> to = new TestObserverEx<>();
        final IllegalArgumentException ex = new IllegalArgumentException();

        Observable.just("Boo!").any(new Predicate<String>() {
            @Override
            public boolean test(String v) {
                throw ex;
            }
        }).subscribe(to);

        to.assertTerminated();
        to.assertNoValues();
        to.assertNotComplete();
        to.assertError(ex);
        // FIXME value as last cause?
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }

    @Test
    public void anyWithTwoItems() {
        Observable<Integer> w = Observable.just(1, 2);
        Single<Boolean> single = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        single.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void isEmptyWithTwoItems() {
        Observable<Integer> w = Observable.just(1, 2);
        Single<Boolean> single = w.isEmpty();

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        single.subscribe(observer);

        verify(observer, never()).onSuccess(true);
        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void anyWithOneItem() {
        Observable<Integer> w = Observable.just(1);
        Single<Boolean> single = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        single.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void isEmptyWithOneItem() {
        Observable<Integer> w = Observable.just(1);
        Single<Boolean> single = w.isEmpty();

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        single.subscribe(observer);

        verify(observer, never()).onSuccess(true);
        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void anyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Single<Boolean> single = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        single.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void isEmptyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Single<Boolean> single = w.isEmpty();

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        single.subscribe(observer);

        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onSuccess(false);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void anyWithPredicate1() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Single<Boolean> single = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        single.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void exists1() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Single<Boolean> single = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        single.subscribe(observer);

        verify(observer, never()).onSuccess(false);
        verify(observer, times(1)).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void anyWithPredicate2() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Single<Boolean> single = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 1;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        single.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void anyWithEmptyAndPredicate() {
        // If the source is empty, always output false.
        Observable<Integer> w = Observable.empty();
        Single<Boolean> single = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t) {
                return true;
            }
        });

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        single.subscribe(observer);

        verify(observer, times(1)).onSuccess(false);
        verify(observer, never()).onSuccess(true);
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void withFollowingFirst() {
        Observable<Integer> o = Observable.fromArray(1, 3, 5, 6);
        Single<Boolean> anyEven = o.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer i) {
                return i % 2 == 0;
            }
        });

        assertTrue(anyEven.blockingGet());
    }

    @Test
    public void issue1935NoUnsubscribeDownstream() {
        Observable<Integer> source = Observable.just(1).isEmpty()
            .flatMapObservable(new Function<Boolean, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Boolean t1) {
                    return Observable.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
            });

        assertEquals((Object)2, source.blockingFirst());
    }

    @Test
    public void predicateThrowsExceptionAndValueInCauseMessage() {
        TestObserverEx<Boolean> to = new TestObserverEx<>();
        final IllegalArgumentException ex = new IllegalArgumentException();

        Observable.just("Boo!").any(new Predicate<String>() {
            @Override
            public boolean test(String v) {
                throw ex;
            }
        }).subscribe(to);

        to.assertTerminated();
        to.assertNoValues();
        to.assertNotComplete();
        to.assertError(ex);
        // FIXME value as last cause?
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).any(Functions.alwaysTrue()).toObservable());

        TestHelper.checkDisposed(Observable.just(1).any(Functions.alwaysTrue()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Boolean>>() {
            @Override
            public ObservableSource<Boolean> apply(Observable<Object> o) throws Exception {
                return o.any(Functions.alwaysTrue()).toObservable();
            }
        });
        TestHelper.checkDoubleOnSubscribeObservableToSingle(new Function<Observable<Object>, SingleSource<Boolean>>() {
            @Override
            public SingleSource<Boolean> apply(Observable<Object> o) throws Exception {
                return o.any(Functions.alwaysTrue());
            }
        });
    }

    @Test
    public void predicateThrowsSuppressOthers() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());

                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onError(new IOException());
                    observer.onComplete();
                }
            }
            .any(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
                    throw new TestException();
                }
            })
            .toObservable()
            .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, IOException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSourceSingle() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onError(new TestException("First"));

                    observer.onNext(1);
                    observer.onError(new TestException("Second"));
                    observer.onComplete();
                }
            }
            .any(Functions.alwaysTrue())
            .to(TestHelper.<Boolean>testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
