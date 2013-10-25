/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Applies a function of your choosing to every item emitted by an Observable, and returns this
 * transformation as a new Observable.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/map.png">
 */
public final class OperationMap {

    /**
     * Accepts a sequence and a transformation function. Returns a sequence that is the result of
     * applying the transformation function to each item in the sequence.
     * 
     * @param sequence
     *            the input sequence.
     * @param func
     *            a function to apply to each item in the sequence.
     * @param <T>
     *            the type of the input sequence.
     * @param <R>
     *            the type of the output sequence.
     * @return a sequence that is the result of applying the transformation function to each item in the input sequence.
     */
    public static <T, R> OnSubscribeFunc<R> map(final Observable<? extends T> sequence, final Func1<? super T, ? extends R> func) {
        return mapWithIndex(sequence, new Func2<T, Integer, R>() {
                    @Override
                    public R call(T value, @SuppressWarnings("unused") Integer unused) {
                        return func.call(value);
                    }
                });
    }

    /**
     * Accepts a sequence and a transformation function. Returns a sequence that is the result of
     * applying the transformation function to each item in the sequence.
     * 
     * @param sequence
     *            the input sequence.
     * @param func
     *            a function to apply to each item in the sequence. The function gets the index of the emitted item 
     *            as additional parameter.
     * @param <T>
     *            the type of the input sequence.
     * @param <R>
     *            the type of the output sequence.
     * @return a sequence that is the result of applying the transformation function to each item in the input sequence.
     */
    public static <T, R> OnSubscribeFunc<R> mapWithIndex(final Observable<? extends T> sequence, final Func2<? super T, Integer, ? extends R> func) {
        return new OnSubscribeFunc<R>() {
            @Override
            public Subscription onSubscribe(Observer<? super R> observer) {
                return new MapObservable<T, R>(sequence, func).onSubscribe(observer);
            }
        };
    }

    /**
     * Accepts a sequence of observable sequences and a transformation function. Returns a flattened sequence that is the result of
     * applying the transformation function to each item in the sequence of each observable sequence.
     * <p>
     * The closure should return an Observable which will then be merged.
     * 
     * @param sequence
     *            the input sequence.
     * @param func
     *            a function to apply to each item in the sequence.
     * @param <T>
     *            the type of the input sequence.
     * @param <R>
     *            the type of the output sequence.
     * @return a sequence that is the result of applying the transformation function to each item in the input sequence.
     */
    public static <T, R> OnSubscribeFunc<R> mapMany(Observable<? extends T> sequence, Func1<? super T, ? extends Observable<? extends R>> func) {
        return OperationMerge.merge(Observable.create(map(sequence, func)));
    }

    /**
     * An observable sequence that is the result of applying a transformation to each item in an input sequence.
     * 
     * @param <T>
     *            the type of the input sequence.
     * @param <R>
     *            the type of the output sequence.
     */
    private static class MapObservable<T, R> implements OnSubscribeFunc<R> {
        public MapObservable(Observable<? extends T> sequence, Func2<? super T, Integer, ? extends R> func) {
            this.sequence = sequence;
            this.func = func;
        }

        private final Observable<? extends T> sequence;
        private final Func2<? super T, Integer, ? extends R> func;
        private int index;

        @Override
        public Subscription onSubscribe(final Observer<? super R> observer) {
            final SafeObservableSubscription subscription = new SafeObservableSubscription();
            return subscription.wrap(sequence.subscribe(new SafeObserver<T>(subscription, new Observer<T>() {
                @Override
                public void onNext(T value) {
                    observer.onNext(func.call(value, index));
                    index++;
                }

                @Override
                public void onError(Throwable ex) {
                    observer.onError(ex);
                }

                @Override
                public void onCompleted() {
                    observer.onCompleted();
                }
            })));
        }
    }

    public static class UnitTest {
        @Mock
        Observer<String> stringObserver;
        @Mock
        Observer<String> stringObserver2;

        final static Func2<String, Integer, String> APPEND_INDEX = new Func2<String, Integer, String>() {
            @Override
            public String call(String value, Integer index) {
                return value + index;
            }
        };
        
