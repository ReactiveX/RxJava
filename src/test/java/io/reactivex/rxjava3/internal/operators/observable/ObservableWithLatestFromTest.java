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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import io.reactivex.rxjava3.disposables.Disposable;
import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.util.CrashingMappedIterable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.*;

public class ObservableWithLatestFromTest extends RxJavaTest {
    static final BiFunction<Integer, Integer, Integer> COMBINER = new BiFunction<Integer, Integer, Integer>() {
        @Override
        public Integer apply(Integer t1, Integer t2) {
            return (t1 << 8) + t2;
        }
    };
    static final BiFunction<Integer, Integer, Integer> COMBINER_ERROR = new BiFunction<Integer, Integer, Integer>() {
        @Override
        public Integer apply(Integer t1, Integer t2) {
            throw new TestException("Forced failure");
        }
    };
    @Test
    public void simple() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observer<Integer> o = TestHelper.mockObserver();
        InOrder inOrder = inOrder(o);

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        result.subscribe(o);

        source.onNext(1);
        inOrder.verify(o, never()).onNext(anyInt());

        other.onNext(1);
        inOrder.verify(o, never()).onNext(anyInt());

        source.onNext(2);
        inOrder.verify(o).onNext((2 << 8) + 1);

        other.onNext(2);
        inOrder.verify(o, never()).onNext(anyInt());

        other.onComplete();
        inOrder.verify(o, never()).onComplete();

        source.onNext(3);
        inOrder.verify(o).onNext((3 << 8) + 2);

        source.onComplete();
        inOrder.verify(o).onComplete();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void emptySource() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestObserverEx<Integer> to = new TestObserverEx<>();

        result.subscribe(to);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        other.onNext(1);

        source.onComplete();

        to.assertNoErrors();
        to.assertTerminated();
        to.assertNoValues();

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }

    @Test
    public void emptyOther() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestObserverEx<Integer> to = new TestObserverEx<>();

        result.subscribe(to);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        source.onNext(1);

        source.onComplete();

        to.assertNoErrors();
        to.assertTerminated();
        to.assertNoValues();

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }

    @Test
    public void unsubscription() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestObserver<Integer> to = new TestObserver<>();

        result.subscribe(to);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        other.onNext(1);
        source.onNext(1);

        to.dispose();

        to.assertValue((1 << 8) + 1);
        to.assertNoErrors();
        to.assertNotComplete();

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }

    @Test
    public void sourceThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestObserverEx<Integer> to = new TestObserverEx<>();

        result.subscribe(to);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        other.onNext(1);
        source.onNext(1);

        source.onError(new TestException());

        to.assertTerminated();
        to.assertValue((1 << 8) + 1);
        to.assertError(TestException.class);
        to.assertNotComplete();

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }

    @Test
    public void otherThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestObserverEx<Integer> to = new TestObserverEx<>();

        result.subscribe(to);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        other.onNext(1);
        source.onNext(1);

        other.onError(new TestException());

        to.assertTerminated();
        to.assertValue((1 << 8) + 1);
        to.assertNotComplete();
        to.assertError(TestException.class);

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }

    @Test
    public void functionThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER_ERROR);

        TestObserverEx<Integer> to = new TestObserverEx<>();

        result.subscribe(to);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        other.onNext(1);
        source.onNext(1);

        to.assertTerminated();
        to.assertNotComplete();
        to.assertNoValues();
        to.assertError(TestException.class);

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }

    @Test
    public void noDownstreamUnsubscribe() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestObserver<Integer> to = new TestObserver<>();

        result.subscribe(to);

        source.onComplete();

        // 2.0.2 - not anymore
