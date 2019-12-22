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

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.UnicastSubject;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableMapTest extends RxJavaTest {

    Observer<String> stringObserver;
    Observer<String> stringObserver2;

    static final BiFunction<String, Integer, String> APPEND_INDEX = new BiFunction<String, Integer, String>() {
        @Override
        public String apply(String value, Integer index) {
            return value + index;
        }
    };

    @Before
    public void before() {
        stringObserver = TestHelper.mockObserver();
        stringObserver2 = TestHelper.mockObserver();
    }

    @Test
    public void map() {
        Map<String, String> m1 = getMap("One");
        Map<String, String> m2 = getMap("Two");
        Observable<Map<String, String>> o = Observable.just(m1, m2);

        Observable<String> m = o.map(new Function<Map<String, String>, String>() {
            @Override
            public String apply(Map<String, String> map) {
                return map.get("firstName");
            }
        });

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
        Observable<String> m = ids.flatMap(new Function<Integer, Observable<String>>() {

            @Override
            public Observable<String> apply(Integer id) {
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
                return subObservable.map(new Function<Map<String, String>, String>() {
                    @Override
                    public String apply(Map<String, String> map) {
                        return map.get("firstName");
                    }
                });
            }

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

        Observable<String> m = o.flatMap(new Function<Observable<Map<String, String>>, Observable<String>>() {

            @Override
            public Observable<String> apply(Observable<Map<String, String>> o) {
                return o.map(new Function<Map<String, String>, String>() {

                    @Override
                    public String apply(Map<String, String> map) {
                        return map.get("firstName");
                    }
                });
            }

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
    public void mapWithError() {
        Observable<String> w = Observable.just("one", "fail", "two", "three", "fail");
        Observable<String> m = w.map(new Function<String, String>() {
            @Override
            public String apply(String s) {
                if ("fail".equals(s)) {
                    throw new RuntimeException("Forced Failure");
                }
                return s;
            }
        }).doOnError(new Consumer<Throwable>() {

            @Override
            public void accept(Throwable t1) {
                t1.printStackTrace();
            }

        });

        m.subscribe(stringObserver);
        verify(stringObserver, times(1)).onNext("one");
        verify(stringObserver, never()).onNext("two");
        verify(stringObserver, never()).onNext("three");
        verify(stringObserver, never()).onComplete();
        verify(stringObserver, times(1)).onError(any(Throwable.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapWithIssue417() {
        Observable.just(1).observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer arg0) {
                        throw new IllegalArgumentException("any error");
                    }
                }).blockingSingle();
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapWithErrorInFuncAndThreadPoolScheduler() throws InterruptedException {
        // The error will throw in one of threads in the thread pool.
        // If map does not handle it, the error will disappear.
        // so map needs to handle the error by itself.
        Observable<String> m = Observable.just("one")
                .observeOn(Schedulers.computation())
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String arg0) {
                        throw new IllegalArgumentException("any error");
                    }
                });

        // block for response, expecting exception thrown
        m.blockingLast();
    }

    /**
     * While mapping over range(1,0).last() we expect NoSuchElementException since the sequence is empty.
     */
    @Test
    public void errorPassesThruMap() {
        assertNull(Observable.range(1, 0).lastElement().map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer i) {
                return i;
            }

        }).blockingGet());
    }

    /**
     * We expect IllegalStateException to pass thru map.
     */
    @Test(expected = IllegalStateException.class)
    public void errorPassesThruMap2() {
        Observable.error(new IllegalStateException()).map(new Function<Object, Object>() {

            @Override
            public Object apply(Object i) {
                return i;
            }

        }).blockingSingle();
    }

    /**
     * We expect an ArithmeticException exception here because last() emits a single value
     * but then we divide by 0.
     */
    @Test(expected = ArithmeticException.class)
    public void mapWithErrorInFunc() {
        Observable.range(1, 1).lastElement().map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer i) {
                return i / 0;
            }

        }).blockingGet();
    }

    // FIXME RS subscribers can't throw
//    @Test(expected = OnErrorNotImplementedException.class)
//    public void verifyExceptionIsThrownIfThereIsNoExceptionHandler() {
//
//        ObservableSource<Object> creator = new ObservableSource<Object>() {
//
//            @Override
//            public void subscribeActual(Observer<? super Object> observer) {
//                observer.onSubscribe(EmptyDisposable.INSTANCE);
//                observer.onNext("a");
//                observer.onNext("b");
//                observer.onNext("c");
//                observer.onComplete();
//            }
//        };
//
//        Function<Object, Observable<Object>> manyMapper = new Function<Object, Observable<Object>>() {
//
//            @Override
//            public Observable<Object> apply(Object object) {
//                return Observable.just(object);
//            }
//        };
//
//        Function<Object, Object> mapper = new Function<Object, Object>() {
//            private int count = 0;
//
//            @Override
//            public Object apply(Object object) {
//                ++count;
//                if (count > 2) {
//                    throw new RuntimeException();
//                }
//                return object;
//            }
//        };
//
//        Consumer<Object> onNext = new Consumer<Object>() {
//
//            @Override
//            public void accept(Object object) {
//                System.out.println(object.toString());
//            }
//        };
//
//        try {
//            Observable.unsafeCreate(creator).flatMap(manyMapper).map(mapper).subscribe(onNext);
//        } catch (RuntimeException e) {
//            e.printStackTrace();
//            throw e;
//        }
//    }

    private static Map<String, String> getMap(String prefix) {
        Map<String, String> m = new HashMap<>();
        m.put("firstName", prefix + "First");
        m.put("lastName", prefix + "Last");
        return m;
    }

    // FIXME RS subscribers can't throw
//    @Test(expected = OnErrorNotImplementedException.class)
//    public void testShouldNotSwallowOnErrorNotImplementedException() {
//        Observable.just("a", "b").flatMap(new Function<String, Observable<String>>() {
//            @Override
//            public Observable<String> apply(String s) {
//                return Observable.just(s + "1", s + "2");
//            }
//        }).flatMap(new Function<String, Observable<String>>() {
//            @Override
//            public Observable<String> apply(String s) {
//                return Observable.error(new Exception("test"));
//            }
//        }).forEach(new Consumer<String>() {
//            @Override
//            public void accept(String s) {
//                System.out.println(s);
//            }
//        });
//    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.range(1, 5).map(Functions.identity()));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.map(Functions.identity());
            }
        });
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
        TestHelper.checkBadSourceObservable(new Function<Observable<Object>, Object>() {
            @Override
            public Object apply(Observable<Object> o) throws Exception {
                return o.map(Functions.identity());
            }
        }, false, 1, 1, 1);
    }
}
