/*
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.mockito.Mockito;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.util.ExceptionHelper;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableToListTest extends RxJavaTest {

    @Test
    public void listObservable() {
        Observable<String> w = Observable.fromIterable(Arrays.asList("one", "two", "three"));
        Observable<List<String>> observable = w.toList().toObservable();

        Observer<List<String>> observer = TestHelper.mockObserver();
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void listViaObservableObservable() {
        Observable<String> w = Observable.fromIterable(Arrays.asList("one", "two", "three"));
        Observable<List<String>> observable = w.toList().toObservable();

        Observer<List<String>> observer = TestHelper.mockObserver();
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void listMultipleSubscribersObservable() {
        Observable<String> w = Observable.fromIterable(Arrays.asList("one", "two", "three"));
        Observable<List<String>> observable = w.toList().toObservable();

        Observer<List<String>> o1 = TestHelper.mockObserver();
        observable.subscribe(o1);

        Observer<List<String>> o2 = TestHelper.mockObserver();
        observable.subscribe(o2);

        List<String> expected = Arrays.asList("one", "two", "three");

        verify(o1, times(1)).onNext(expected);
        verify(o1, Mockito.never()).onError(any(Throwable.class));
        verify(o1, times(1)).onComplete();

        verify(o2, times(1)).onNext(expected);
        verify(o2, Mockito.never()).onError(any(Throwable.class));
        verify(o2, times(1)).onComplete();
    }

    @Test
    public void listWithBlockingFirstObservable() {
        Observable<String> o = Observable.fromIterable(Arrays.asList("one", "two", "three"));
        List<String> actual = o.toList().toObservable().blockingFirst();
        Assert.assertEquals(Arrays.asList("one", "two", "three"), actual);
    }

    @Test
    public void capacityHintObservable() {
        Observable.range(1, 10)
        .toList(4)
        .toObservable()
        .test()
        .assertResult(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void list() {
        Observable<String> w = Observable.fromIterable(Arrays.asList("one", "two", "three"));
        Single<List<String>> single = w.toList();

        SingleObserver<List<String>> observer = TestHelper.mockSingleObserver();
        single.subscribe(observer);
        verify(observer, times(1)).onSuccess(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
    }

    @Test
    public void listViaObservable() {
        Observable<String> w = Observable.fromIterable(Arrays.asList("one", "two", "three"));
        Single<List<String>> single = w.toList();

        SingleObserver<List<String>> observer = TestHelper.mockSingleObserver();
        single.subscribe(observer);
        verify(observer, times(1)).onSuccess(Arrays.asList("one", "two", "three"));
        verify(observer, Mockito.never()).onError(any(Throwable.class));
    }

    @Test
    public void listMultipleSubscribers() {
        Observable<String> w = Observable.fromIterable(Arrays.asList("one", "two", "three"));
        Single<List<String>> single = w.toList();

        SingleObserver<List<String>> o1 = TestHelper.mockSingleObserver();
        single.subscribe(o1);

        SingleObserver<List<String>> o2 = TestHelper.mockSingleObserver();
        single.subscribe(o2);

        List<String> expected = Arrays.asList("one", "two", "three");

        verify(o1, times(1)).onSuccess(expected);
        verify(o1, Mockito.never()).onError(any(Throwable.class));

        verify(o2, times(1)).onSuccess(expected);
        verify(o2, Mockito.never()).onError(any(Throwable.class));
    }

    @Test
    public void listWithBlockingFirst() {
        Observable<String> o = Observable.fromIterable(Arrays.asList("one", "two", "three"));
        List<String> actual = o.toList().blockingGet();
        Assert.assertEquals(Arrays.asList("one", "two", "three"), actual);
    }

    static void await(CyclicBarrier cb) {
        try {
            cb.await();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } catch (BrokenBarrierException ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void capacityHint() {
        Observable.range(1, 10)
        .toList(4)
        .test()
        .assertResult(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).toList().toObservable());

        TestHelper.checkDisposed(Observable.just(1).toList());
    }

    @Test
    public void error() {
        Observable.error(new TestException())
        .toList()
        .toObservable()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorSingle() {
        Observable.error(new TestException())
        .toList()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void collectionSupplierThrows() {
        Observable.just(1)
        .toList(new Supplier<Collection<Integer>>() {
            @Override
            public Collection<Integer> get() throws Exception {
                throw new TestException();
            }
        })
        .toObservable()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void collectionSupplierReturnsNull() {
        Observable.just(1)
        .toList(new Supplier<Collection<Integer>>() {
            @Override
            public Collection<Integer> get() throws Exception {
                return null;
            }
        })
        .toObservable()
        .to(TestHelper.<Collection<Integer>>testConsumer())
        .assertFailure(NullPointerException.class)
        .assertErrorMessage(ExceptionHelper.nullWarning("The collectionSupplier returned a null Collection."));
    }

    @Test
    public void singleCollectionSupplierThrows() {
        Observable.just(1)
        .toList(new Supplier<Collection<Integer>>() {
            @Override
            public Collection<Integer> get() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void singleCollectionSupplierReturnsNull() {
        Observable.just(1)
        .toList(new Supplier<Collection<Integer>>() {
            @Override
            public Collection<Integer> get() throws Exception {
                return null;
            }
        })
        .to(TestHelper.<Collection<Integer>>testConsumer())
        .assertFailure(NullPointerException.class)
        .assertErrorMessage(ExceptionHelper.nullWarning("The collectionSupplier returned a null Collection."));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, Observable<List<Object>>>() {
            @Override
            public Observable<List<Object>> apply(Observable<Object> f)
                    throws Exception {
                return f.toList().toObservable();
            }
        });
        TestHelper.checkDoubleOnSubscribeObservableToSingle(new Function<Observable<Object>, Single<List<Object>>>() {
            @Override
            public Single<List<Object>> apply(Observable<Object> f)
                    throws Exception {
                return f.toList();
            }
        });
    }
}
