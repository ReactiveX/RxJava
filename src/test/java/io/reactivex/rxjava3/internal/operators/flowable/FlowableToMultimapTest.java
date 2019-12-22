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

public class FlowableToMultimapTest extends RxJavaTest {
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
    public void toMultimapFlowable() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd");

        Flowable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc).toFlowable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("a", "b"));
        expected.put(2, Arrays.asList("cc", "dd"));

        mapped.subscribe(objectSubscriber);

        verify(objectSubscriber, never()).onError(any(Throwable.class));
        verify(objectSubscriber, times(1)).onNext(expected);
        verify(objectSubscriber, times(1)).onComplete();
    }

    @Test
    public void toMultimapWithValueSelectorFlowable() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd");

        Flowable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc, duplicate).toFlowable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("aa", "bb"));
        expected.put(2, Arrays.asList("cccc", "dddd"));

        mapped.subscribe(objectSubscriber);

        verify(objectSubscriber, never()).onError(any(Throwable.class));
        verify(objectSubscriber, times(1)).onNext(expected);
        verify(objectSubscriber, times(1)).onComplete();
    }

    @Test
    public void toMultimapWithMapFactoryFlowable() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd", "eee", "fff");

        Supplier<Map<Integer, Collection<String>>> mapFactory = new Supplier<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> get() {
                return new LinkedHashMap<Integer, Collection<String>>() {

                    private static final long serialVersionUID = -2084477070717362859L;

                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Integer, Collection<String>> eldest) {
                        return size() > 2;
                    }
                };
            }
        };

        Function<String, String> identity = new Function<String, String>() {
            @Override
            public String apply(String v) {
                return v;
            }
        };

        Flowable<Map<Integer, Collection<String>>> mapped = source.toMultimap(
                lengthFunc, identity,
                mapFactory, new Function<Integer, Collection<String>>() {
                    @Override
                    public Collection<String> apply(Integer e) {
                        return new ArrayList<>();
                    }
                }).toFlowable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Arrays.asList("eee", "fff"));

        mapped.subscribe(objectSubscriber);

        verify(objectSubscriber, never()).onError(any(Throwable.class));
        verify(objectSubscriber, times(1)).onNext(expected);
        verify(objectSubscriber, times(1)).onComplete();
    }

    @Test
    public void toMultimapWithCollectionFactoryFlowable() {
        Flowable<String> source = Flowable.just("cc", "dd", "eee", "eee");

        Function<Integer, Collection<String>> collectionFactory = new Function<Integer, Collection<String>>() {
            @Override
            public Collection<String> apply(Integer t1) {
                if (t1 == 2) {
                    return new ArrayList<>();
                } else {
                    return new HashSet<>();
                }
            }
        };

        Function<String, String> identity = new Function<String, String>() {
            @Override
            public String apply(String v) {
                return v;
            }
        };
        Supplier<Map<Integer, Collection<String>>> mapSupplier = new Supplier<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> get() {
                return new HashMap<>();
            }
        };

        Flowable<Map<Integer, Collection<String>>> mapped = source
                .toMultimap(lengthFunc, identity, mapSupplier, collectionFactory).toFlowable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, new HashSet<>(Arrays.asList("eee")));

        mapped.subscribe(objectSubscriber);

        verify(objectSubscriber, never()).onError(any(Throwable.class));
        verify(objectSubscriber, times(1)).onNext(expected);
        verify(objectSubscriber, times(1)).onComplete();
    }

    @Test
    public void toMultimapWithErrorFlowable() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd");

        Function<String, Integer> lengthFuncErr = new Function<String, Integer>() {
            @Override
            public Integer apply(String t1) {
                if ("b".equals(t1)) {
                    throw new RuntimeException("Forced Failure");
                }
                return t1.length();
            }
        };

        Flowable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFuncErr).toFlowable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("a", "b"));
        expected.put(2, Arrays.asList("cc", "dd"));

        mapped.subscribe(objectSubscriber);

        verify(objectSubscriber, times(1)).onError(any(Throwable.class));
        verify(objectSubscriber, never()).onNext(expected);
        verify(objectSubscriber, never()).onComplete();
    }

    @Test
    public void toMultimapWithErrorInValueSelectorFlowable() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd");

        Function<String, String> duplicateErr = new Function<String, String>() {
            @Override
            public String apply(String t1) {
                if ("b".equals(t1)) {
                    throw new RuntimeException("Forced failure");
                }
                return t1 + t1;
            }
        };

        Flowable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc, duplicateErr).toFlowable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("aa", "bb"));
        expected.put(2, Arrays.asList("cccc", "dddd"));

        mapped.subscribe(objectSubscriber);

        verify(objectSubscriber, times(1)).onError(any(Throwable.class));
        verify(objectSubscriber, never()).onNext(expected);
        verify(objectSubscriber, never()).onComplete();
    }

    @Test
    public void toMultimapWithMapThrowingFactoryFlowable() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd", "eee", "fff");

        Supplier<Map<Integer, Collection<String>>> mapFactory = new Supplier<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> get() {
                throw new RuntimeException("Forced failure");
            }
        };

        Flowable<Map<Integer, Collection<String>>> mapped = source
                .toMultimap(lengthFunc, new Function<String, String>() {
                    @Override
                    public String apply(String v) {
                        return v;
                    }
                }, mapFactory).toFlowable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Arrays.asList("eee", "fff"));

        mapped.subscribe(objectSubscriber);

        verify(objectSubscriber, times(1)).onError(any(Throwable.class));
        verify(objectSubscriber, never()).onNext(expected);
        verify(objectSubscriber, never()).onComplete();
    }

    @Test
    public void toMultimapWithThrowingCollectionFactoryFlowable() {
        Flowable<String> source = Flowable.just("cc", "cc", "eee", "eee");

        Function<Integer, Collection<String>> collectionFactory = new Function<Integer, Collection<String>>() {
            @Override
            public Collection<String> apply(Integer t1) {
                if (t1 == 2) {
                    throw new RuntimeException("Forced failure");
                } else {
                    return new HashSet<>();
                }
            }
        };

        Function<String, String> identity = new Function<String, String>() {
            @Override
            public String apply(String v) {
                return v;
            }
        };
        Supplier<Map<Integer, Collection<String>>> mapSupplier = new Supplier<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> get() {
                return new HashMap<>();
            }
        };

        Flowable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc,
                identity, mapSupplier, collectionFactory).toFlowable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Collections.singleton("eee"));

        mapped.subscribe(objectSubscriber);

        verify(objectSubscriber, times(1)).onError(any(Throwable.class));
        verify(objectSubscriber, never()).onNext(expected);
        verify(objectSubscriber, never()).onComplete();
    }

    @Test
    public void toMultimap() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd");

        Single<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc);

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("a", "b"));
        expected.put(2, Arrays.asList("cc", "dd"));

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onError(any(Throwable.class));
        verify(singleObserver, times(1)).onSuccess(expected);
    }

    @Test
    public void toMultimapWithValueSelector() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd");

        Single<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc, duplicate);

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("aa", "bb"));
        expected.put(2, Arrays.asList("cccc", "dddd"));

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onError(any(Throwable.class));
        verify(singleObserver, times(1)).onSuccess(expected);
    }

    @Test
    public void toMultimapWithMapFactory() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd", "eee", "fff");

        Supplier<Map<Integer, Collection<String>>> mapFactory = new Supplier<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> get() {
                return new LinkedHashMap<Integer, Collection<String>>() {

                    private static final long serialVersionUID = -2084477070717362859L;

                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Integer, Collection<String>> eldest) {
                        return size() > 2;
                    }
                };
            }
        };

        Function<String, String> identity = new Function<String, String>() {
            @Override
            public String apply(String v) {
                return v;
            }
        };

        Single<Map<Integer, Collection<String>>> mapped = source.toMultimap(
                lengthFunc, identity,
                mapFactory, new Function<Integer, Collection<String>>() {
                    @Override
                    public Collection<String> apply(Integer e) {
                        return new ArrayList<>();
                    }
                });

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Arrays.asList("eee", "fff"));

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onError(any(Throwable.class));
        verify(singleObserver, times(1)).onSuccess(expected);
    }

    @Test
    public void toMultimapWithCollectionFactory() {
        Flowable<String> source = Flowable.just("cc", "dd", "eee", "eee");

        Function<Integer, Collection<String>> collectionFactory = new Function<Integer, Collection<String>>() {
            @Override
            public Collection<String> apply(Integer t1) {
                if (t1 == 2) {
                    return new ArrayList<>();
                } else {
                    return new HashSet<>();
                }
            }
        };

        Function<String, String> identity = new Function<String, String>() {
            @Override
            public String apply(String v) {
                return v;
            }
        };
        Supplier<Map<Integer, Collection<String>>> mapSupplier = new Supplier<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> get() {
                return new HashMap<>();
            }
        };

        Single<Map<Integer, Collection<String>>> mapped = source
                .toMultimap(lengthFunc, identity, mapSupplier, collectionFactory);

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, new HashSet<>(Arrays.asList("eee")));

        mapped.subscribe(singleObserver);

        verify(singleObserver, never()).onError(any(Throwable.class));
        verify(singleObserver, times(1)).onSuccess(expected);
    }

    @Test
    public void toMultimapWithError() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd");

        Function<String, Integer> lengthFuncErr = new Function<String, Integer>() {
            @Override
            public Integer apply(String t1) {
                if ("b".equals(t1)) {
                    throw new RuntimeException("Forced Failure");
                }
                return t1.length();
            }
        };

        Single<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFuncErr);

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("a", "b"));
        expected.put(2, Arrays.asList("cc", "dd"));

        mapped.subscribe(singleObserver);

        verify(singleObserver, times(1)).onError(any(Throwable.class));
        verify(singleObserver, never()).onSuccess(expected);
    }

    @Test
    public void toMultimapWithErrorInValueSelector() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd");

        Function<String, String> duplicateErr = new Function<String, String>() {
            @Override
            public String apply(String t1) {
                if ("b".equals(t1)) {
                    throw new RuntimeException("Forced failure");
                }
                return t1 + t1;
            }
        };

        Single<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc, duplicateErr);

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("aa", "bb"));
        expected.put(2, Arrays.asList("cccc", "dddd"));

        mapped.subscribe(singleObserver);

        verify(singleObserver, times(1)).onError(any(Throwable.class));
        verify(singleObserver, never()).onSuccess(expected);
    }

    @Test
    public void toMultimapWithMapThrowingFactory() {
        Flowable<String> source = Flowable.just("a", "b", "cc", "dd", "eee", "fff");

        Supplier<Map<Integer, Collection<String>>> mapFactory = new Supplier<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> get() {
                throw new RuntimeException("Forced failure");
            }
        };

        Single<Map<Integer, Collection<String>>> mapped = source
                .toMultimap(lengthFunc, new Function<String, String>() {
                    @Override
                    public String apply(String v) {
                        return v;
                    }
                }, mapFactory);

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Arrays.asList("eee", "fff"));

        mapped.subscribe(singleObserver);

        verify(singleObserver, times(1)).onError(any(Throwable.class));
        verify(singleObserver, never()).onSuccess(expected);
    }

    @Test
    public void toMultimapWithThrowingCollectionFactory() {
        Flowable<String> source = Flowable.just("cc", "cc", "eee", "eee");

        Function<Integer, Collection<String>> collectionFactory = new Function<Integer, Collection<String>>() {
            @Override
            public Collection<String> apply(Integer t1) {
                if (t1 == 2) {
                    throw new RuntimeException("Forced failure");
                } else {
                    return new HashSet<>();
                }
            }
        };

        Function<String, String> identity = new Function<String, String>() {
            @Override
            public String apply(String v) {
                return v;
            }
        };
        Supplier<Map<Integer, Collection<String>>> mapSupplier = new Supplier<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> get() {
                return new HashMap<>();
            }
        };

        Single<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc,
                identity, mapSupplier, collectionFactory);

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Collections.singleton("eee"));

        mapped.subscribe(singleObserver);

        verify(singleObserver, times(1)).onError(any(Throwable.class));
        verify(singleObserver, never()).onSuccess(expected);
    }

}