        @Before
        public void before() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testMap() {
            Map<String, String> m1 = getMap("One");
            Map<String, String> m2 = getMap("Two");
            Observable<Map<String, String>> observable = Observable.from(m1, m2);

            Observable<String> m = Observable.create(map(observable, new Func1<Map<String, String>, String>() {

                @Override
                public String call(Map<String, String> map) {
                    return map.get("firstName");
                }

            }));
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Throwable.class));
            verify(stringObserver, times(1)).onNext("OneFirst");
            verify(stringObserver, times(1)).onNext("TwoFirst");
            verify(stringObserver, times(1)).onCompleted();
        }

        @Test
        public void testMapWithIndex() {
            Observable<String> w = Observable.from("a", "b", "c");
            Observable<String> m = Observable.create(mapWithIndex(w, APPEND_INDEX));
            m.subscribe(stringObserver);
            InOrder inOrder = inOrder(stringObserver);
            inOrder.verify(stringObserver, times(1)).onNext("a0");
            inOrder.verify(stringObserver, times(1)).onNext("b1");
            inOrder.verify(stringObserver, times(1)).onNext("c2");
            inOrder.verify(stringObserver, times(1)).onCompleted();
            verify(stringObserver, never()).onError(any(Throwable.class));
        }
        
        @Test
        public void testMapWithIndexAndMultipleSubscribers() {
            Observable<String> w = Observable.from("a", "b", "c");
            Observable<String> m = Observable.create(mapWithIndex(w, APPEND_INDEX));
            m.subscribe(stringObserver);
            m.subscribe(stringObserver2);
            InOrder inOrder = inOrder(stringObserver);
            inOrder.verify(stringObserver, times(1)).onNext("a0");
            inOrder.verify(stringObserver, times(1)).onNext("b1");
            inOrder.verify(stringObserver, times(1)).onNext("c2");
            inOrder.verify(stringObserver, times(1)).onCompleted();
            verify(stringObserver, never()).onError(any(Throwable.class));

            InOrder inOrder2 = inOrder(stringObserver2);
            inOrder2.verify(stringObserver2, times(1)).onNext("a0");
            inOrder2.verify(stringObserver2, times(1)).onNext("b1");
            inOrder2.verify(stringObserver2, times(1)).onNext("c2");
            inOrder2.verify(stringObserver2, times(1)).onCompleted();
            verify(stringObserver2, never()).onError(any(Throwable.class));
        }
        
        @Test
        public void testMapMany() {
            /* simulate a top-level async call which returns IDs */
            Observable<Integer> ids = Observable.from(1, 2);

            /* now simulate the behavior to take those IDs and perform nested async calls based on them */
            Observable<String> m = Observable.create(mapMany(ids, new Func1<Integer, Observable<String>>() {

                @Override
                public Observable<String> call(Integer id) {
                    /* simulate making a nested async call which creates another Observable */
                    Observable<Map<String, String>> subObservable = null;
                    if (id == 1) {
                        Map<String, String> m1 = getMap("One");
                        Map<String, String> m2 = getMap("Two");
                        subObservable = Observable.from(m1, m2);
                    } else {
                        Map<String, String> m3 = getMap("Three");
                        Map<String, String> m4 = getMap("Four");
                        subObservable = Observable.from(m3, m4);
                    }

                    /* simulate kicking off the async call and performing a select on it to transform the data */
                    return Observable.create(map(subObservable, new Func1<Map<String, String>, String>() {
                        @Override
                        public String call(Map<String, String> map) {
                            return map.get("firstName");
                        }
                    }));
                }

            }));
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Throwable.class));
            verify(stringObserver, times(1)).onNext("OneFirst");
            verify(stringObserver, times(1)).onNext("TwoFirst");
            verify(stringObserver, times(1)).onNext("ThreeFirst");
            verify(stringObserver, times(1)).onNext("FourFirst");
            verify(stringObserver, times(1)).onCompleted();
        }

        @Test
        public void testMapMany2() {
            Map<String, String> m1 = getMap("One");
            Map<String, String> m2 = getMap("Two");
            Observable<Map<String, String>> observable1 = Observable.from(m1, m2);

            Map<String, String> m3 = getMap("Three");
            Map<String, String> m4 = getMap("Four");
            Observable<Map<String, String>> observable2 = Observable.from(m3, m4);

            Observable<Observable<Map<String, String>>> observable = Observable.from(observable1, observable2);

            Observable<String> m = Observable.create(mapMany(observable, new Func1<Observable<Map<String, String>>, Observable<String>>() {

                @Override
                public Observable<String> call(Observable<Map<String, String>> o) {
                    return Observable.create(map(o, new Func1<Map<String, String>, String>() {

                        @Override
                        public String call(Map<String, String> map) {
                            return map.get("firstName");
                        }
                    }));
                }

            }));
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Throwable.class));
            verify(stringObserver, times(1)).onNext("OneFirst");
            verify(stringObserver, times(1)).onNext("TwoFirst");
            verify(stringObserver, times(1)).onNext("ThreeFirst");
            verify(stringObserver, times(1)).onNext("FourFirst");
            verify(stringObserver, times(1)).onCompleted();

        }

        @Test
        public void testMapWithError() {
            Observable<String> w = Observable.from("one", "fail", "two", "three", "fail");
            Observable<String> m = Observable.create(map(w, new Func1<String, String>() {
                @Override
                public String call(String s) {
                    if ("fail".equals(s)) {
                        throw new RuntimeException("Forced Failure");
                    }
                    return s;
                }
            }));
            
            m.subscribe(stringObserver);
            verify(stringObserver, times(1)).onNext("one");
            verify(stringObserver, never()).onNext("two");
            verify(stringObserver, never()).onNext("three");
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(1)).onError(any(Throwable.class));
        }
        
        @Test
        public void testMapWithSynchronousObservableContainingError() {
            Observable<String> w = Observable.from("one", "fail", "two", "three", "fail");
            final AtomicInteger c1 = new AtomicInteger();
            final AtomicInteger c2 = new AtomicInteger();
            Observable<String> m = Observable.create(map(w, new Func1<String, String>() {
                @Override
                public String call(String s) {
                    if ("fail".equals(s))
                        throw new RuntimeException("Forced Failure");
                    System.out.println("BadMapper:" + s);
                    c1.incrementAndGet();
                    return s;
                }
            })).map(new Func1<String, String>() {
                @Override
                public String call(String s) {
                    System.out.println("SecondMapper:" + s);
                    c2.incrementAndGet();
                    return s;
                }
            });

            m.subscribe(stringObserver);

            verify(stringObserver, times(1)).onNext("one");
            verify(stringObserver, never()).onNext("two");
            verify(stringObserver, never()).onNext("three");
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(1)).onError(any(Throwable.class));

            // we should have only returned 1 value: "one"
            assertEquals(1, c1.get());
            assertEquals(1, c2.get());
        }

        @Test(expected = IllegalArgumentException.class)
        public void testMapWithIssue417() {
            Observable.from(1).observeOn(Schedulers.threadPoolForComputation())
            .map(new Func1<Integer, Integer>() {
                public Integer call(Integer arg0) {
                    throw new IllegalArgumentException("any error");
                }
            }).toBlockingObservable().single();
        }

        @Test
        public void testMapWithErrorInFuncAndThreadPoolScheduler() throws InterruptedException {
            // The error will throw in one of threads in the thread pool.
            // If map does not handle it, the error will disappear.
            // so map needs to handle the error by itself.
            final CountDownLatch latch = new CountDownLatch(1);
            Observable<String> m = Observable.from("one")
                    .observeOn(Schedulers.threadPoolForComputation())
                    .map(new Func1<String, String>() {
                        public String call(String arg0) {
                            try {
                                throw new IllegalArgumentException("any error");
                            } finally {
                                latch.countDown();
                            }
                        }
                    });

            m.subscribe(stringObserver);
            latch.await();
            InOrder inorder = inOrder(stringObserver);
            inorder.verify(stringObserver, times(1)).onError(any(IllegalArgumentException.class));
            inorder.verifyNoMoreInteractions();
        }

        private static Map<String, String> getMap(String prefix) {
            Map<String, String> m = new HashMap<String, String>();
            m.put("firstName", prefix + "First");
            m.put("lastName", prefix + "Last");
            return m;
        }

    }
}
