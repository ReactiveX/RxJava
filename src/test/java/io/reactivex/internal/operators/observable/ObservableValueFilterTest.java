/**
 * Copyright (c) 2016-present, RxJava Contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.observable;

import org.junit.Test;
import org.mockito.Mockito;

import io.reactivex.*;
import io.reactivex.observers.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.fuseable.QueueDisposable;
import io.reactivex.subjects.UnicastSubject;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ObservableValueFilterTest {

    @Test
    public void testFilter() {
        Observable<String> w = Observable.just("one", "two", "three");
        Observable<String> observable = w.filter("two");

        Observer<String> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, Mockito.never()).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testPrimitiveFilter() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Integer> observable = w.filter(3);

        Observer<Integer> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, Mockito.never()).onNext(1);
        verify(observer, Mockito.never()).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testMultipleMatches() {
        Observable<String> w = Observable.just("one", "two", "three", "two", "one");
        Observable<String> observable = w.filter("two");

        Observer<String> observer = TestHelper.mockObserver();

        observable.subscribe(observer);

        verify(observer, Mockito.never()).onNext("one");
        verify(observer, times(2)).onNext("two");
        verify(observer, Mockito.never()).onNext("three");
        verify(observer, Mockito.never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    // FIXME subscribers are not allowed to throw
//    @Test
//    public void testFatalError() {
//        try {
//            Observable.just(1)
//            .filter(new Predicate<Integer>() {
//                @Override
//                public boolean test(Integer t) {
//                    return true;
//                }
//            })
//            .first()
//            .subscribe(new Consumer<Integer>() {
//                @Override
//                public void accept(Integer t) {
//                    throw new TestException();
//                }
//            });
//            Assert.fail("No exception was thrown");
//        } catch (OnErrorNotImplementedException ex) {
//            if (!(ex.getCause() instanceof TestException)) {
//                Assert.fail("Failed to report the original exception, instead: " + ex.getCause());
//            }
//        }
//    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.range(1, 5).filter(anyInt()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.filter(1);
            }
        });
    }

    @Test
    public void fusedSync() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY);

        Observable.range(1, 5)
                .filter(3)
                .subscribe(to);

        ObserverFusion.assertFusion(to, QueueDisposable.SYNC)
                .assertResult(3);
    }

    @Test
    public void fusedAsync() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY);

        UnicastSubject<Integer> us = UnicastSubject.create();

        us
                .filter(3)
                .subscribe(to);

        TestHelper.emit(us, 1, 2, 3, 4, 5);

        ObserverFusion.assertFusion(to, QueueDisposable.ASYNC)
                .assertResult(3);
    }

    @Test
    public void fusedReject() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY | QueueDisposable.BOUNDARY);

        Observable.range(1, 5)
                .filter(3)
                .subscribe(to);

        ObserverFusion.assertFusion(to, QueueDisposable.NONE)
                .assertResult(3);
    }

}
