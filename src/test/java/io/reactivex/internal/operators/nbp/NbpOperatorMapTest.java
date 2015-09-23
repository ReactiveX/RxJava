/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.function.*;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.schedulers.Schedulers;

public class NbpOperatorMapTest {

    NbpSubscriber<String> stringObserver;
    NbpSubscriber<String> stringObserver2;

    final static BiFunction<String, Integer, String> APPEND_INDEX = new BiFunction<String, Integer, String>() {
        @Override
        public String apply(String value, Integer index) {
            return value + index;
        }
    };

    @Before
    public void before() {
        stringObserver = TestHelper.mockNbpSubscriber();
        stringObserver2 = TestHelper.mockNbpSubscriber();
    }

    @Test
    public void testMap() {
        Map<String, String> m1 = getMap("One");
        Map<String, String> m2 = getMap("Two");
        NbpObservable<Map<String, String>> o = NbpObservable.just(m1, m2);

        NbpObservable<String> m = o.map(map -> map.get("firstName"));
        
        m.subscribe(stringObserver);

        verify(stringObserver, never()).onError(any(Throwable.class));
        verify(stringObserver, times(1)).onNext("OneFirst");
        verify(stringObserver, times(1)).onNext("TwoFirst");
        verify(stringObserver, times(1)).onComplete();
    }

    @Test
    public void testMapMany() {
        /* simulate a top-level async call which returns IDs */
        NbpObservable<Integer> ids = NbpObservable.just(1, 2);

        /* now simulate the behavior to take those IDs and perform nested async calls based on them */
        NbpObservable<String> m = ids.flatMap(new Function<Integer, NbpObservable<String>>() {

            @Override
            public NbpObservable<String> apply(Integer id) {
                /* simulate making a nested async call which creates another NbpObservable */
                NbpObservable<Map<String, String>> subObservable = null;
                if (id == 1) {
                    Map<String, String> m1 = getMap("One");
                    Map<String, String> m2 = getMap("Two");
                    subObservable = NbpObservable.just(m1, m2);
                } else {
                    Map<String, String> m3 = getMap("Three");
                    Map<String, String> m4 = getMap("Four");
                    subObservable = NbpObservable.just(m3, m4);
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
        NbpObservable<Map<String, String>> observable1 = NbpObservable.just(m1, m2);

        Map<String, String> m3 = getMap("Three");
        Map<String, String> m4 = getMap("Four");
        NbpObservable<Map<String, String>> observable2 = NbpObservable.just(m3, m4);

        NbpObservable<NbpObservable<Map<String, String>>> o = NbpObservable.just(observable1, observable2);

        NbpObservable<String> m = o.flatMap(new Function<NbpObservable<Map<String, String>>, NbpObservable<String>>() {

            @Override
            public NbpObservable<String> apply(NbpObservable<Map<String, String>> o) {
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
        NbpObservable<String> w = NbpObservable.just("one", "fail", "two", "three", "fail");
        NbpObservable<String> m = w.map(new Function<String, String>() {
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
        NbpObservable.just(1).observeOn(Schedulers.computation())
                .map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer arg0) {
                        throw new IllegalArgumentException("any error");
                    }
                }).toBlocking().single();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapWithErrorInFuncAndThreadPoolScheduler() throws InterruptedException {
        // The error will throw in one of threads in the thread pool.
        // If map does not handle it, the error will disappear.
        // so map needs to handle the error by itself.
        NbpObservable<String> m = NbpObservable.just("one")
                .observeOn(Schedulers.computation())
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String arg0) {
                        throw new IllegalArgumentException("any error");
                    }
                });

        // block for response, expecting exception thrown
        m.toBlocking().last();
    }

    /**
     * While mapping over range(1,0).last() we expect NoSuchElementException since the sequence is empty.
     */
    @Test(expected = NoSuchElementException.class)
    public void testErrorPassesThruMap() {
        NbpObservable.range(1, 0).last().map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer i) {
                return i;
            }

        }).toBlocking().single();
    }

    /**
     * We expect IllegalStateException to pass thru map.
     */
    @Test(expected = IllegalStateException.class)
    public void testErrorPassesThruMap2() {
        NbpObservable.error(new IllegalStateException()).map(new Function<Object, Object>() {

            @Override
            public Object apply(Object i) {
                return i;
            }

        }).toBlocking().single();
    }

    /**
     * We expect an ArithmeticException exception here because last() emits a single value
     * but then we divide by 0.
     */
    @Test(expected = ArithmeticException.class)
    public void testMapWithErrorInFunc() {
        NbpObservable.range(1, 1).last().map(new Function<Integer, Integer>() {

            @Override
            public Integer apply(Integer i) {
                return i / 0;
            }

        }).toBlocking().single();
    }

    // FIXME RS subscribers can't throw
//    @Test(expected = OnErrorNotImplementedException.class)
//    public void verifyExceptionIsThrownIfThereIsNoExceptionHandler() {
//
//        NbpOnSubscribe<Object> creator = new NbpOnSubscribe<Object>() {
//
//            @Override
//            public void accept(NbpSubscriber<? super Object> NbpObserver) {
//                NbpObserver.onSubscribe(EmptyDisposable.INSTANCE);
//                NbpObserver.onNext("a");
//                NbpObserver.onNext("b");
//                NbpObserver.onNext("c");
//                NbpObserver.onComplete();
//            }
//        };
//
//        Function<Object, NbpObservable<Object>> manyMapper = new Function<Object, NbpObservable<Object>>() {
//
//            @Override
//            public NbpObservable<Object> apply(Object object) {
//                return NbpObservable.just(object);
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
//            NbpObservable.create(creator).flatMap(manyMapper).map(mapper).subscribe(onNext);
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
//        NbpObservable.just("a", "b").flatMap(new Function<String, NbpObservable<String>>() {
//            @Override
//            public NbpObservable<String> apply(String s) {
//                return NbpObservable.just(s + "1", s + "2");
//            }
//        }).flatMap(new Function<String, NbpObservable<String>>() {
//            @Override
//            public NbpObservable<String> apply(String s) {
//                return NbpObservable.error(new Exception("test"));
//            }
//        }).forEach(new Consumer<String>() {
//            @Override
//            public void accept(String s) {
//                System.out.println(s);
//            }
//        });
//    }
}