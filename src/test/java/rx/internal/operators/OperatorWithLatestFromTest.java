/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.*;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.exceptions.TestException;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OperatorWithLatestFromTest {
    static final Func2<Integer, Integer, Integer> COMBINER = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer t1, Integer t2) {
            return (t1 << 8) + t2;
        }
    };
    static final Func2<Integer, Integer, Integer> COMBINER_ERROR = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer t1, Integer t2) {
            throw new TestException("Forced failure");
        }
    };
    @Test
    public void testSimple() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        @SuppressWarnings("unchecked")
        Observer<Integer> o = mock(Observer.class);
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

        other.onCompleted();
        inOrder.verify(o, never()).onCompleted();

        source.onNext(3);
        inOrder.verify(o).onNext((3 << 8) + 2);

        source.onCompleted();
        inOrder.verify(o).onCompleted();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testEmptySource() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        other.onNext(1);

        source.onCompleted();

        ts.assertNoErrors();
        ts.assertTerminalEvent();
        assertEquals(0, ts.getOnNextEvents().size());

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }

    @Test
    public void testEmptyOther() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        source.onNext(1);

        source.onCompleted();

        ts.assertNoErrors();
        ts.assertTerminalEvent();
        assertEquals(0, ts.getOnNextEvents().size());

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }


    @Test
    public void testUnsubscription() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        other.onNext(1);
        source.onNext(1);

        ts.unsubscribe();

        ts.assertReceivedOnNext(Arrays.asList((1 << 8) + 1));
        ts.assertNoErrors();
        assertEquals(0, ts.getCompletions());

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }

    @Test
    public void testSourceThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        other.onNext(1);
        source.onNext(1);

        source.onError(new TestException());

        ts.assertTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList((1 << 8) + 1));
        assertEquals(1, ts.getOnErrorEvents().size());
        assertTrue(ts.getOnErrorEvents().get(0) instanceof TestException);

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }
    @Test
    public void testOtherThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        other.onNext(1);
        source.onNext(1);

        other.onError(new TestException());

        ts.assertTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList((1 << 8) + 1));
        assertEquals(1, ts.getOnErrorEvents().size());
        assertTrue(ts.getOnErrorEvents().get(0) instanceof TestException);

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }

    @Test
    public void testFunctionThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER_ERROR);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        result.subscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(other.hasObservers());

        other.onNext(1);
        source.onNext(1);

        ts.assertTerminalEvent();
        assertEquals(0, ts.getOnNextEvents().size());
        assertEquals(1, ts.getOnErrorEvents().size());
        assertTrue(ts.getOnErrorEvents().get(0) instanceof TestException);

        assertFalse(source.hasObservers());
        assertFalse(other.hasObservers());
    }

    @Test
    public void testNoDownstreamUnsubscribe() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        result.unsafeSubscribe(ts);

        source.onCompleted();

        assertFalse(ts.isUnsubscribed());
    }
    @Test
    public void testBackpressure() {
        Observable<Integer> source = Observable.range(1, 10);
        PublishSubject<Integer> other = PublishSubject.create();

        Observable<Integer> result = source.withLatestFrom(other, COMBINER);

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>() {
            @Override
            public void onStart() {
                request(0);
            }
        };

        result.subscribe(ts);

        ts.requestMore(1);

        ts.assertReceivedOnNext(Collections.<Integer>emptyList());

        other.onNext(1);

        ts.requestMore(1);

        ts.assertReceivedOnNext(Arrays.asList((2 << 8) + 1));

        ts.requestMore(5);
        ts.assertReceivedOnNext(Arrays.asList(
                (2 << 8) + 1, (3 << 8) + 1, (4 << 8) + 1, (5 << 8) + 1,
                (6 << 8) + 1, (7 << 8) + 1
        ));

        ts.unsubscribe();

        assertFalse("Other has observers!", other.hasObservers());

        ts.assertNoErrors();
    }

    static final FuncN<String> toArray = new FuncN<String>() {
        @Override
        public String call(Object... args) {
            return Arrays.toString(args);
        }
    };

    @Test
    public void manySources() {
        PublishSubject<String> ps1 = PublishSubject.create();
        PublishSubject<String> ps2 = PublishSubject.create();
        PublishSubject<String> ps3 = PublishSubject.create();
        PublishSubject<String> main = PublishSubject.create();

        TestSubscriber<String> ts = new TestSubscriber<String>();

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

        ps3.onCompleted();
        ts.assertValues("[2, a, A, =]");

        ps1.onNext("b");

        main.onNext("3");

        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");

        main.onCompleted();
        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");
        ts.assertNoErrors();
        ts.assertCompleted();

        Assert.assertFalse("ps1 has subscribers?", ps1.hasObservers());
        Assert.assertFalse("ps2 has subscribers?", ps2.hasObservers());
        Assert.assertFalse("ps3 has subscribers?", ps3.hasObservers());
    }

    @Test
    public void manySourcesIterable() {
        PublishSubject<String> ps1 = PublishSubject.create();
        PublishSubject<String> ps2 = PublishSubject.create();
        PublishSubject<String> ps3 = PublishSubject.create();
        PublishSubject<String> main = PublishSubject.create();

        TestSubscriber<String> ts = new TestSubscriber<String>();

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

        ps3.onCompleted();
        ts.assertValues("[2, a, A, =]");

        ps1.onNext("b");

        main.onNext("3");

        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");

        main.onCompleted();
        ts.assertValues("[2, a, A, =]", "[3, b, B, =]");
        ts.assertNoErrors();
        ts.assertCompleted();

        Assert.assertFalse("ps1 has subscribers?", ps1.hasObservers());
        Assert.assertFalse("ps2 has subscribers?", ps2.hasObservers());
        Assert.assertFalse("ps3 has subscribers?", ps3.hasObservers());
    }

    @Test
    public void manySourcesIterableSweep() {
        for (String val : new String[] { "1", null }) {
            int n = 35;
            for (int i = 0; i < n; i++) {
                List<Observable<?>> sources = new ArrayList<Observable<?>>();
                List<String> expected = new ArrayList<String>();
                expected.add(val);

                for (int j = 0; j < i; j++) {
                    sources.add(Observable.just(val));
                    expected.add(String.valueOf(val));
                }

                TestSubscriber<String> ts = new TestSubscriber<String>();

                PublishSubject<String> main = PublishSubject.create();

                main.withLatestFrom(sources, toArray).subscribe(ts);

                ts.assertNoValues();

                main.onNext(val);
                main.onCompleted();

                ts.assertValue(expected.toString());
                ts.assertNoErrors();
                ts.assertCompleted();
            }
        }
    }

    @Test
    public void backpressureNoSignal() {
        PublishSubject<String> ps1 = PublishSubject.create();
        PublishSubject<String> ps2 = PublishSubject.create();

        TestSubscriber<String> ts = new TestSubscriber<String>(0);

        Observable.range(1, 10).withLatestFrom(new Observable<?>[] { ps1, ps2 }, toArray)
        .subscribe(ts);

        ts.assertNoValues();

        ts.requestMore(1);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertCompleted();

        Assert.assertFalse("ps1 has subscribers?", ps1.hasObservers());
        Assert.assertFalse("ps2 has subscribers?", ps2.hasObservers());
    }

    @Test
    public void backpressureWithSignal() {
        PublishSubject<String> ps1 = PublishSubject.create();
        PublishSubject<String> ps2 = PublishSubject.create();

        TestSubscriber<String> ts = new TestSubscriber<String>(0);

        Observable.range(1, 3).withLatestFrom(new Observable<?>[] { ps1, ps2 }, toArray)
        .subscribe(ts);

        ts.assertNoValues();

        ps1.onNext("1");
        ps2.onNext("1");

        ts.requestMore(1);

        ts.assertValue("[1, 1, 1]");

        ts.requestMore(1);

        ts.assertValues("[1, 1, 1]", "[2, 1, 1]");

        ts.requestMore(1);

        ts.assertValues("[1, 1, 1]", "[2, 1, 1]", "[3, 1, 1]");
        ts.assertNoErrors();
        ts.assertCompleted();

        Assert.assertFalse("ps1 has subscribers?", ps1.hasObservers());
        Assert.assertFalse("ps2 has subscribers?", ps2.hasObservers());
    }

    @Test
    public void withEmpty() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);

        Observable.range(1, 3).withLatestFrom(
                new Observable<?>[] { Observable.just(1), Observable.empty() }, toArray)
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void withError() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);

        Observable.range(1, 3).withLatestFrom(
                new Observable<?>[] { Observable.just(1), Observable.error(new TestException()) }, toArray)
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void withMainError() {
        TestSubscriber<String> ts = new TestSubscriber<String>(0);

        Observable.error(new TestException()).withLatestFrom(
                new Observable<?>[] { Observable.just(1), Observable.just(1) }, toArray)
        .subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void with2Others() {
        Observable<Integer> just = Observable.just(1);

        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        just.withLatestFrom(just, just, new Func3<Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer a, Integer b, Integer c) {
                return Arrays.asList(a, b, c);
            }
        })
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1));
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void with3Others() {
        Observable<Integer> just = Observable.just(1);

        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        just.withLatestFrom(just, just, just, new Func4<Integer, Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer a, Integer b, Integer c, Integer d) {
                return Arrays.asList(a, b, c, d);
            }
        })
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1, 1));
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void with4Others() {
        Observable<Integer> just = Observable.just(1);

        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        just.withLatestFrom(just, just, just, just, new Func5<Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer a, Integer b, Integer c, Integer d, Integer e) {
                return Arrays.asList(a, b, c, d, e);
            }
        })
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1, 1, 1));
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void with5Others() {
        Observable<Integer> just = Observable.just(1);

        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        just.withLatestFrom(just, just, just, just, just, new Func6<Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer a, Integer b, Integer c, Integer d, Integer e, Integer f) {
                return Arrays.asList(a, b, c, d, e, f);
            }
        })
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1, 1, 1, 1));
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void with6Others() {
        Observable<Integer> just = Observable.just(1);

        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        just.withLatestFrom(just, just, just, just, just, just, new Func7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer a, Integer b, Integer c, Integer d, Integer e, Integer f, Integer g) {
                return Arrays.asList(a, b, c, d, e, f, g);
            }
        })
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1, 1, 1, 1, 1));
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void with7Others() {
        Observable<Integer> just = Observable.just(1);

        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        just.withLatestFrom(just, just, just, just, just, just, just, new Func8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer a, Integer b, Integer c, Integer d, Integer e, Integer f, Integer g, Integer i) {
                return Arrays.asList(a, b, c, d, e, f, g, i);
            }
        })
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1, 1, 1, 1, 1, 1));
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void with8Others() {
        Observable<Integer> just = Observable.just(1);

        TestSubscriber<List<Integer>> ts = new TestSubscriber<List<Integer>>();

        just.withLatestFrom(just, just, just, just, just, just, just, just, new Func9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer a, Integer b, Integer c, Integer d, Integer e, Integer f, Integer g, Integer i, Integer j) {
                return Arrays.asList(a, b, c, d, e, f, g, i, j);
            }
        })
        .subscribe(ts);

        ts.assertValue(Arrays.asList(1, 1, 1, 1, 1, 1, 1, 1, 1));
        ts.assertNoErrors();
        ts.assertCompleted();
    }

}
