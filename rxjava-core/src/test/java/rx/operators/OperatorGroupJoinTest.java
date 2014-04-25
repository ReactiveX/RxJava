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
package rx.operators;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;

public class OperatorGroupJoinTest {
    @Mock
    Observer<Object> observer;

    Func2<Integer, Integer, Integer> add = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer t1, Integer t2) {
            return t1 + t2;
        }
    };

    <T> Func1<Integer, Observable<T>> just(final Observable<T> observable) {
        return new Func1<Integer, Observable<T>>() {
            @Override
            public Observable<T> call(Integer t1) {
                return observable;
            }
        };
    }

    <T, R> Func1<T, Observable<R>> just2(final Observable<R> observable) {
        return new Func1<T, Observable<R>>() {
            @Override
            public Observable<R> call(T t1) {
                return observable;
            }
        };
    }

    Func2<Integer, Observable<Integer>, Observable<Integer>> add2 = new Func2<Integer, Observable<Integer>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(final Integer leftValue, Observable<Integer> rightValues) {
            return rightValues.map(new Func1<Integer, Integer>() {
                @Override
                public Integer call(Integer rightValue) {
                    return add.call(leftValue, rightValue);
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

        source1.onCompleted();
        source2.onCompleted();

        verify(observer, times(1)).onNext(17);
        verify(observer, times(1)).onNext(18);
        verify(observer, times(1)).onNext(20);
        verify(observer, times(1)).onNext(33);
        verify(observer, times(1)).onNext(34);
        verify(observer, times(1)).onNext(36);
        verify(observer, times(1)).onNext(65);
        verify(observer, times(1)).onNext(66);
        verify(observer, times(1)).onNext(68);

        verify(observer, times(1)).onCompleted(); //Never emitted?
        verify(observer, never()).onError(any(Throwable.class));
    }

    class Person {
        final int id;
        final String name;

        public Person(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    class PersonFruit {
        final int personId;
        final String fruit;

        public PersonFruit(int personId, String fruit) {
            this.personId = personId;
            this.fruit = fruit;
        }
    }

    class PPF {
        final Person person;
        final Observable<PersonFruit> fruits;

        public PPF(Person person, Observable<PersonFruit> fruits) {
            this.person = person;
            this.fruits = fruits;
        }
    }

    @Test
    public void normal1() {
        Observable<Person> source1 = Observable.from(Arrays.asList(
                new Person(1, "Joe"),
                new Person(2, "Mike"),
                new Person(3, "Charlie")
                ));

        Observable<PersonFruit> source2 = Observable.from(Arrays.asList(
                new PersonFruit(1, "Strawberry"),
                new PersonFruit(1, "Apple"),
                new PersonFruit(3, "Peach")
                ));

        Observable<PPF> q = source1.groupJoin(
                source2,
                just2(Observable.<Object> never()),
                just2(Observable.<Object> never()),
                new Func2<Person, Observable<PersonFruit>, PPF>() {
                    @Override
                    public PPF call(Person t1, Observable<PersonFruit> t2) {
                        return new PPF(t1, t2);
                    }
                });

        q.subscribe(
                new Subscriber<PPF>() {
                    @Override
                    public void onNext(final PPF ppf) {
                        ppf.fruits.filter(new Func1<PersonFruit, Boolean>() {
                            @Override
                            public Boolean call(PersonFruit t1) {
                                return ppf.person.id == t1.personId;
                            }
                        }).subscribe(new Action1<PersonFruit>() {
                            @Override
                            public void call(PersonFruit t1) {
                                observer.onNext(Arrays.asList(ppf.person.name, t1.fruit));
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        observer.onError(e);
                    }

                    @Override
                    public void onCompleted() {
                        observer.onCompleted();
                    }

                }
                );

        verify(observer, times(1)).onNext(Arrays.asList("Joe", "Strawberry"));
        verify(observer, times(1)).onNext(Arrays.asList("Joe", "Apple"));
        verify(observer, times(1)).onNext(Arrays.asList("Charlie", "Peach"));

        verify(observer, times(1)).onCompleted();
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
        verify(observer, never()).onCompleted();
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
        verify(observer, never()).onCompleted();
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
        verify(observer, never()).onCompleted();
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
        verify(observer, never()).onCompleted();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void leftDurationSelectorThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Func1<Integer, Observable<Integer>> fail = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };

        Observable<Observable<Integer>> m = source1.groupJoin(source2,
                fail,
                just(Observable.never()), add2);
        m.subscribe(observer);

        source1.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void rightDurationSelectorThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Func1<Integer, Observable<Integer>> fail = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };

        Observable<Observable<Integer>> m = source1.groupJoin(source2,
                just(Observable.never()),
                fail, add2);
        m.subscribe(observer);

        source2.onNext(1);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        verify(observer, never()).onNext(any());
    }

    @Test
    public void resultSelectorThrows() {
        PublishSubject<Integer> source1 = PublishSubject.create();
        PublishSubject<Integer> source2 = PublishSubject.create();

        Func2<Integer, Observable<Integer>, Integer> fail = new Func2<Integer, Observable<Integer>, Integer>() {
            @Override
            public Integer call(Integer t1, Observable<Integer> t2) {
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
        verify(observer, never()).onCompleted();
        verify(observer, never()).onNext(any());
    }
}