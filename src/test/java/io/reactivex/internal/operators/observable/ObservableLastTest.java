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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;

public class ObservableLastTest {

    @Test
    public void testLastWithElements() {
        Maybe<Integer> last = Observable.just(1, 2, 3).lastElement();
        assertEquals(3, last.blockingGet().intValue());
    }

    @Test
    public void testLastWithNoElements() {
        Maybe<?> last = Observable.empty().lastElement();
        assertNull(last.blockingGet());
    }

    @Test
    public void testLastMultiSubscribe() {
        Maybe<Integer> last = Observable.just(1, 2, 3).lastElement();
        assertEquals(3, last.blockingGet().intValue());
        assertEquals(3, last.blockingGet().intValue());
    }

    @Test
    public void testLastViaObservable() {
        Observable.just(1, 2, 3).lastElement();
    }

    @Test
    public void testLast() {
        Maybe<Integer> o = Observable.just(1, 2, 3).lastElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(3);
//        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithOneElement() {
        Maybe<Integer> o = Observable.just(1).lastElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(1);
//        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithEmpty() {
        Maybe<Integer> o = Observable.<Integer> empty().lastElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithPredicate() {
        Maybe<Integer> o = Observable.just(1, 2, 3, 4, 5, 6)
                .filter(new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .lastElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(6);
//        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithPredicateAndOneElement() {
        Maybe<Integer> o = Observable.just(1, 2)
            .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
            .lastElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(2);
//        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithPredicateAndEmpty() {
        Maybe<Integer> o = Observable.just(1)
            .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                }).lastElement();

        MaybeObserver<Integer> observer = TestHelper.mockMaybeObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onComplete();
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefault() {
        Single<Integer> o = Observable.just(1, 2, 3)
                .last(4);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(3);
//        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithOneElement() {
        Single<Integer> o = Observable.just(1).last(2);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(1);
//        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithEmpty() {
        Single<Integer> o = Observable.<Integer> empty()
                .last(1);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(1);
//        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithPredicate() {
        Single<Integer> o = Observable.just(1, 2, 3, 4, 5, 6)
                .filter(new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .last(8);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(6);
//        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithPredicateAndOneElement() {
        Single<Integer> o = Observable.just(1, 2)
                .filter(new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .last(4);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(2);
//        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithPredicateAndEmpty() {
        Single<Integer> o = Observable.just(1)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .last(2);

        SingleObserver<Integer> observer = TestHelper.mockSingleObserver();
        o.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onSuccess(2);
//        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void lastOrErrorNoElement() {
        Observable.empty()
            .lastOrError()
            .test()
            .assertNoValues()
            .assertError(NoSuchElementException.class);
    }

    @Test
    public void lastOrErrorOneElement() {
        Observable.just(1)
            .lastOrError()
            .test()
            .assertNoErrors()
            .assertValue(1);
    }

    @Test
    public void lastOrErrorMultipleElements() {
        Observable.just(1, 2, 3)
            .lastOrError()
            .test()
            .assertNoErrors()
            .assertValue(3);
    }

    @Test
    public void lastOrErrorError() {
        Observable.error(new RuntimeException("error"))
            .lastOrError()
            .test()
            .assertNoValues()
            .assertErrorMessage("error")
            .assertError(RuntimeException.class);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.never().lastElement().toObservable());
        TestHelper.checkDisposed(Observable.never().lastElement());

        TestHelper.checkDisposed(Observable.just(1).lastOrError().toObservable());
        TestHelper.checkDisposed(Observable.just(1).lastOrError());

        TestHelper.checkDisposed(Observable.just(1).last(2).toObservable());
        TestHelper.checkDisposed(Observable.just(1).last(2));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservableToMaybe(new Function<Observable<Object>, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> apply(Observable<Object> o) throws Exception {
                return o.lastElement();
            }
        });
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.lastElement().toObservable();
            }
        });

        TestHelper.checkDoubleOnSubscribeObservableToSingle(new Function<Observable<Object>, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> apply(Observable<Object> o) throws Exception {
                return o.lastOrError();
            }
        });
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.lastOrError().toObservable();
            }
        });

        TestHelper.checkDoubleOnSubscribeObservableToSingle(new Function<Observable<Object>, SingleSource<Object>>() {
            @Override
            public SingleSource<Object> apply(Observable<Object> o) throws Exception {
                return o.last(2);
            }
        });
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.last(2).toObservable();
            }
        });
    }

    @Test
    public void error() {
        Observable.error(new TestException())
        .lastElement()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorLastOrErrorObservable() {
        Observable.error(new TestException())
        .lastOrError()
        .toObservable()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyLastOrErrorObservable() {
        Observable.empty()
        .lastOrError()
        .toObservable()
        .test()
        .assertFailure(NoSuchElementException.class);
    }
}
