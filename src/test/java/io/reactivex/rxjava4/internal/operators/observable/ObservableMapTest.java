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

package io.reactivex.rxjava4.internal.operators.observable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.*;

import io.reactivex.rxjava4.core.Observable;
import io.reactivex.rxjava4.core.Observer;
import io.reactivex.rxjava4.core.RxJavaTest;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.internal.functions.Functions;
import io.reactivex.rxjava4.operators.QueueFuseable;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.subjects.UnicastSubject;
import io.reactivex.rxjava4.testsupport.*;

public class ObservableMapTest extends RxJavaTest {

    Observer<String> stringObserver;
    Observer<String> stringObserver2;

    static final BiFunction<String, Integer, String> APPEND_INDEX = (value, index) -> value + index;

    @BeforeEach
    public void before() {
        stringObserver = TestHelper.mockObserver();
        stringObserver2 = TestHelper.mockObserver();
    }

    @Test
    public void map() {
        Map<String, String> m1 = getMap("One");
        Map<String, String> m2 = getMap("Two");
        Observable<Map<String, String>> o = Observable.just(m1, m2);

        Observable<String> m = o.map(map -> map.get("firstName"));

        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onNext("OneFirst");
        verify(stringObserver, times(1)).onNext("TwoFirst");
        verify(stringObserver, times(1)).onComplete();
    }

    @Test
    public void mapMany() {
        /* simulate a top-level async call which returns IDs */
        Observable<Integer> ids = Observable.just(1, 2);

        /* now simulate the behavior to take those IDs and perform nested async calls based on them */
        Observable<String> m = ids.flatMap((Function<Integer, Observable<String>>) id -> {
            /* simulate making a nested async call which creates another Observable */
            Observable<Map<String, String>> subObservable = null;
            if (id == 1) {
                Map<String, String> m1 = getMap("One");
                Map<String, String> m2 = getMap("Two");
                subObservable = Observable.just(m1, m2);
            } else {
                Map<String, String> m3 = getMap("Three");
                Map<String, String> m4 = getMap("Four");
                subObservable = Observable.just(m3, m4);
            }

            /* simulate kicking off the async call and performing a select on it to transform the data */
            return subObservable.map(map -> map.get("firstName"));
        });
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onNext("OneFirst");
        verify(stringObserver, times(1)).onNext("TwoFirst");
        verify(stringObserver, times(1)).onNext("ThreeFirst");
        verify(stringObserver, times(1)).onNext("FourFirst");
        verify(stringObserver, times(1)).onComplete();
    }

    @Test
    public void mapMany2() {
        Map<String, String> m1 = getMap("One");
        Map<String, String> m2 = getMap("Two");
        Observable<Map<String, String>> observable1 = Observable.just(m1, m2);

        Map<String, String> m3 = getMap("Three");
        Map<String, String> m4 = getMap("Four");
        Observable<Map<String, String>> observable2 = Observable.just(m3, m4);

        Observable<Observable<Map<String, String>>> o = Observable.just(observable1, observable2);

        Observable<String> m = o.flatMap((Function<Observable<Map<String, String>>, Observable<String>>) o1 -> o1.map(map -> map.get("firstName")));
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onNext("OneFirst");
        verify(stringObserver, times(1)).onNext("TwoFirst");
        verify(stringObserver, times(1)).onNext("ThreeFirst");
        verify(stringObserver, times(1)).onNext("FourFirst");
        verify(stringObserver, times(1)).onComplete();

    }

    @Test
    public void mapWithError() {
        Observable<String> w = Observable.just("one", "fail", "two", "three", "fail");
        Observable<String> m = w.map(s -> {
            if ("fail".equals(s)) {
                throw new RuntimeException("Forced Failure");
            }
            return s;
        }).doOnError(Throwable::printStackTrace);

        m.subscribe(stringObserver);
        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, never()).onNext("two");
        verify(stringObserver, never()).onNext("three");
        verify(stringObserver, never()).onComplete();
        verify(stringObserver, times(1)).onError(any(Throwable.class));
    }

    @Test
    public void mapWithIssue417() {
        assertThrows(IllegalArgumentException.class, () -> {
            Observable.just(1).observeOn(Schedulers.computation())
            .map((Function<Integer, Integer>) _ -> {
                throw new IllegalArgumentException("any error");
            }).blockingSingle();
        });
    }

    @Test
    public void mapWithErrorInFuncAndThreadPoolScheduler() throws InterruptedException {
        assertThrows(IllegalArgumentException.class, () -> {
            // The error will throw in one of threads in the thread pool.
            // If map does not handle it, the error will disappear.
            // so map needs to handle the error by itself.
            Observable<String> m = Observable.just("one")
                    .observeOn(Schedulers.computation())
                    .map(_ -> {
                        throw new IllegalArgumentException("any error");
                    });

            // block for response, expecting exception thrown
            m.blockingLast();
        });
    }

    /**
     * While mapping over range(1,0).last() we expect NoSuchElementException since the sequence is empty.
     */
    @Test
    public void errorPassesThruMap() {
        assertNull(Observable.range(1, 0).lastElement().map(i -> i).blockingGet());
    }

    /**
     * We expect IllegalStateException to pass thru map.
     */
    @Test
    public void errorPassesThruMap2() {
        assertThrows(IllegalStateException.class, () -> {
            Observable.error(new IllegalStateException()).map(i -> i).blockingSingle();
        });
    }

    /**
     * We expect an ArithmeticException exception here because last() emits a single value
     * but then we divide by 0.
     */
    @Test
    public void mapWithErrorInFunc() {
        assertThrows(ArithmeticException.class, () -> {
            Observable.range(1, 1).lastElement().map(i -> i / 0).blockingGet();
        });
    }

    private static Map<String, String> getMap(String prefix) {
        Map<String, String> m = new HashMap<>();
        m.put("firstName", prefix + "First");
        m.put("lastName", prefix + "Last");
        return m;
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.range(1, 5).map(Functions.identity()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(o -> o.map(Functions.identity()));
    }

    @Test
    public void fusedSync() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

        Observable.range(1, 5)
        .map(Functions.<Integer>identity())
        .subscribe(to);

        to.assertFusionMode(QueueFuseable.SYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void fusedAsync() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

        UnicastSubject<Integer> us = UnicastSubject.create();

        us
        .map(Functions.<Integer>identity())
        .subscribe(to);

        TestHelper.emit(us, 1, 2, 3, 4, 5);

        to.assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void fusedReject() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY | QueueFuseable.BOUNDARY);

        Observable.range(1, 5)
        .map(Functions.<Integer>identity())
        .subscribe(to);

        to.assertFusionMode(QueueFuseable.NONE)
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceObservable((Function<Observable<Object>, Object>) o -> o.map(Functions.identity()), false, 1, 1, 1);
    }
}
