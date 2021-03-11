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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;

import io.reactivex.rxjava3.annotations.NonNull;
import org.junit.*;
import org.mockito.MockitoAnnotations;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.operators.observable.ObservableGroupJoin.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableGroupJoinTest extends RxJavaTest {

    final Observer<Object> observer = TestHelper.mockObserver();

    final BiFunction<Integer, Integer, Integer> add = Integer::sum;

    <T> Function<Integer, Observable<T>> just(final Observable<T> observable) {
        return t1 -> observable;
    }

    <T, R> Function<T, Observable<R>> just2(final Observable<R> observable) {
        return t1 -> observable;
    }

    final BiFunction<Integer, Observable<Integer>, Observable<Integer>> add2 = (leftValue, rightValues) -> rightValues.map(rightValue -> add.apply(leftValue, rightValue));

    @Before
    public void before() {
        MockitoAnnotations.openMocks(this);
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

    static class Person {
        final int id;
        final String name;

        Person(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static class PersonFruit {
        final int personId;
        final String fruit;

        PersonFruit(int personId, String fruit) {
            this.personId = personId;
            this.fruit = fruit;
        }
    }

    static class PPF {
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
                just2(Observable.never()),
                just2(Observable.never()),
                PPF::new);

        q.subscribe(
                new Observer<PPF>() {
                    @Override
                    public void onNext(final @NonNull PPF ppf) {
                        ppf.fruits.filter(t1 -> ppf.person.id == t1.personId).subscribe(t1 -> observer.onNext(Arrays.asList(ppf.person.name, t1.fruit)));
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        observer.onError(e);
                    }

                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }

                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
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

        Observable<Integer> duration1 = Observable.error(new RuntimeException("Forced failure"));

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

        Observable<Integer> duration1 = Observable.error(new RuntimeException("Forced failure"));

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

        Function<Integer, Observable<Integer>> fail = t1 -> {
            throw new RuntimeException("Forced failure");
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

        Function<Integer, Observable<Integer>> fail = t1 -> {
            throw new RuntimeException("Forced failure");
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

        BiFunction<Integer, Observable<Integer>, Integer> fail = (t1, t2) -> {
            throw new RuntimeException("Forced failure");
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
                left -> Observable.never(),
                right -> Observable.never(),
                (BiFunction<Integer, Observable<Integer>, Object>) (r, l) -> l
        ));
    }

    @Test
    public void innerCompleteLeft() {
        Observable.just(1)
        .groupJoin(
            Observable.just(2),
                left -> Observable.empty(),
                right -> Observable.never(),
                (r, l) -> l
        )
        .flatMap(Functions.identity())
        .test()
        .assertResult();
    }

    @Test
    public void innerErrorLeft() {
        Observable.just(1)
        .groupJoin(
            Observable.just(2),
                left -> Observable.error(new TestException()),
                right -> Observable.never(),
                (r, l) -> l
        )
        .flatMap(Functions.identity())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerCompleteRight() {
        Observable.just(1)
        .groupJoin(
            Observable.just(2),
                left -> Observable.never(),
                right -> Observable.empty(),
                (r, l) -> l
        )
        .flatMap(Functions.identity())
        .test()
        .assertResult(2);
    }

    @Test
    @SuppressUndeliverable
    public void innerErrorRight() {
        Observable.just(1)
        .groupJoin(
            Observable.just(2),
                left -> Observable.never(),
                right -> Observable.error(new TestException()),
                (r, l) -> l
        )
        .flatMap(Functions.identity())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerErrorRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Object> ps1 = PublishSubject.create();
            final PublishSubject<Object> ps2 = PublishSubject.create();

            List<Throwable> errors = TestHelper.trackPluginErrors();

            try {
                TestObserverEx<Observable<Integer>> to = Observable.just(1)
                .groupJoin(
                    Observable.just(2).concatWith(Observable.never()),
                        left -> ps1,
                        right -> ps2,
                        (r, l) -> l
                )
                .to(TestHelper.testConsumer());

                final TestException ex1 = new TestException();
                final TestException ex2 = new TestException();

                Runnable r1 = () -> ps1.onError(ex1);
                Runnable r2 = () -> ps2.onError(ex2);

                TestHelper.race(r1, r2);

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
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Object> ps1 = PublishSubject.create();
            final PublishSubject<Object> ps2 = PublishSubject.create();

            List<Throwable> errors = TestHelper.trackPluginErrors();

            try {
                TestObserverEx<Object> to = ps1
                .groupJoin(
                    ps2,
                        left -> Observable.never(),
                        right -> Observable.never(),
                        (r, l) -> l
                )
                .flatMap(Functions.identity())
                .to(TestHelper.testConsumer());

                final TestException ex1 = new TestException();
                final TestException ex2 = new TestException();

                Runnable r1 = () -> ps1.onError(ex1);
                Runnable r2 = () -> ps2.onError(ex2);

                TestHelper.race(r1, r2);

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
                left -> Observable.never(),
                right -> Observable.never(),
                (r, l) -> l
        )
        .flatMap(Functions.identity())
        .test();

        ps2.onNext(2);

        ps1.onNext(1);
        ps1.onComplete();

        ps2.onComplete();

        to.assertResult(2);
    }

    @Test
    public void leftRightState() {
        JoinSupport js = mock(JoinSupport.class);

        LeftRightObserver o = new LeftRightObserver(js, false);

        assertFalse(o.isDisposed());

        o.onNext(1);
        o.onNext(2);

        o.dispose();

        assertTrue(o.isDisposed());

        verify(js).innerValue(false, 1);
        verify(js).innerValue(false, 2);
    }

    @Test
    public void leftRightEndState() {
        JoinSupport js = mock(JoinSupport.class);

        LeftRightEndObserver o = new LeftRightEndObserver(js, false, 0);

        assertFalse(o.isDisposed());

        o.onNext(1);
        o.onNext(2);

        assertTrue(o.isDisposed());

        verify(js).innerClose(false, o);
    }

    @Test
    public void disposeAfterOnNext() {
        PublishSubject<Integer> ps1 = PublishSubject.create();
        PublishSubject<Integer> ps2 = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<>();

        ps1.groupJoin(ps2, v -> Observable.never(), v -> Observable.never(), (a, b) -> a)
        .doOnNext(v -> to.dispose())
        .subscribe(to);

        ps2.onNext(1);
        ps1.onNext(1);
    }

    @Test
    public void completeWithMoreWork() {
        PublishSubject<Integer> ps1 = PublishSubject.create();
        PublishSubject<Integer> ps2 = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<>();

        ps1.groupJoin(ps2, v -> Observable.never(), v -> Observable.never(), (a, b) -> a)
        .doOnNext(v -> {
            if (v == 1) {
                ps2.onNext(2);
                ps1.onComplete();
                ps2.onComplete();
            }
        })
        .subscribe(to);

        ps2.onNext(1);
        ps1.onNext(1);
    }
}
