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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableSingleTest extends RxJavaTest {

    @Test
    public void singleObservable() {
        Observable<Integer> o = Observable.just(1).singleElement().toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithTooManyElementsObservable() {
        Observable<Integer> o = Observable.just(1, 2).singleElement().toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithEmptyObservable() {
        Observable<Integer> o = Observable.<Integer> empty().singleElement().toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithPredicateObservable() {
        Observable<Integer> o = Observable.just(1, 2)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement().toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithPredicateAndTooManyElementsObservable() {
        Observable<Integer> o = Observable.just(1, 2, 3, 4)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement().toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithPredicateAndEmptyObservable() {
        Observable<Integer> o = Observable.just(1)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement().toObservable();
        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultObservable() {
        Observable<Integer> o = Observable.just(1).single(2).toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithTooManyElementsObservable() {
        Observable<Integer> o = Observable.just(1, 2).single(3).toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithEmptyObservable() {
        Observable<Integer> o = Observable.<Integer> empty()
                .single(1).toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithPredicateObservable() {
        Observable<Integer> o = Observable.just(1, 2)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(4).toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithPredicateAndTooManyElementsObservable() {
        Observable<Integer> o = Observable.just(1, 2, 3, 4)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(6).toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithPredicateAndEmptyObservable() {
        Observable<Integer> o = Observable.just(1)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(2).toObservable();

        Observer<Integer> observer = TestHelper.mockObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void issue1527Observable() throws InterruptedException {
        //https://github.com/ReactiveX/RxJava/pull/1527
        Observable<Integer> source = Observable.just(1, 2, 3, 4, 5, 6);
        Observable<Integer> reduced = source.reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer i1, Integer i2) {
                return i1 + i2;
            }
        }).toObservable();

        Integer r = reduced.blockingFirst();
        assertEquals(21, r.intValue());
    }

    @Test
    public void single() {
        Maybe<Integer> o = Observable.just(1).singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(1);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithTooManyElements() {
        Maybe<Integer> o = Observable.just(1, 2).singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithEmpty() {
        Maybe<Integer> o = Observable.<Integer> empty().singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithPredicate() {
        Maybe<Integer> o = Observable.just(1, 2)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(2);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithPredicateAndTooManyElements() {
        Maybe<Integer> o = Observable.just(1, 2, 3, 4)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleWithPredicateAndEmpty() {
        Maybe<Integer> o = Observable.just(1)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .singleElement();
        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefault() {
        Single<Integer> o = Observable.just(1).single(2);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(1);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithTooManyElements() {
        Single<Integer> o = Observable.just(1, 2).single(3);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithEmpty() {
        Single<Integer> o = Observable.<Integer> empty()
                .single(1);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(1);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithPredicate() {
        Single<Integer> o = Observable.just(1, 2)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(4);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(2);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithPredicateAndTooManyElements() {
        Single<Integer> o = Observable.just(1, 2, 3, 4)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(6);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void singleOrDefaultWithPredicateAndEmpty() {
        Single<Integer> o = Observable.just(1)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(2);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(2);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void issue1527() throws InterruptedException {
        //https://github.com/ReactiveX/RxJava/pull/1527
        Observable<Integer> source = Observable.just(1, 2, 3, 4, 5, 6);
        Maybe<Integer> reduced = source.reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer i1, Integer i2) {
                return i1 + i2;
            }
        });

        Integer r = reduced.blockingGet();
        assertEquals(21, r.intValue());
    }

    @Test
    public void singleElementOperatorDoNotSwallowExceptionWhenDone() {
        final Throwable exception = new RuntimeException("some error");
        final AtomicReference<Throwable> error = new AtomicReference<>();

        try {
            RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
                @Override public void accept(final Throwable throwable) throws Exception {
                    error.set(throwable);
                }
            });

            Observable.unsafeCreate(new ObservableSource<Integer>() {
                @Override public void subscribe(final Observer<? super Integer> observer) {
                    observer.onComplete();
                    observer.onError(exception);
                }
            }).singleElement().test().assertComplete();

            assertSame(exception, error.get().getCause());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void singleOrErrorNoElement() {
        Observable.empty()
            .singleOrError()
            .test()
            .assertNoValues()
            .assertError(NoSuchElementException.class);
    }

    @Test
    public void singleOrErrorOneElement() {
        Observable.just(1)
            .singleOrError()
            .test()
            .assertNoErrors()
            .assertValue(1);
    }

    @Test
    public void singleOrErrorMultipleElements() {
        Observable.just(1, 2, 3)
            .singleOrError()
            .test()
            .assertNoValues()
            .assertError(IllegalArgumentException.class);
    }

    @Test
    public void singleOrErrorError() {
        Observable.error(new RuntimeException("error"))
            .singleOrError()
            .to(TestHelper.testConsumer())
            .assertNoValues()
            .assertErrorMessage("error")
            .assertError(RuntimeException.class);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceObservable(new Function<Observable<Object>, Object>() {
            @Override
            public Object apply(Observable<Object> o) throws Exception {
                return o.singleOrError();
            }
        }, false, 1, 1, 1);

        TestHelper.checkBadSourceObservable(new Function<Observable<Object>, Object>() {
            @Override
            public Object apply(Observable<Object> o) throws Exception {
                return o.singleElement();
            }
        }, false, 1, 1, 1);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservableToSingle(new Function<Observable<Object>, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> apply(Observable<Object> o) throws Exception {
                return o.singleOrError();
            }
        });

        TestHelper.checkDoubleOnSubscribeObservableToMaybe(new Function<Observable<Object>, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> apply(Observable<Object> o) throws Exception {
                return o.singleElement();
            }
        });
    }

    @Test
    public void singleOrError() {
        Observable.empty()
        .singleOrError()
        .toObservable()
        .test()
        .assertFailure(NoSuchElementException.class);
    }
}
