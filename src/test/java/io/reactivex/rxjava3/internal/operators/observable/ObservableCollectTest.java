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

import static io.reactivex.rxjava3.internal.util.TestingHelper.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.TestHelper;

public final class ObservableCollectTest extends RxJavaTest {

    @Test
    public void collectToListObservable() {
        Observable<List<Integer>> o = Observable.just(1, 2, 3)
        .collect(new Supplier<List<Integer>>() {
            @Override
            public List<Integer> get() {
                return new ArrayList<>();
            }
        }, new BiConsumer<List<Integer>, Integer>() {
            @Override
            public void accept(List<Integer> list, Integer v) {
                list.add(v);
            }
        }).toObservable();

        List<Integer> list =  o.blockingLast();

        assertEquals(3, list.size());
        assertEquals(1, list.get(0).intValue());
        assertEquals(2, list.get(1).intValue());
        assertEquals(3, list.get(2).intValue());

        // test multiple subscribe
        List<Integer> list2 =  o.blockingLast();

        assertEquals(3, list2.size());
        assertEquals(1, list2.get(0).intValue());
        assertEquals(2, list2.get(1).intValue());
        assertEquals(3, list2.get(2).intValue());
    }

    @Test
    public void collectToStringObservable() {
        String value = Observable.just(1, 2, 3).collect(new Supplier<StringBuilder>() {
            @Override
            public StringBuilder get() {
                return new StringBuilder();
            }
        },
            new BiConsumer<StringBuilder, Integer>() {
                @Override
                public void accept(StringBuilder sb, Integer v) {
                if (sb.length() > 0) {
                    sb.append("-");
                }
                sb.append(v);
      }
            }).toObservable().blockingLast().toString();

        assertEquals("1-2-3", value);
    }