//        assertTrue("Not cancelled!", ts.isCancelled());
    }

    static final Function<Object[], String> toArray = new Function<Object[], String>() {
        @Override
        public String apply(Object[] args) {
            return Arrays.toString(args);
        }
    };

    @Test
    public void manySources() {
        PublishSubject<String> ps1 = PublishSubject.create();
        PublishSubject<String> ps2 = PublishSubject.create();
        PublishSubject<String> ps3 = PublishSubject.create();
        PublishSubject<String> main = PublishSubject.create();

        TestObserver<String> to = new TestObserver<>();

        main.withLatestFrom(new Observable[] { ps1, ps2, ps3 }, toArray)
        .subscribe(to);

        main.onNext("1");
        to.assertNoValues();
        ps1.onNext("a");
        to.assertNoValues();
        ps2.onNext("A");
        to.assertNoValues();
        ps3.onNext("=");
        to.assertNoValues();

        main.onNext("2");
        to.assertValues("[2, a, A, =]");

        ps2.onNext("B");

        to.assertValues("[2, a, A, =]");

        ps3.onComplete();
        to.assertValues("[2, a, A, =]");

        ps1.onNext("b");

        main.onNext("3");

        to.assertValues("[2, a, A, =]", "[3, b, B, =]");

        main.onComplete();
        to.assertValues("[2, a, A, =]", "[3, b, B, =]");
        to.assertNoErrors();
        to.assertComplete();

        assertFalse("ps1 has subscribers?", ps1.hasObservers());
        assertFalse("ps2 has subscribers?", ps2.hasObservers());
        assertFalse("ps3 has subscribers?", ps3.hasObservers());
    }

    @Test
    public void manySourcesIterable() {
        PublishSubject<String> ps1 = PublishSubject.create();
        PublishSubject<String> ps2 = PublishSubject.create();
        PublishSubject<String> ps3 = PublishSubject.create();
        PublishSubject<String> main = PublishSubject.create();

        TestObserver<String> to = new TestObserver<>();

        main.withLatestFrom(Arrays.<Observable<?>>asList(ps1, ps2, ps3), toArray)
        .subscribe(to);

        main.onNext("1");
        to.assertNoValues();
        ps1.onNext("a");
        to.assertNoValues();
        ps2.onNext("A");
        to.assertNoValues();
        ps3.onNext("=");
        to.assertNoValues();

        main.onNext("2");
        to.assertValues("[2, a, A, =]");

        ps2.onNext("B");

        to.assertValues("[2, a, A, =]");

        ps3.onComplete();
        to.assertValues("[2, a, A, =]");

        ps1.onNext("b");

        main.onNext("3");

        to.assertValues("[2, a, A, =]", "[3, b, B, =]");

        main.onComplete();
        to.assertValues("[2, a, A, =]", "[3, b, B, =]");
        to.assertNoErrors();
        to.assertComplete();

        assertFalse("ps1 has subscribers?", ps1.hasObservers());
        assertFalse("ps2 has subscribers?", ps2.hasObservers());
        assertFalse("ps3 has subscribers?", ps3.hasObservers());
    }

    @Test
    public void manySourcesIterableSweep() {
        for (String val : new String[] { "1" /*, null*/ }) {
            int n = 35;
            for (int i = 0; i < n; i++) {
                List<Observable<?>> sources = new ArrayList<>();
                List<String> expected = new ArrayList<>();
                expected.add(val);

                for (int j = 0; j < i; j++) {
                    sources.add(Observable.just(val));
                    expected.add(String.valueOf(val));
                }

                TestObserver<String> to = new TestObserver<>();

                PublishSubject<String> main = PublishSubject.create();

                main.withLatestFrom(sources, toArray).subscribe(to);

                to.assertNoValues();

                main.onNext(val);
                main.onComplete();

                to.assertValue(expected.toString());
                to.assertNoErrors();
                to.assertComplete();
            }
        }
    }

    @Test
    public void withEmpty() {
        TestObserver<String> to = new TestObserver<>();

        Observable.range(1, 3).withLatestFrom(
                new Observable<?>[] { Observable.just(1), Observable.empty() }, toArray)
        .subscribe(to);

        to.assertNoValues();
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void withError() {
        TestObserver<String> to = new TestObserver<>();

        Observable.range(1, 3).withLatestFrom(
                new Observable<?>[] { Observable.just(1), Observable.error(new TestException()) }, toArray)
        .subscribe(to);

        to.assertNoValues();
        to.assertError(TestException.class);
        to.assertNotComplete();
    }

    @Test
    public void withMainError() {
        TestObserver<String> to = new TestObserver<>();

        Observable.error(new TestException()).withLatestFrom(
                new Observable<?>[] { Observable.just(1), Observable.just(1) }, toArray)
        .subscribe(to);

        to.assertNoValues();
        to.assertError(TestException.class);
        to.assertNotComplete();
    }

    @Test
    public void with2Others() {
        Observable<Integer> just = Observable.just(1);

        TestObserver<List<Integer>> to = new TestObserver<>();

        just.withLatestFrom(just, just, new Function3<Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> apply(Integer a, Integer b, Integer c) {
                return Arrays.asList(a, b, c);
            }
        })
        .subscribe(to);

        to.assertValue(Arrays.asList(1, 1, 1));
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void with3Others() {
        Observable<Integer> just = Observable.just(1);

        TestObserver<List<Integer>> to = new TestObserver<>();

        just.withLatestFrom(just, just, just, new Function4<Integer, Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> apply(Integer a, Integer b, Integer c, Integer d) {
                return Arrays.asList(a, b, c, d);
            }
        })
        .subscribe(to);

        to.assertValue(Arrays.asList(1, 1, 1, 1));
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void with4Others() {
        Observable<Integer> just = Observable.just(1);

        TestObserver<List<Integer>> to = new TestObserver<>();

        just.withLatestFrom(just, just, just, just, new Function5<Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> apply(Integer a, Integer b, Integer c, Integer d, Integer e) {
                return Arrays.asList(a, b, c, d, e);
            }
        })
        .subscribe(to);

        to.assertValue(Arrays.asList(1, 1, 1, 1, 1));
        to.assertNoErrors();
        to.assertComplete();
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.just(1).withLatestFrom(Observable.just(2), new BiFunction<Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b) throws Exception {
                return a;
            }
        }));

        TestHelper.checkDisposed(Observable.just(1).withLatestFrom(Observable.just(2), Observable.just(3), new Function3<Integer, Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b, Integer c) throws Exception {
                return a;
            }
        }));
    }

    @Test
    public void manyIteratorThrows() {
        Observable.just(1)
        .withLatestFrom(new CrashingMappedIterable<>(1, 100, 100, new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) throws Exception {
                return Observable.just(2);
            }
        }), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] a) throws Exception {
                return a;
            }
        })
        .to(TestHelper.<Object>testConsumer())
        .assertFailureAndMessage(TestException.class, "iterator()");
    }

    @Test
    public void manyCombinerThrows() {
        Observable.just(1).withLatestFrom(Observable.just(2), Observable.just(3), new Function3<Integer, Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b, Integer c) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void manyErrors() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposable.empty());
                    observer.onError(new TestException("First"));
                    observer.onNext(1);
                    observer.onError(new TestException("Second"));
                    observer.onComplete();
                }
            }.withLatestFrom(Observable.just(2), Observable.just(3), new Function3<Integer, Integer, Integer, Object>() {
                @Override
                public Object apply(Integer a, Integer b, Integer c) throws Exception {
                    return a;
                }
            })
            .to(TestHelper.<Object>testConsumer())
            .assertFailureAndMessage(TestException.class, "First");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void combineToNull1() {
        Observable.just(1)
        .withLatestFrom(Observable.just(2), new BiFunction<Integer, Integer, Object>() {
            @Override
            public Object apply(Integer a, Integer b) throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void combineToNull2() {
        Observable.just(1)
        .withLatestFrom(Arrays.asList(Observable.just(2), Observable.just(3)), new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] o) throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void zeroOtherCombinerReturnsNull() {
        Observable.just(1)
        .withLatestFrom(new Observable[0], Functions.justFunction(null))
        .to(TestHelper.<Object>testConsumer())
        .assertFailureAndMessage(NullPointerException.class, "The combiner returned a null value");
    }
}
