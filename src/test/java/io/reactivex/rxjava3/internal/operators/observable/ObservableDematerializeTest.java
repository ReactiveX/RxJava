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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableDematerializeTest extends RxJavaTest {

    @Test
    public void simpleSelector() {
        Observable<Notification<Integer>> notifications = Observable.just(1, 2).materialize();
        Observable<Integer> dematerialize = notifications.dematerialize(Functions.<Notification<Integer>>identity());

        Observer<Integer> observer = TestHelper.mockObserver();

        dematerialize.subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void selectorCrash() {
        Observable.just(1, 2)
        .materialize()
        .dematerialize(new Function<Notification<Integer>, Notification<Object>>() {
            @Override
            public Notification<Object> apply(Notification<Integer> v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void selectorNull() {
        Observable.just(1, 2)
        .materialize()
        .dematerialize(new Function<Notification<Integer>, Notification<Object>>() {
            @Override
            public Notification<Object> apply(Notification<Integer> v) throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void dematerialize1() {
        Observable<Notification<Integer>> notifications = Observable.just(1, 2).materialize();
        Observable<Integer> dematerialize = notifications.dematerialize(Functions.<Notification<Integer>>identity());

        Observer<Integer> observer = TestHelper.mockObserver();

        dematerialize.subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void dematerialize2() {
        Throwable exception = new Throwable("test");
        Observable<Integer> o = Observable.error(exception);
        Observable<Integer> dematerialize = o.materialize().dematerialize(Functions.<Notification<Integer>>identity());

        Observer<Integer> observer = TestHelper.mockObserver();

        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void dematerialize3() {
        Exception exception = new Exception("test");
        Observable<Integer> o = Observable.error(exception);
        Observable<Integer> dematerialize = o.materialize().dematerialize(Functions.<Notification<Integer>>identity());

        Observer<Integer> observer = TestHelper.mockObserver();

        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void errorPassThru() {
        Exception exception = new Exception("test");
        Observable<Notification<Integer>> o = Observable.error(exception);
        Observable<Integer> dematerialize = o.dematerialize(Functions.<Notification<Integer>>identity());

        Observer<Integer> observer = TestHelper.mockObserver();

        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void completePassThru() {
        Observable<Notification<Integer>> o = Observable.empty();
        Observable<Integer> dematerialize = o.dematerialize(Functions.<Notification<Integer>>identity());

        Observer<Integer> observer = TestHelper.mockObserver();

        TestObserverEx<Integer> to = new TestObserverEx<>(observer);
        dematerialize.subscribe(to);

        System.out.println(to.errors());

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void honorsContractWhenCompleted() {
        Observable<Integer> source = Observable.just(1);

        Observable<Integer> result = source.materialize().dematerialize(Functions.<Notification<Integer>>identity());

        Observer<Integer> o = TestHelper.mockObserver();

        result.subscribe(o);

        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void honorsContractWhenThrows() {
        Observable<Integer> source = Observable.error(new TestException());

        Observable<Integer> result = source.materialize().dematerialize(Functions.<Notification<Integer>>identity());

        Observer<Integer> o = TestHelper.mockObserver();

        result.subscribe(o);

        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(Notification.<Integer>createOnComplete()).dematerialize(Functions.<Notification<Integer>>identity()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Notification<Object>>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Notification<Object>> o) throws Exception {
                return o.dematerialize(Functions.<Notification<Object>>identity());
            }
        });
    }

    @Test
    public void eventsAfterDematerializedTerminal() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Notification<Object>>() {
                @Override
                protected void subscribeActual(Observer<? super Notification<Object>> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onNext(Notification.createOnComplete());
                    observer.onNext(Notification.<Object>createOnNext(1));
                    observer.onNext(Notification.createOnError(new TestException("First")));
                    observer.onError(new TestException("Second"));
                }
            }
            .dematerialize(Functions.<Notification<Object>>identity())
            .test()
            .assertResult();

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "First");
            TestHelper.assertUndeliverable(errors, 1, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void nonNotificationInstanceAfterDispose() {
        new Observable<Notification<Object>>() {
            @Override
            protected void subscribeActual(Observer<? super Notification<Object>> observer) {
                observer.onSubscribe(Disposable.empty());
                observer.onNext(Notification.createOnComplete());
                observer.onNext(Notification.<Object>createOnNext(1));
            }
        }
        .dematerialize(Functions.<Notification<Object>>identity())
        .test()
        .assertResult();
    }
}