    @Test
    public void collectorFailureDoesNotResultInTwoErrorEmissionsObservable() {
        try {
            final List<Throwable> list = new CopyOnWriteArrayList<>();
            RxJavaPlugins.setErrorHandler(addToList(list));
            final RuntimeException e1 = new RuntimeException();
            final RuntimeException e2 = new RuntimeException();

            Burst.items(1).error(e2) //
                    .collect(supplierListCreator(), biConsumerThrows(e1)) //
                    .toObservable()
                    .test() //
                    .assertError(e1) //
                    .assertNotComplete();

            assertEquals(1, list.size());
            assertEquals(e2, list.get(0).getCause());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void collectorFailureDoesNotResultInErrorAndCompletedEmissionsObservable() {
        final RuntimeException e = new RuntimeException();
        Burst.item(1).create() //
                .collect(supplierListCreator(), biConsumerThrows(e)) //
                .toObservable()
                .test() //
                .assertError(e) //
                .assertNotComplete();
    }

    @Test
    public void collectorFailureDoesNotResultInErrorAndOnNextEmissionsObservable() {
        final RuntimeException e = new RuntimeException();
        final AtomicBoolean added = new AtomicBoolean();
        BiConsumer<Object, Integer> throwOnFirstOnly = new BiConsumer<Object, Integer>() {

            boolean once = true;

            @Override
            public void accept(Object o, Integer t) {
                if (once) {
                    once = false;
                    throw e;
                } else {
                    added.set(true);
                }
            }
        };
        Burst.items(1, 2).create() //
                .collect(supplierListCreator(), throwOnFirstOnly)//
                .test() //
                .assertError(e) //
                .assertNoValues() //
                .assertNotComplete();
        assertFalse(added.get());
    }

    @Test
    public void collectIntoObservable() {
        Observable.just(1, 1, 1, 1, 2)
        .collectInto(new HashSet<>(), new BiConsumer<HashSet<Integer>, Integer>() {
            @Override
            public void accept(HashSet<Integer> s, Integer v) throws Exception {
                s.add(v);
            }
        }).toObservable()
        .test()
        .assertResult(new HashSet<>(Arrays.asList(1, 2)));
    }

    @Test
    public void collectToList() {
        Single<List<Integer>> o = Observable.just(1, 2, 3)
        .collect(new Supplier<List<Integer>>() {
            @Override
            public List<Integer> get() {
                return new ArrayList<>();
            }
        }, new BiConsumer<List<Integer>, Integer>() {
            @Override
            public void accept(List<Integer> list, Integer v) {
                list.add(v);
            }
        });

        List<Integer> list =  o.blockingGet();

        assertEquals(3, list.size());
        assertEquals(1, list.get(0).intValue());
        assertEquals(2, list.get(1).intValue());
        assertEquals(3, list.get(2).intValue());

        // test multiple subscribe
        List<Integer> list2 =  o.blockingGet();

        assertEquals(3, list2.size());
        assertEquals(1, list2.get(0).intValue());
        assertEquals(2, list2.get(1).intValue());
        assertEquals(3, list2.get(2).intValue());
    }

    @Test
    public void collectToString() {
        String value = Observable.just(1, 2, 3).collect(new Supplier<StringBuilder>() {
            @Override
            public StringBuilder get() {
                return new StringBuilder();
            }
        },
            new BiConsumer<StringBuilder, Integer>() {
                @Override
                public void accept(StringBuilder sb, Integer v) {
                if (sb.length() > 0) {
                    sb.append("-");
                }
                sb.append(v);
      }
            }).blockingGet().toString();

        assertEquals("1-2-3", value);
    }

    @Test
    public void collectorFailureDoesNotResultInTwoErrorEmissions() {
        try {
            final List<Throwable> list = new CopyOnWriteArrayList<>();
            RxJavaPlugins.setErrorHandler(addToList(list));
            final RuntimeException e1 = new RuntimeException();
            final RuntimeException e2 = new RuntimeException();

            Burst.items(1).error(e2) //
                    .collect(supplierListCreator(), biConsumerThrows(e1)) //
                    .test() //
                    .assertError(e1) //
                    .assertNotComplete();

            assertEquals(1, list.size());
            assertEquals(e2, list.get(0).getCause());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void collectorFailureDoesNotResultInErrorAndCompletedEmissions() {
        final RuntimeException e = new RuntimeException();
        Burst.item(1).create() //
                .collect(supplierListCreator(), biConsumerThrows(e)) //
                .test() //
                .assertError(e) //
                .assertNotComplete();
    }

    @Test
    public void collectorFailureDoesNotResultInErrorAndOnNextEmissions() {
        final RuntimeException e = new RuntimeException();
        final AtomicBoolean added = new AtomicBoolean();
        BiConsumer<Object, Integer> throwOnFirstOnly = new BiConsumer<Object, Integer>() {

            boolean once = true;

            @Override
            public void accept(Object o, Integer t) {
                if (once) {
                    once = false;
                    throw e;
                } else {
                    added.set(true);
                }
            }
        };
        Burst.items(1, 2).create() //
                .collect(supplierListCreator(), throwOnFirstOnly)//
                .test() //
                .assertError(e) //
                .assertNoValues() //
                .assertNotComplete();
        assertFalse(added.get());
    }

    @Test
    public void collectInto() {
        Observable.just(1, 1, 1, 1, 2)
        .collectInto(new HashSet<>(), new BiConsumer<HashSet<Integer>, Integer>() {
            @Override
            public void accept(HashSet<Integer> s, Integer v) throws Exception {
                s.add(v);
            }
        })
        .test()
        .assertResult(new HashSet<>(Arrays.asList(1, 2)));
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.range(1, 3).collect(new Supplier<List<Integer>>() {
            @Override
            public List<Integer> get() throws Exception {
                return new ArrayList<>();
            }
        }, new BiConsumer<List<Integer>, Integer>() {
            @Override
            public void accept(List<Integer> a, Integer b) throws Exception {
                a.add(b);
            }
        }));

        TestHelper.checkDisposed(Observable.range(1, 3).collect(new Supplier<List<Integer>>() {
            @Override
            public List<Integer> get() throws Exception {
                return new ArrayList<>();
            }
        }, new BiConsumer<List<Integer>, Integer>() {
            @Override
            public void accept(List<Integer> a, Integer b) throws Exception {
                a.add(b);
            }
        }).toObservable());
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservableToSingle(new Function<Observable<Integer>, SingleSource<List<Integer>>>() {
            @Override
            public SingleSource<List<Integer>> apply(Observable<Integer> o) throws Exception {
                return o.collect(new Supplier<List<Integer>>() {
                    @Override
                    public List<Integer> get() throws Exception {
                        return new ArrayList<>();
                    }
                }, new BiConsumer<List<Integer>, Integer>() {
                    @Override
                    public void accept(List<Integer> a, Integer b) throws Exception {
                        a.add(b);
                    }
                });
            }
        });

        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Integer>, ObservableSource<List<Integer>>>() {
            @Override
            public ObservableSource<List<Integer>> apply(Observable<Integer> o) throws Exception {
                return o.collect(new Supplier<List<Integer>>() {
                    @Override
                    public List<Integer> get() throws Exception {
                        return new ArrayList<>();
                    }
                }, new BiConsumer<List<Integer>, Integer>() {
                    @Override
                    public void accept(List<Integer> a, Integer b) throws Exception {
                        a.add(b);
                    }
                }).toObservable();
            }
        });
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceObservable(new Function<Observable<Integer>, Object>() {
            @Override
            public Object apply(Observable<Integer> o) throws Exception {
                return o.collect(new Supplier<List<Integer>>() {
                    @Override
                    public List<Integer> get() throws Exception {
                        return new ArrayList<>();
                    }
                }, new BiConsumer<List<Integer>, Integer>() {
                    @Override
                    public void accept(List<Integer> a, Integer b) throws Exception {
                        a.add(b);
                    }
                }).toObservable();
            }
        }, false, 1, 2, Arrays.asList(1));
    }
}
