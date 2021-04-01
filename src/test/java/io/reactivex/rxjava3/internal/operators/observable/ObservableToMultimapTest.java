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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableToMultimapTest extends RxJavaTest {
    Observer<Object> objectObserver;
    SingleObserver<Object> singleObserver;

    @Before
    public void before() {
        objectObserver = TestHelper.mockObserver();
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
    public void toMultimapObservable() {
        Observable<String> source = Observable.just("a", "b", "cc", "dd");

        Observable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc).toObservable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("a", "b"));
        expected.put(2, Arrays.asList("cc", "dd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onComplete();
    }

    @Test
    public void toMultimapWithValueSelectorObservable() {
        Observable<String> source = Observable.just("a", "b", "cc", "dd");

        Observable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc, duplicate).toObservable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("aa", "bb"));
        expected.put(2, Arrays.asList("cccc", "dddd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onComplete();
    }

    @Test
    public void toMultimapWithMapFactoryObservable() {
        Observable<String> source = Observable.just("a", "b", "cc", "dd", "eee", "fff");

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

        Observable<Map<Integer, Collection<String>>> mapped = source.toMultimap(
                lengthFunc, identity,
                mapFactory, new Function<Integer, Collection<String>>() {
                    @Override
                    public Collection<String> apply(Integer v) {
                        return new ArrayList<>();
                    }
                }).toObservable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Arrays.asList("eee", "fff"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onComplete();
    }

    @Test
    public void toMultimapWithCollectionFactoryObservable() {
        Observable<String> source = Observable.just("cc", "dd", "eee", "eee");

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

        Observable<Map<Integer, Collection<String>>> mapped = source
                .toMultimap(lengthFunc, identity, mapSupplier, collectionFactory).toObservable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, new HashSet<>(Arrays.asList("eee")));

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onComplete();
    }

    @Test
    public void toMultimapWithErrorObservable() {
        Observable<String> source = Observable.just("a", "b", "cc", "dd");

        Function<String, Integer> lengthFuncErr = new Function<String, Integer>() {
            @Override
            public Integer apply(String t1) {
                if ("b".equals(t1)) {
                    throw new RuntimeException("Forced Failure");
                }
                return t1.length();
            }
        };

        Observable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFuncErr).toObservable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("a", "b"));
        expected.put(2, Arrays.asList("cc", "dd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onComplete();
    }

    @Test
    public void toMultimapWithErrorInValueSelectorObservable() {
        Observable<String> source = Observable.just("a", "b", "cc", "dd");

        Function<String, String> duplicateErr = new Function<String, String>() {
            @Override
            public String apply(String t1) {
                if ("b".equals(t1)) {
                    throw new RuntimeException("Forced failure");
                }
                return t1 + t1;
            }
        };

        Observable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc, duplicateErr).toObservable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("aa", "bb"));
        expected.put(2, Arrays.asList("cccc", "dddd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onComplete();
    }

    @Test
    public void toMultimapWithMapThrowingFactoryObservable() {
        Observable<String> source = Observable.just("a", "b", "cc", "dd", "eee", "fff");

        Supplier<Map<Integer, Collection<String>>> mapFactory = new Supplier<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> get() {
                throw new RuntimeException("Forced failure");
            }
        };

        Observable<Map<Integer, Collection<String>>> mapped = source
                .toMultimap(lengthFunc, new Function<String, String>() {
                    @Override
                    public String apply(String v) {
                        return v;
                    }
                }, mapFactory).toObservable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Arrays.asList("eee", "fff"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onComplete();
    }

    @Test
    public void toMultimapWithThrowingCollectionFactoryObservable() {
        Observable<String> source = Observable.just("cc", "cc", "eee", "eee");

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

        Observable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc,
                identity, mapSupplier, collectionFactory).toObservable();

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Collections.singleton("eee"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onComplete();
    }

    @Test
    public void toMultimap() {
        Observable<String> source = Observable.just("a", "b", "cc", "dd");

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
        Observable<String> source = Observable.just("a", "b", "cc", "dd");

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
        Observable<String> source = Observable.just("a", "b", "cc", "dd", "eee", "fff");

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
                    public Collection<String> apply(Integer v) {
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
        Observable<String> source = Observable.just("cc", "dd", "eee", "eee");

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
        Observable<String> source = Observable.just("a", "b", "cc", "dd");

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
        Observable<String> source = Observable.just("a", "b", "cc", "dd");

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
        Observable<String> source = Observable.just("a", "b", "cc", "dd", "eee", "fff");

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
        Observable<String> source = Observable.just("cc", "cc", "eee", "eee");

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
