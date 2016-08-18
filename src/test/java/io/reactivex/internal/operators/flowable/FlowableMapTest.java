/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.flowable;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.*;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableMapTest {

    Subscriber<String> stringObserver;
    Subscriber<String> stringObserver2;

    final static BiFunction<String, Integer, String> APPEND_INDEX = new BiFunction<String, Integer, String>() {
        @Override
        public String apply(String value, Integer index) {
            return value + index;
        }
    };

    @Before
    public void before() {
        stringObserver = TestHelper.mockSubscriber();
        stringObserver2 = TestHelper.mockSubscriber();
    }

    @Test
    public void testMap() {
        Map<String, String> m1 = getMap("One");
        Map<String, String> m2 = getMap("Two");
        Flowable<Map<String, String>> observable = Flowable.just(m1, m2);

        Flowable<String> m = observable.map(new Function<Map<String, String>, String>() {
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
    public void testMapMany() {
        /* simulate a top-level async call which returns IDs */
        Flowable<Integer> ids = Flowable.just(1, 2);

        /* now simulate the behavior to take those IDs and perform nested async calls based on them */
        Flowable<String> m = ids.flatMap(new Function<Integer, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Integer id) {
                /* simulate making a nested async call which creates another Observable */
                Flowable<Map<String, String>> subObservable = null;
                if (id == 1) {
                    Map<String, String> m1 = getMap("One");
                    Map<String, String> m2 = getMap("Two");
                    subObservable = Flowable.just(m1, m2);
                } else {
                    Map<String, String> m3 = getMap("Three");
                    Map<String, String> m4 = getMap("Four");
                    subObservable = Flowable.just(m3, m4);
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
    public void testMapMany2() {
        Map<String, String> m1 = getMap("One");
        Map<String, String> m2 = getMap("Two");
        Flowable<Map<String, String>> observable1 = Flowable.just(m1, m2);

        Map<String, String> m3 = getMap("Three");
        Map<String, String> m4 = getMap("Four");
        Flowable<Map<String, String>> observable2 = Flowable.just(m3, m4);

        Flowable<Flowable<Map<String, String>>> observable = Flowable.just(observable1, observable2);

        Flowable<String> m = observable.flatMap(new Function<Flowable<Map<String, String>>, Flowable<String>>() {

            @Override
            public Flowable<String> apply(Flowable<Map<String, String>> o) {
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
    public void testMapWithError() {
        Flowable<String> w = Flowable.just("one", "fail", "two", "three", "fail");
        Flowable<String> m = w.map(new Function<String, String>() {
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
    public void testMapWithIssue417() {
        Flowable.just(1).observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer arg0) {
                        throw new IllegalArgumentException("any error");
                    }
                }).blockingSingle();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapWithErrorInFuncAndThreadPoolScheduler() throws InterruptedException {
        // The error will throw in one of threads in the thread pool.
        // If map does not handle it, the error will disappear.
        // so map needs to handle the error by itself.
        Flowable<String> m = Flowable.just("one")
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
    @Test(expected = NoSuchElementException.class)
    public void testErrorPassesThruMap() {
        Flowable.range(1, 0).last().map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer i) {
                return i;
            }

        }).blockingSingle();
    }

    /**
     * We expect IllegalStateException to pass thru map.
     */
    @Test(expected = IllegalStateException.class)
    public void testErrorPassesThruMap2() {
        Flowable.error(new IllegalStateException()).map(new Function<Object, Object>() {

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
    public void testMapWithErrorInFunc() {
        Flowable.range(1, 1).last().map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer i) {
                return i / 0;
            }

        }).blockingSingle();
    }

    // FIXME RS subscribers can't throw
//    @Test(expected = OnErrorNotImplementedException.class)
//    public void verifyExceptionIsThrownIfThereIsNoExceptionHandler() {
//
//        Publisher<Object> creator = new Publisher<Object>() {
//
//            @Override
//            public void subscribe(Subscriber<? super Object> observer) {
//                observer.onSubscribe(EmptySubscription.INSTANCE);
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
//            Observable.create(creator).flatMap(manyMapper).map(mapper).subscribe(onNext);
//        } catch (RuntimeException e) {
//            e.printStackTrace();
//            throw e;
//        }
//    }

    private static Map<String, String> getMap(String prefix) {
        Map<String, String> m = new HashMap<String, String>();
        m.put("firstName", prefix + "First");
        m.put("lastName", prefix + "Last");
        return m;
    }

    @Test//(expected = OnErrorNotImplementedException.class)
    @Ignore("RS subscribers can't throw")
    public void testShouldNotSwallowOnErrorNotImplementedException() {
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
    }
    
    @Test//(expected = OnErrorNotImplementedException.class)
    @Ignore("RS subscribers can't throw")
    public void verifyExceptionIsThrownIfThereIsNoExceptionHandler() {
//
//        Observable.OnSubscribe<Object> creator = new Observable.OnSubscribe<Object>() {
//
//            @Override
//            public void call(Subscriber<? super Object> observer) {
//                observer.onNext("a");
//                observer.onNext("b");
//                observer.onNext("c");
//                observer.onCompleted();
//            }
//        };
//
//        Func1<Object, Observable<Object>> manyMapper = new Func1<Object, Observable<Object>>() {
//
//            @Override
//            public Observable<Object> call(Object object) {
//                return Observable.just(object);
//            }
//        };
//
//        Func1<Object, Object> mapper = new Func1<Object, Object>() {
//            private int count = 0;
//
//            @Override
//            public Object call(Object object) {
//                ++count;
//                if (count > 2) {
//                    throw new RuntimeException();
//                }
//                return object;
//            }
//        };
//
//        Action1<Object> onNext = new Action1<Object>() {
//
//            @Override
//            public void call(Object object) {
//                System.out.println(object.toString());
//            }
//        };
//
//        try {
//            Observable.create(creator).flatMap(manyMapper).map(mapper).subscribe(onNext);
//        } catch (RuntimeException e) {
//            e.printStackTrace();
//            throw e;
//        }
    }

    @Test
    public void functionCrashUnsubscribes() {
        
        PublishProcessor<Integer> ps = PublishProcessor.create();
        
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        ps.map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) { 
                throw new TestException(); 
            }
        }).subscribe(ts);
        
        Assert.assertTrue("Not subscribed?", ps.hasSubscribers());
        
        ps.onNext(1);
        
        Assert.assertFalse("Subscribed?", ps.hasSubscribers());
        
        ts.assertError(TestException.class);
    }
}