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

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.operators.observable.ObservableMapNotification.MapNotificationObserver;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableMapNotificationTest extends RxJavaTest {
    @Test
    public void just() {
        TestObserver<Object> to = new TestObserver<>();
        Observable.just(1)
        .flatMap(
                new Function<Integer, Observable<Object>>() {
                    @Override
                    public Observable<Object> apply(Integer item) {
                        return Observable.just((Object)(item + 1));
                    }
                },
                new Function<Throwable, Observable<Object>>() {
                    @Override
                    public Observable<Object> apply(Throwable e) {
                        return Observable.error(e);
                    }
                },
                new Supplier<Observable<Object>>() {
                    @Override
                    public Observable<Object> get() {
                        return Observable.never();
                    }
                }
        ).subscribe(to);

        to.assertNoErrors();
        to.assertNotComplete();
        to.assertValue(2);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(new Observable<Integer>() {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            protected void subscribeActual(Observer<? super Integer> observer) {
                MapNotificationObserver mn = new MapNotificationObserver(
                        observer,
                        Functions.justFunction(Observable.just(1)),
                        Functions.justFunction(Observable.just(2)),
                        Functions.justSupplier(Observable.just(3))
                );
                mn.onSubscribe(Disposable.empty());
            }
        });
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Observable<Object> o) throws Exception {
                return o.flatMap(
                        Functions.justFunction(Observable.just(1)),
                        Functions.justFunction(Observable.just(2)),
                        Functions.justSupplier(Observable.just(3))
                );
            }
        });
    }

    @Test
    public void onErrorCrash() {
        TestObserverEx<Integer> to = Observable.<Integer>error(new TestException("Outer"))
        .flatMap(Functions.justFunction(Observable.just(1)),
                new Function<Throwable, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> apply(Throwable t) throws Exception {
                        throw new TestException("Inner");
                    }
                },
                Functions.justSupplier(Observable.just(3)))
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        TestHelper.assertError(to, 0, TestException.class, "Outer");
        TestHelper.assertError(to, 1, TestException.class, "Inner");
    }
}
