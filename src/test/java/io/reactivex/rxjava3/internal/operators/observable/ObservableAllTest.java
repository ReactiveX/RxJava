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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableAllTest extends RxJavaTest {

    @Test
    public void allObservable() {
        Observable<String> obs = Observable.just("one", "two", "six");

        Observer <Boolean> observer = TestHelper.mockObserver();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        }).toObservable()
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onNext(true);
        verify(observer).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void notAllObservable() {
        Observable<String> obs = Observable.just("one", "two", "three", "six");

        Observer <Boolean> observer = TestHelper.mockObserver();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        }).toObservable()
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onNext(false);
        verify(observer).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void emptyObservable() {
        Observable<String> obs = Observable.empty();

        Observer <Boolean> observer = TestHelper.mockObserver();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        }).toObservable()
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onNext(true);
        verify(observer).onComplete();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void errorObservable() {
        Throwable error = new Throwable();
        Observable<String> obs = Observable.error(error);

        Observer <Boolean> observer = TestHelper.mockObserver();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        }).toObservable()
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onError(error);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void followingFirstObservable() {
        Observable<Integer> o = Observable.fromArray(1, 3, 5, 6);
        Observable<Boolean> allOdd = o.all(new Predicate<Integer>() {
            @Override
            public boolean test(Integer i) {
                return i % 2 == 1;
            }
        }).toObservable();

        assertFalse(allOdd.blockingFirst());
    }

    @Test
    public void issue1935NoUnsubscribeDownstreamObservable() {
        Observable<Integer> source = Observable.just(1)
            .all(new Predicate<Integer>() {
                @Override
                public boolean test(Integer t1) {
                    return false;
                }
            }).toObservable()
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

        Observable.just("Boo!").all(new Predicate<String>() {
            @Override
            public boolean test(String v) {
                throw ex;
            }
        })
        .subscribe(to);

        to.assertTerminated();
        to.assertNoValues();
        to.assertNotComplete();
        to.assertError(ex);
        // FIXME need to decide about adding the value that probably caused the crash in some way
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }

    @Test
    public void all() {
        Observable<String> obs = Observable.just("one", "two", "six");

        SingleObserver<Boolean> observer = TestHelper.mockSingleObserver();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onSuccess(true);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void notAll() {
        Observable<String> obs = Observable.just("one", "two", "three", "six");

        SingleObserver <Boolean> observer = TestHelper.mockSingleObserver();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onSuccess(false);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void empty() {
        Observable<String> obs = Observable.empty();

        SingleObserver <Boolean> observer = TestHelper.mockSingleObserver();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onSuccess(true);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void error() {
        Throwable error = new Throwable();
        Observable<String> obs = Observable.error(error);

        SingleObserver <Boolean> observer = TestHelper.mockSingleObserver();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(observer);

        verify(observer).onSubscribe((Disposable)any());
        verify(observer).onError(error);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void followingFirst() {
        Observable<Integer> o = Observable.fromArray(1, 3, 5, 6);
        Single<Boolean> allOdd = o.all(new Predicate<Integer>() {
            @Override
            public boolean test(Integer i) {
                return i % 2 == 1;
            }
        });

        assertFalse(allOdd.blockingGet());
    }

    @Test
    public void issue1935NoUnsubscribeDownstream() {
        Observable<Integer> source = Observable.just(1)
            .all(new Predicate<Integer>() {
                @Override
                public boolean test(Integer t1) {
                    return false;
                }
            })
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

        Observable.just("Boo!").all(new Predicate<String>() {
            @Override
            public boolean test(String v) {
                throw ex;
            }
        })
        .subscribe(to);

        to.assertTerminated();
        to.assertNoValues();
        to.assertNotComplete();
        to.assertError(ex);
        // FIXME need to decide about adding the value that probably caused the crash in some way
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).all(Functions.alwaysTrue()).toObservable());

        TestHelper.checkDisposed(Observable.just(1).all(Functions.alwaysTrue()));
    }

    @Test
    public void predicateThrowsObservable() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());

                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .all(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
                    throw new TestException();
                }
            })
            .toObservable()
            .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void predicateThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());

                    observer.onNext(1);
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .all(new Predicate<Integer>() {
                @Override
                public boolean test(Integer v) throws Exception {
                    throw new TestException();
                }
            })
            .test()
            .assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
