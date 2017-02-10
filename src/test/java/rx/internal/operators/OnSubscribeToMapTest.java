/**
 * Copyright 2014 Netflix, Inc.
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
package rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Producer;
import rx.Subscriber;
import rx.exceptions.TestException;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;
import rx.observers.TestSubscriber;
import rx.plugins.RxJavaHooks;

public class OnSubscribeToMapTest {
    @Mock
    Observer<Object> objectObserver;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    Func1<String, Integer> lengthFunc = new Func1<String, Integer>() {
        @Override
        public Integer call(String t1) {
            return t1.length();
        }
    };
    Func1<String, String> duplicate = new Func1<String, String>() {
        @Override
        public String call(String t1) {
            return t1 + t1;
        }
    };

    @Test
    public void testToMap() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Observable<Map<Integer, String>> mapped = source.toMap(lengthFunc);

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "a");
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onCompleted();
    }

    @Test
    public void testToMapWithValueSelector() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Observable<Map<Integer, String>> mapped = source.toMap(lengthFunc, duplicate);

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "aa");
        expected.put(2, "bbbb");
        expected.put(3, "cccccc");
        expected.put(4, "dddddddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onCompleted();
    }

    @Test
    public void testToMapWithError() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Func1<String, Integer> lengthFuncErr = new Func1<String, Integer>() {
            @Override
            public Integer call(String t1) {
                if ("bb".equals(t1)) {
                    throw new RuntimeException("Forced Failure");
                }
                return t1.length();
            }
        };
        Observable<Map<Integer, String>> mapped = source.toMap(lengthFuncErr);

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "a");
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onCompleted();
        verify(objectObserver, times(1)).onError(any(Throwable.class));

    }

    @Test
    public void testToMapWithErrorInValueSelector() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Func1<String, String> duplicateErr = new Func1<String, String>() {
            @Override
            public String call(String t1) {
                if ("bb".equals(t1)) {
                    throw new RuntimeException("Forced failure");
                }
                return t1 + t1;
            }
        };

        Observable<Map<Integer, String>> mapped = source.toMap(lengthFunc, duplicateErr);

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "aa");
        expected.put(2, "bbbb");
        expected.put(3, "cccccc");
        expected.put(4, "dddddddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onCompleted();
        verify(objectObserver, times(1)).onError(any(Throwable.class));

    }

    @Test
    public void testToMapWithFactory() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Func0<Map<Integer, String>> mapFactory = new Func0<Map<Integer, String>>() {
            @Override
            public Map<Integer, String> call() {
                return new LinkedHashMap<Integer, String>() {
                    /** */
                    private static final long serialVersionUID = -3296811238780863394L;

                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                        return size() > 3;
                    }
                };
            }
        };

        Func1<String, Integer> lengthFunc = new Func1<String, Integer>() {
            @Override
            public Integer call(String t1) {
                return t1.length();
            }
        };
        Observable<Map<Integer, String>> mapped = source.toMap(lengthFunc, UtilityFunctions.<String>identity(), mapFactory);

        Map<Integer, String> expected = new LinkedHashMap<Integer, String>();
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onCompleted();
    }

    @Test
    public void testToMapWithErrorThrowingFactory() {
        Observable<String> source = Observable.just("a", "bb", "ccc", "dddd");

        Func0<Map<Integer, String>> mapFactory = new Func0<Map<Integer, String>>() {
            @Override
            public Map<Integer, String> call() {
                throw new RuntimeException("Forced failure");
            }
        };

        Func1<String, Integer> lengthFunc = new Func1<String, Integer>() {
            @Override
            public Integer call(String t1) {
                return t1.length();
            }
        };
        Observable<Map<Integer, String>> mapped = source.toMap(lengthFunc, UtilityFunctions.<String>identity(), mapFactory);

        Map<Integer, String> expected = new LinkedHashMap<Integer, String>();
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onCompleted();
        verify(objectObserver, times(1)).onError(any(Throwable.class));
    }

    @Test
    public void testKeySelectorThrows() {
        TestSubscriber<Object> ts = TestSubscriber.create();

        Observable.just(1, 2).toMap(new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer v) {
                throw new TestException();
            }
        }).subscribe(ts);

        ts.assertError(TestException.class);
        ts.assertNoValues();
        ts.assertNotCompleted();
    }

    @Test
    public void testValueSelectorThrows() {
        TestSubscriber<Object> ts = TestSubscriber.create();

        Observable.just(1, 2).toMap(new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer v) {
                return v;
            }
        }, new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer v) {
                throw new TestException();
            }
        }).subscribe(ts);

        ts.assertError(TestException.class);
        ts.assertNoValues();
        ts.assertNotCompleted();
    }

    @Test
    public void testMapFactoryThrows() {
        TestSubscriber<Object> ts = TestSubscriber.create();

        Observable.just(1, 2).toMap(new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer v) {
                return v;
            }
        }, new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer v) {
                return v;
            }
        }, new Func0<Map<Integer, Integer>>() {
            @Override
            public Map<Integer, Integer> call() {
                throw new TestException();
            }
        }).subscribe(ts);

        ts.assertError(TestException.class);
        ts.assertNoValues();
        ts.assertNotCompleted();
    }

    @Test
    public void testFactoryFailureDoesNotAllowErrorAndCompletedEmissions() {
        TestSubscriber<Map<Integer, Integer>> ts = TestSubscriber.create(0);
        final RuntimeException e = new RuntimeException();
        Observable.unsafeCreate(new OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> sub) {
                sub.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        if (n > 1) {
                            sub.onNext(1);
                            sub.onCompleted();
                        }
                    }
                });
            }
        }).toMap(new Func1<Integer,Integer>() {

            @Override
            public Integer call(Integer t) {
                throw e;
            }
        }).unsafeSubscribe(ts);
        ts.assertNoValues();
        ts.assertError(e);
        ts.assertNotCompleted();
    }

    @Test
    public void testFactoryFailureDoesNotAllowTwoErrorEmissions() {
        try {
            final List<Throwable> list = new CopyOnWriteArrayList<Throwable>();
            RxJavaHooks.setOnError(new Action1<Throwable>() {

                @Override
                public void call(Throwable t) {
                    list.add(t);
                }
            });
            TestSubscriber<Map<Integer, Integer>> ts = TestSubscriber.create(0);
            final RuntimeException e1 = new RuntimeException();
            final RuntimeException e2 = new RuntimeException();
            Observable.unsafeCreate(new OnSubscribe<Integer>() {

                @Override
                public void call(final Subscriber<? super Integer> sub) {
                    sub.setProducer(new Producer() {

                        @Override
                        public void request(long n) {
                            if (n > 1) {
                                sub.onNext(1);
                                sub.onError(e2);
                            }
                        }
                    });
                }
            }).toMap(new Func1<Integer, Integer>() {

                @Override
                public Integer call(Integer t) {
                    throw e1;
                }
            }).unsafeSubscribe(ts);
            ts.assertNoValues();
            assertEquals(Arrays.asList(e1), ts.getOnErrorEvents());
            assertEquals(Arrays.asList(e2), list);
            ts.assertNotCompleted();
        } finally {
            RxJavaHooks.reset();
        }
    }

    @Test
    public void testFactoryFailureDoesNotAllowErrorThenOnNextEmissions() {
        TestSubscriber<Map<Integer, Integer>> ts = TestSubscriber.create(0);
        final RuntimeException e = new RuntimeException();
        Observable.unsafeCreate(new OnSubscribe<Integer>() {

            @Override
            public void call(final Subscriber<? super Integer> sub) {
                sub.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        if (n > 1) {
                            sub.onNext(1);
                            sub.onNext(2);
                        }
                    }
                });
            }
        }).toMap(new Func1<Integer,Integer>() {

            @Override
            public Integer call(Integer t) {
                throw e;
            }
        }).unsafeSubscribe(ts);
        ts.assertNoValues();
        ts.assertError(e);
        ts.assertNotCompleted();
    }

    @Test
    public void testBackpressure() {
        TestSubscriber<Object> ts = TestSubscriber.create(0);
        Observable
            .just("a", "bb", "ccc", "dddd")
            .toMap(lengthFunc)
            .subscribe(ts);
        ts.assertNoErrors();
        ts.assertNotCompleted();
        ts.assertNoValues();
        ts.requestMore(1);
        ts.assertValueCount(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }
}
