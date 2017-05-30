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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.util.CrashingMappedIterable;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.PublishSubject;

public class ObservableWithLatestFromTest {
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
    public void testSimple() {
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
    public void testEmptySource() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestObserver<Integer> ts = new TestObserver<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        other.onNext(1);

        source.onComplete();

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertNoValues();

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }

    @Test
    public void testEmptyOther() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestObserver<Integer> ts = new TestObserver<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        source.onNext(1);

        source.onComplete();

        ts.assertNoErrors();
        ts.assertTerminated();
        ts.assertNoValues();

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }


    @Test
    public void testUnsubscription() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestObserver<Integer> ts = new TestObserver<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        other.onNext(1);
        source.onNext(1);

        ts.dispose();

        ts.assertValue((1 << 8) + 1);
        ts.assertNoErrors();
        ts.assertNotComplete();

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }

    @Test
    public void testSourceThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestObserver<Integer> ts = new TestObserver<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        other.onNext(1);
        source.onNext(1);

        source.onError(new TestException());

        ts.assertTerminated();
        ts.assertValue((1 << 8) + 1);
        ts.assertError(TestException.class);
        ts.assertNotComplete();

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }
    @Test
    public void testOtherThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestObserver<Integer> ts = new TestObserver<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        other.onNext(1);
        source.onNext(1);

        other.onError(new TestException());

        ts.assertTerminated();
        ts.assertValue((1 << 8) + 1);
        ts.assertNotComplete();
        ts.assertError(TestException.class);

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }

    @Test
    public void testFunctionThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER_ERROR);

        TestObserver<Integer> ts = new TestObserver<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        other.onNext(1);
        source.onNext(1);

        ts.assertTerminated();
        ts.assertNotComplete();
        ts.assertNoValues();
        ts.assertError(TestException.class);

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }

    @Test
    public void testNoDownstreamUnsubscribe() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestObserver<Integer> ts = new TestObserver<Integer>();

        result.subscribe(ts);

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

        TestObserver<String> ts = new TestObserver<String>();

        main.withLatestFrom(new Observable[] { ps1, ps2, ps3 }, toArray)
        .subscribe(ts);

        main.onNext("1");
        ts.assertNoValues();
        ps1.onNext("a");
        ts.assertNoValues();
        ps2.onNext("A");
        ts.assertNoValues();
        ps3.onNext("=");
        ts.assertNoValues();

        main.onNext("2");
        ts.assertValues("[2, a, A, =]");

        ps2.onNext("B");

        ts.assertValues("[2, a, A, =]");

        ps3.onComplete();
        ts.assertValues("[2, a, A, =]");

        ps1.onNext("b");

        main.onNext("3");

        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");

        main.onComplete();
        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");
        ts.assertNoErrors();
        ts.assertComplete();

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

        TestObserver<String> ts = new TestObserver<String>();

        main.withLatestFrom(Arrays.<Observable<?>>asList(ps1, ps2, ps3), toArray)
        .subscribe(ts);

        main.onNext("1");
        ts.assertNoValues();
        ps1.onNext("a");
        ts.assertNoValues();
        ps2.onNext("A");
        ts.assertNoValues();
        ps3.onNext("=");
        ts.assertNoValues();

        main.onNext("2");
        ts.assertValues("[2, a, A, =]");

        ps2.onNext("B");

        ts.assertValues("[2, a, A, =]");

        ps3.onComplete();
        ts.assertValues("[2, a, A, =]");

        ps1.onNext("b");

        main.onNext("3");

        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");

        main.onComplete();
        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");
        ts.assertNoErrors();
        ts.assertComplete();

        assertFalse("ps1 has subscribers?", ps1.hasObservers());
        assertFalse("ps2 has subscribers?", ps2.hasObservers());
        assertFalse("ps3 has subscribers?", ps3.hasObservers());
    }

    @Test
    public void manySourcesIterableSweep() {
        for (String val : new String[] { "1" /*, null*/ }) {
            int n = 35;
            for (int i = 0; i < n; i++) {
                List<Observable<?>> sources = new ArrayList<Observable<?>>();
                List<String> expected = new ArrayList<String>();
                expected.add(val);

                for (int j = 0; j < i; j++) {
                    sources.add(Observable.just(val));
                    expected.add(String.valueOf(val));
                }

                TestObserver<String> ts = new TestObserver<String>();

                PublishSubject<String> main = PublishSubject.create();

                main.withLatestFrom(sources, toArray).subscribe(ts);

                ts.assertNoValues();

                main.onNext(val);
                main.onComplete();

                ts.assertValue(expected.toString());
                ts.assertNoErrors();
                ts.assertComplete();
            }
        }
    }

    @Test
    @Ignore("Observable doesn't support backpressure")
    public void backpressureNoSignal() {
//        PublishSubject<String> ps1 = PublishSubject.create();
//        PublishSubject<String> ps2 = PublishSubject.create();
//
//        TestObserver<String> ts = new TestObserver<String>();
//
//        Observable.range(1, 10).withLatestFrom(new Observable<?>[] { ps1, ps2 }, toArray)
//        .subscribe(ts);
//
//        ts.assertNoValues();
//
//        ts.request(1);
//
//        ts.assertNoValues();
//        ts.assertNoErrors();
//        ts.assertComplete();
//
//        assertFalse("ps1 has subscribers?", ps1.hasSubscribers());
//        assertFalse("ps2 has subscribers?", ps2.hasSubscribers());
    }

    @Test
    @Ignore("Observable doesn't support backpressure")
    public void backpressureWithSignal() {
//        PublishSubject<String> ps1 = PublishSubject.create();
//        PublishSubject<String> ps2 = PublishSubject.create();
//
//        TestObserver<String> ts = new TestObserver<String>();
//
//        Observable.range(1, 3).withLatestFrom(new Observable<?>[] { ps1, ps2 }, toArray)
//        .subscribe(ts);
//
//        ts.assertNoValues();
//
//        ps1.onNext("1");
//        ps2.onNext("1");
//
//        ts.request(1);
//
//        ts.assertValue("[1, 1, 1]");
//
//        ts.request(1);
//
//        ts.assertValues("[1, 1, 1]", "[2, 1, 1]");
//
//        ts.request(1);
//
//        ts.assertValues("[1, 1, 1]", "[2, 1, 1]", "[3, 1, 1]");
//        ts.assertNoErrors();
//        ts.assertComplete();
//
//        assertFalse("ps1 has subscribers?", ps1.hasSubscribers());
//        assertFalse("ps2 has subscribers?", ps2.hasSubscribers());
    }

    @Test
    public void withEmpty() {
        TestObserver<String> ts = new TestObserver<String>();

        Observable.range(1, 3).withLatestFrom(
                new Observable<?>[] { Observable.just(1), Observable.empty() }, toArray)
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void withError() {
        TestObserver<String> ts = new TestObserver<String>();

        Observable.range(1, 3).withLatestFrom(
                new Observable<?>[] { Observable.just(1), Observable.error(new TestException()) }, toArray)
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void withMainError() {
        TestObserver<String> ts = new TestObserver<String>();

        Observable.error(new TestException()).withLatestFrom(
                new Observable<?>[] { Observable.just(1), Observable.just(1) }, toArray)
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotComplete();
    }

    @Test
    public void with2Others() {
        Observable<Integer> just = Observable.just(1);

        TestObserver<List<Integer>> ts = new TestObserver<List<Integer>>();

        just.withLatestFrom(just, just, new Function3<Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> apply(Integer a, Integer b, Integer c) {
                return Arrays.asList(a, b, c);
            }
        })
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1));
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void with3Others() {
        Observable<Integer> just = Observable.just(1);

        TestObserver<List<Integer>> ts = new TestObserver<List<Integer>>();

        just.withLatestFrom(just, just, just, new Function4<Integer, Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> apply(Integer a, Integer b, Integer c, Integer d) {
                return Arrays.asList(a, b, c, d);
            }
        })
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1, 1));
        ts.assertNoErrors();
        ts.assertComplete();
    }

    @Test
    public void with4Others() {
        Observable<Integer> just = Observable.just(1);

        TestObserver<List<Integer>> ts = new TestObserver<List<Integer>>();

        just.withLatestFrom(just, just, just, just, new Function5<Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> apply(Integer a, Integer b, Integer c, Integer d, Integer e) {
                return Arrays.asList(a, b, c, d, e);
            }
        })
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1, 1, 1));
        ts.assertNoErrors();
        ts.assertComplete();
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
        .withLatestFrom(new CrashingMappedIterable<Observable<Integer>>(1, 100, 100, new Function<Integer, Observable<Integer>>() {
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
        .test()
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
                    observer.onSubscribe(Disposables.empty());
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
            .test()
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

    @SuppressWarnings("unchecked")
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
        .test()
        .assertFailureAndMessage(NullPointerException.class, "The combiner returned a null value");
    }
}
