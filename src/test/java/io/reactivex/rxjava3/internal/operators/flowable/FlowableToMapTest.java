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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.*;
import org.reactivestreams.Subscriber;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FlowableToMapTest extends RxJavaTest {
    Subscriber<Object> objectSubscriber;
    SingleObserver<Object> singleObserver;

    @Before
    public void before() {
        objectSubscriber = TestHelper.mockSubscriber();
        singleObserver = TestHelper.mockSingleObserver();
    }

    Function<String, Integer> lengthFunc = new Function<String, Integer>() {
        @Override
        public Integer apply(String t1) {
            return t1.length();
        }
    };
    Function<String, String> duplicate = new Function<String, String>() {
        @Override
        public String apply(String t1) {
            return t1 + t1;
        }
    };

    @Test
    public void toMapFlowable() {
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Flowable<Map<Integer, String>> mapped = source.toMap(lengthFunc).toFlowable();

        Map<Integer, String> expected = new HashMap<>();
        expected.put(1, "a");
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectSubscriber);

        verify(objectSubscriber, never()).onError(any(Throwable.class));
        verify(objectSubscriber, times(1)).onNext(expected);
        verify(objectSubscriber, times(1)).onComplete();
    }

    @Test
    public void toMapWithValueSelectorFlowable() {
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Flowable<Map<Integer, String>> mapped = source.toMap(lengthFunc, duplicate).toFlowable();

        Map<Integer, String> expected = new HashMap<>();
        expected.put(1, "aa");
        expected.put(2, "bbbb");
        expected.put(3, "cccccc");
        expected.put(4, "dddddddd");

        mapped.subscribe(objectSubscriber);

        verify(objectSubscriber, never()).onError(any(Throwable.class));
        verify(objectSubscriber, times(1)).onNext(expected);
        verify(objectSubscriber, times(1)).onComplete();
    }

    @Test
    public void toMapWithErrorFlowable() {
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Function<String, Integer> lengthFuncErr = new Function<String, Integer>() {
            @Override
            public Integer apply(String t1) {
                if ("bb".equals(t1)) {
                    throw new RuntimeException("Forced Failure");
                }
                return t1.length();
            }
        };
        Flowable<Map<Integer, String>> mapped = source.toMap(lengthFuncErr).toFlowable();

        Map<Integer, String> expected = new HashMap<>();
        expected.put(1, "a");
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectSubscriber);

        verify(objectSubscriber, never()).onNext(expected);
        verify(objectSubscriber, never()).onComplete();
        verify(objectSubscriber, times(1)).onError(any(Throwable.class));

    }

    @Test
    public void toMapWithErrorInValueSelectorFlowable() {
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Function<String, String> duplicateErr = new Function<String, String>() {
            @Override
            public String apply(String t1) {
                if ("bb".equals(t1)) {
                    throw new RuntimeException("Forced failure");
                }
                return t1 + t1;
            }
        };

        Flowable<Map<Integer, String>> mapped = source.toMap(lengthFunc, duplicateErr).toFlowable();

        Map<Integer, String> expected = new HashMap<>();
        expected.put(1, "aa");
        expected.put(2, "bbbb");
        expected.put(3, "cccccc");
        expected.put(4, "dddddddd");

        mapped.subscribe(objectSubscriber);

        verify(objectSubscriber, never()).onNext(expected);
        verify(objectSubscriber, never()).onComplete();
        verify(objectSubscriber, times(1)).onError(any(Throwable.class));

    }

    @Test
    public void toMapWithFactoryFlowable() {
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Supplier<Map<Integer, String>> mapFactory = new Supplier<Map<Integer, String>>() {
            @Override
            public Map<Integer, String> get() {
                return new LinkedHashMap<Integer, String>() {

                    private static final long serialVersionUID = -3296811238780863394L;

                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                        return size() > 3;
                    }
                };
            }
        };

        Function<String, Integer> lengthFunc = new Function<String, Integer>() {
            @Override
            public Integer apply(String t1) {
                return t1.length();
            }
        };
        Flowable<Map<Integer, String>> mapped = source.toMap(lengthFunc, new Function<String, String>() {
            @Override
            public String apply(String v) {
                return v;
            }
        }, mapFactory).toFlowable();

        Map<Integer, String> expected = new LinkedHashMap<>();
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectSubscriber);

        verify(objectSubscriber, never()).onError(any(Throwable.class));
        verify(objectSubscriber, times(1)).onNext(expected);
        verify(objectSubscriber, times(1)).onComplete();
    }

    @Test
    public void toMapWithErrorThrowingFactoryFlowable() {
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Supplier<Map<Integer, String>> mapFactory = new Supplier<Map<Integer, String>>() {
            @Override
            public Map<Integer, String> get() {
                throw new RuntimeException("Forced failure");
            }
        };

        Function<String, Integer> lengthFunc = new Function<String, Integer>() {
            @Override
            public Integer apply(String t1) {
                return t1.length();
            }
        };
        Flowable<Map<Integer, String>> mapped = source.toMap(lengthFunc, new Function<String, String>() {
            @Override
            public String apply(String v) {
                return v;
            }
        }, mapFactory).toFlowable();

        Map<Integer, String> expected = new LinkedHashMap<>();
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectSubscriber);

        verify(objectSubscriber, never()).onNext(expected);
        verify(objectSubscriber, never()).onComplete();
        verify(objectSubscriber, times(1)).onError(any(Throwable.class));
    }

    @Test
    public void toMap() {
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Single<Map<Integer, String>> mapped = source.toMap(lengthFunc);

        Map<Integer, String> expected = new HashMap<>();
        expected.put(1, "a");
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onError(any(Throwable.class));
        verify(singleObserver, times(1)).onSuccess(expected);
    }

    @Test
    public void toMapWithValueSelector() {
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Single<Map<Integer, String>> mapped = source.toMap(lengthFunc, duplicate);

        Map<Integer, String> expected = new HashMap<>();
        expected.put(1, "aa");
        expected.put(2, "bbbb");
        expected.put(3, "cccccc");
        expected.put(4, "dddddddd");

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onError(any(Throwable.class));
        verify(singleObserver, times(1)).onSuccess(expected);
    }

    @Test
    public void toMapWithError() {
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Function<String, Integer> lengthFuncErr = new Function<String, Integer>() {
            @Override
            public Integer apply(String t1) {
                if ("bb".equals(t1)) {
                    throw new RuntimeException("Forced Failure");
                }
                return t1.length();
            }
        };
        Single<Map<Integer, String>> mapped = source.toMap(lengthFuncErr);

        Map<Integer, String> expected = new HashMap<>();
        expected.put(1, "a");
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onSuccess(expected);
        verify(singleObserver, times(1)).onError(any(Throwable.class));

    }

    @Test
    public void toMapWithErrorInValueSelector() {
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Function<String, String> duplicateErr = new Function<String, String>() {
            @Override
            public String apply(String t1) {
                if ("bb".equals(t1)) {
                    throw new RuntimeException("Forced failure");
                }
                return t1 + t1;
            }
        };

        Single<Map<Integer, String>> mapped = source.toMap(lengthFunc, duplicateErr);

        Map<Integer, String> expected = new HashMap<>();
        expected.put(1, "aa");
        expected.put(2, "bbbb");
        expected.put(3, "cccccc");
        expected.put(4, "dddddddd");

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onSuccess(expected);
        verify(singleObserver, times(1)).onError(any(Throwable.class));

    }

    @Test
    public void toMapWithFactory() {
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Supplier<Map<Integer, String>> mapFactory = new Supplier<Map<Integer, String>>() {
            @Override
            public Map<Integer, String> get() {
                return new LinkedHashMap<Integer, String>() {

                    private static final long serialVersionUID = -3296811238780863394L;

                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                        return size() > 3;
                    }
                };
            }
        };

        Function<String, Integer> lengthFunc = new Function<String, Integer>() {
            @Override
            public Integer apply(String t1) {
                return t1.length();
            }
        };
        Single<Map<Integer, String>> mapped = source.toMap(lengthFunc, new Function<String, String>() {
            @Override
            public String apply(String v) {
                return v;
            }
        }, mapFactory);

        Map<Integer, String> expected = new LinkedHashMap<>();
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onError(any(Throwable.class));
        verify(singleObserver, times(1)).onSuccess(expected);
    }

    @Test
    public void toMapWithErrorThrowingFactory() {
        Flowable<String> source = Flowable.just("a", "bb", "ccc", "dddd");

        Supplier<Map<Integer, String>> mapFactory = new Supplier<Map<Integer, String>>() {
            @Override
            public Map<Integer, String> get() {
                throw new RuntimeException("Forced failure");
            }
        };

        Function<String, Integer> lengthFunc = new Function<String, Integer>() {
            @Override
            public Integer apply(String t1) {
                return t1.length();
            }
        };
        Single<Map<Integer, String>> mapped = source.toMap(lengthFunc, new Function<String, String>() {
            @Override
            public String apply(String v) {
                return v;
            }
        }, mapFactory);

        Map<Integer, String> expected = new LinkedHashMap<>();
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onSuccess(expected);
        verify(singleObserver, times(1)).onError(any(Throwable.class));
    }

}
