/**
 * Copyright (c) 2016-present, RxJava Contributors.
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
package io.reactivex.internal.operators.observable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.*;
import org.mockito.MockitoAnnotations;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class ObservableGroupJoinTest {

    Observer<Object> observer = TestHelper.mockObserver();

    BiFunction<Integer, Integer, Integer> add = new BiFunction<Integer, Integer, Integer>() {
        @Override
        public Integer apply(Integer t1, Integer t2) {
            return t1 + t2;
        }
    };

    <T> Function<Integer, Observable<T>> just(final Observable<T> observable) {
        return new Function<Integer, Observable<T>>() {
            @Override
            public Observable<T> apply(Integer t1) {
                return observable;
            }
        };
    }

    <T, R> Function<T, Observable<R>> just2(final Observable<R> observable) {
        return new Function<T, Observable<R>>() {
            @Override
            public Observable<R> apply(T t1) {
                return observable;
            }
        };
    }

    BiFunction<Integer, Observable<Integer>, Observable<Integer>> add2 = new BiFunction<Integer, Observable<Integer>, Observable<Integer>>() {
        @Override
        public Observable<Integer> apply(final Integer leftValue, Observable<Integer> rightValues) {
            return rightValues.map(new Function<Integer, Integer>() {
                @Override
                public Integer apply(Integer rightValue) throws Exception {
                    return add.apply(leftValue, rightValue);
                }
            });
        }

    };

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void behaveAsJoin() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Observable<Integer> m = Observable.merge(source1.groupJoin(source2,
                just(Observable.never()),
                just(Observable.never()), add2));

        m.subscribe(observer);

        source1.onNext(1);
        source1.onNext(2);
        source1.onNext(4);

        source2.onNext(16);
        source2.onNext(32);
        source2.onNext(64);

        source1.onComplete();
        source2.onComplete();

        verify(observer, times(1)).onNext(17);
        verify(observer, times(1)).onNext(18);
        verify(observer, times(1)).onNext(20);
        verify(observer, times(1)).onNext(33);
        verify(observer, times(1)).onNext(34);
        verify(observer, times(1)).onNext(36);
        verify(observer, times(1)).onNext(65);
        verify(observer, times(1)).onNext(66);
        verify(observer, times(1)).onNext(68);

        verify(observer, times(1)).onComplete(); //Never emitted?
        verify(observer, never()).onError(any(Throwable.class));
    }

    class Person {
        final int id;
        final String name;

        Person(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    class PersonFruit {
        final int personId;
        final String fruit;

        PersonFruit(int personId, String fruit) {
            this.personId = personId;
            this.fruit = fruit;
        }
    }

    class PPF {
        final Person person;
        final Observable<PersonFruit> fruits;

        PPF(Person person, Observable<PersonFruit> fruits) {
            this.person = person;
            this.fruits = fruits;
        }
    }

    @Test
    public void normal1() {
        Observable<Person> source1 = Observable.fromIterable(Arrays.asList(
                new Person(1, "Joe"),
                new Person(2, "Mike"),
                new Person(3, "Charlie")
                ));

        Observable<PersonFruit> source2 = Observable.fromIterable(Arrays.asList(
                new PersonFruit(1, "Strawberry"),
                new PersonFruit(1, "Apple"),
                new PersonFruit(3, "Peach")
                ));

        Observable<PPF> q = source1.groupJoin(
                source2,
                just2(Observable.<Object> never()),
                just2(Observable.<Object> never()),
                new BiFunction<Person, Observable<PersonFruit>, PPF>() {
                    @Override
                    public PPF apply(Person t1, Observable<PersonFruit> t2) {
                        return new PPF(t1, t2);
                    }
                });

        q.subscribe(
                new Observer<PPF>() {
                    @Override
                    public void onNext(final PPF ppf) {
                        ppf.fruits.filter(new Predicate<PersonFruit>() {
                            @Override
                            public boolean test(PersonFruit t1) {
                                return ppf.person.id == t1.personId;
                            }
                        }).subscribe(new Consumer<PersonFruit>() {
                            @Override
                            public void accept(PersonFruit t1) {
                                observer.onNext(Arrays.asList(ppf.person.name, t1.fruit));
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        observer.onError(e);
                    }

                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }

                    @Override
                    public void onSubscribe(Disposable s) {
                    }

                }
                );

        verify(observer, times(1)).onNext(Arrays.asList("Joe", "Strawberry"));
        verify(observer, times(1)).onNext(Arrays.asList("Joe", "Apple"));
        verify(observer, times(1)).onNext(Arrays.asList("Charlie", "Peach"));

        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void leftThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Observable<Observable<Integer>> m = source1.groupJoin(source2,
                just(Observable.never()),
                just(Observable.never()), add2);

        m.subscribe(observer);

        source2.onNext(1);
        source1.onError(new RuntimeException("Forced failure"));

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void rightThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Observable<Observable<Integer>> m = source1.groupJoin(source2,
                just(Observable.never()),
                just(Observable.never()), add2);

        m.subscribe(observer);

        source1.onNext(1);
        source2.onError(new RuntimeException("Forced failure"));

        verify(observer, times(1)).onNext(any(Observable.class));
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
    }

    @Test
    public void leftDurationThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Observable<Integer> duration1 = Observable.<Integer> error(new RuntimeException("Forced failure"));

        Observable<Observable<Integer>> m = source1.groupJoin(source2,
                just(duration1),
                just(Observable.never()), add2);
        m.subscribe(observer);

        source1.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void rightDurationThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Observable<Integer> duration1 = Observable.<Integer> error(new RuntimeException("Forced failure"));

        Observable<Observable<Integer>> m = source1.groupJoin(source2,
                just(Observable.never()),
                just(duration1), add2);
        m.subscribe(observer);

        source2.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void leftDurationSelectorThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Function<Integer, Observable<Integer>> fail = new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };

        Observable<Observable<Integer>> m = source1.groupJoin(source2,
                fail,
                just(Observable.never()), add2);
        m.subscribe(observer);

        source1.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void rightDurationSelectorThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Function<Integer, Observable<Integer>> fail = new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };

        Observable<Observable<Integer>> m = source1.groupJoin(source2,
                just(Observable.never()),
                fail, add2);
        m.subscribe(observer);

        source2.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void resultSelectorThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        BiFunction<Integer, Observable<Integer>, Integer> fail = new BiFunction<Integer, Observable<Integer>, Integer>() {
            @Override
            public Integer apply(Integer t1, Observable<Integer> t2) {
                throw new RuntimeException("Forced failure");
            }
        };

        Observable<Integer> m = source1.groupJoin(source2,
                just(Observable.never()),
                just(Observable.never()), fail);
        m.subscribe(observer);

        source1.onNext(1);
        source2.onNext(2);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onComplete();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).groupJoin(
            Observable.just(2),
            new Function<Integer, ObservableSource<Object>>() {
                @Override
                public ObservableSource<Object> apply(Integer left) throws Exception {
                    return Observable.never();
                }
            },
            new Function<Integer, ObservableSource<Object>>() {
                @Override
                public ObservableSource<Object> apply(Integer right) throws Exception {
                    return Observable.never();
                }
            },
            new BiFunction<Integer, Observable<Integer>, Object>() {
                @Override
                public Object apply(Integer r, Observable<Integer> l) throws Exception {
                    return l;
                }
            }
        ));
    }

    @Test
    public void innerCompleteLeft() {
        Observable.just(1)
        .groupJoin(
            Observable.just(2),
            new Function<Integer, ObservableSource<Object>>() {
                @Override
                public ObservableSource<Object> apply(Integer left) throws Exception {
                    return Observable.empty();
                }
            },
            new Function<Integer, ObservableSource<Object>>() {
                @Override
                public ObservableSource<Object> apply(Integer right) throws Exception {
                    return Observable.never();
                }
            },
            new BiFunction<Integer, Observable<Integer>, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer r, Observable<Integer> l) throws Exception {
                    return l;
                }
            }
        )
        .flatMap(Functions.<Observable<Integer>>identity())
        .test()
        .assertResult();
    }

    @Test
    public void innerErrorLeft() {
        Observable.just(1)
        .groupJoin(
            Observable.just(2),
            new Function<Integer, ObservableSource<Object>>() {
                @Override
                public ObservableSource<Object> apply(Integer left) throws Exception {
                    return Observable.error(new TestException());
                }
            },
            new Function<Integer, ObservableSource<Object>>() {
                @Override
                public ObservableSource<Object> apply(Integer right) throws Exception {
                    return Observable.never();
                }
            },
            new BiFunction<Integer, Observable<Integer>, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer r, Observable<Integer> l) throws Exception {
                    return l;
                }
            }
        )
        .flatMap(Functions.<Observable<Integer>>identity())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerCompleteRight() {
        Observable.just(1)
        .groupJoin(
            Observable.just(2),
            new Function<Integer, ObservableSource<Object>>() {
                @Override
                public ObservableSource<Object> apply(Integer left) throws Exception {
                    return Observable.never();
                }
            },
            new Function<Integer, ObservableSource<Object>>() {
                @Override
                public ObservableSource<Object> apply(Integer right) throws Exception {
                    return Observable.empty();
                }
            },
            new BiFunction<Integer, Observable<Integer>, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer r, Observable<Integer> l) throws Exception {
                    return l;
                }
            }
        )
        .flatMap(Functions.<Observable<Integer>>identity())
        .test()
        .assertResult(2);
    }

    @Test
    public void innerErrorRight() {
        Observable.just(1)
        .groupJoin(
            Observable.just(2),
            new Function<Integer, ObservableSource<Object>>() {
                @Override
                public ObservableSource<Object> apply(Integer left) throws Exception {
                    return Observable.never();
                }
            },
            new Function<Integer, ObservableSource<Object>>() {
                @Override
                public ObservableSource<Object> apply(Integer right) throws Exception {
                    return Observable.error(new TestException());
                }
            },
            new BiFunction<Integer, Observable<Integer>, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer r, Observable<Integer> l) throws Exception {
                    return l;
                }
            }
        )
        .flatMap(Functions.<Observable<Integer>>identity())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerErrorRace() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Object> ps1 = PublishSubject.create();
            final PublishSubject<Object> ps2 = PublishSubject.create();

            List<Throwable> errors = TestHelper.trackPluginErrors();

            try {
                TestObserver<Observable<Integer>> to = Observable.just(1)
                .groupJoin(
                    Observable.just(2).concatWith(Observable.<Integer>never()),
                    new Function<Integer, ObservableSource<Object>>() {
                        @Override
                        public ObservableSource<Object> apply(Integer left) throws Exception {
                            return ps1;
                        }
                    },
                    new Function<Integer, ObservableSource<Object>>() {
                        @Override
                        public ObservableSource<Object> apply(Integer right) throws Exception {
                            return ps2;
                        }
                    },
                    new BiFunction<Integer, Observable<Integer>, Observable<Integer>>() {
                        @Override
                        public Observable<Integer> apply(Integer r, Observable<Integer> l) throws Exception {
                            return l;
                        }
                    }
                )
                .test();

                final TestException ex1 = new TestException();
                final TestException ex2 = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onError(ex1);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex2);
                    }
                };

                TestHelper.race(r1, r2, Schedulers.single());

                to.assertError(Throwable.class).assertSubscribed().assertNotComplete().assertValueCount(1);

                Throwable exc = to.errors().get(0);

                if (exc instanceof CompositeException) {
                    List<Throwable> es = TestHelper.compositeList(exc);
                    TestHelper.assertError(es, 0, TestException.class);
                    TestHelper.assertError(es, 1, TestException.class);
                } else {
                    to.assertError(TestException.class);
                }

                if (!errors.isEmpty()) {
                    TestHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void outerErrorRace() {
        for (int i = 0; i < 500; i++) {
            final PublishSubject<Object> ps1 = PublishSubject.create();
            final PublishSubject<Object> ps2 = PublishSubject.create();

            List<Throwable> errors = TestHelper.trackPluginErrors();

            try {
                TestObserver<Object> to = ps1
                .groupJoin(
                    ps2,
                    new Function<Object, ObservableSource<Object>>() {
                        @Override
                        public ObservableSource<Object> apply(Object left) throws Exception {
                            return Observable.never();
                        }
                    },
                    new Function<Object, ObservableSource<Object>>() {
                        @Override
                        public ObservableSource<Object> apply(Object right) throws Exception {
                            return Observable.never();
                        }
                    },
                    new BiFunction<Object, Observable<Object>, Observable<Object>>() {
                        @Override
                        public Observable<Object> apply(Object r, Observable<Object> l) throws Exception {
                            return l;
                        }
                    }
                )
                .flatMap(Functions.<Observable<Object>>identity())
                .test();

                final TestException ex1 = new TestException();
                final TestException ex2 = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onError(ex1);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex2);
                    }
                };

                TestHelper.race(r1, r2, Schedulers.single());

                to.assertError(Throwable.class).assertSubscribed().assertNotComplete().assertNoValues();

                Throwable exc = to.errors().get(0);

                if (exc instanceof CompositeException) {
                    List<Throwable> es = TestHelper.compositeList(exc);
                    TestHelper.assertError(es, 0, TestException.class);
                    TestHelper.assertError(es, 1, TestException.class);
                } else {
                    to.assertError(TestException.class);
                }

                if (!errors.isEmpty()) {
                    TestHelper.assertUndeliverable(errors, 0, TestException.class);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void rightEmission() {
        final PublishSubject<Object> ps1 = PublishSubject.create();
        final PublishSubject<Object> ps2 = PublishSubject.create();

        TestObserver<Object> to = ps1
        .groupJoin(
            ps2,
            new Function<Object, ObservableSource<Object>>() {
                @Override
                public ObservableSource<Object> apply(Object left) throws Exception {
                    return Observable.never();
                }
            },
            new Function<Object, ObservableSource<Object>>() {
                @Override
                public ObservableSource<Object> apply(Object right) throws Exception {
                    return Observable.never();
                }
            },
            new BiFunction<Object, Observable<Object>, Observable<Object>>() {
                @Override
                public Observable<Object> apply(Object r, Observable<Object> l) throws Exception {
                    return l;
                }
            }
        )
        .flatMap(Functions.<Observable<Object>>identity())
        .test();

        ps2.onNext(2);

        ps1.onNext(1);
        ps1.onComplete();

        ps2.onComplete();

        to.assertResult(2);
    }
}
