/**
 * Copyright 2016 Netflix, Inc.
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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import rx.Observable;
import rx.exceptions.*;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OnSubscribeConcatDelayErrorTest {

    @Test
    public void mainCompletes() {
        PublishSubject<Integer> source = PublishSubject.create();

        TestSubscriber<Integer> ts = TestSubscriber.create();

        source.concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.range(v, 2);
            }
        }).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onCompleted();

        ts.assertValues(1, 2, 2, 3);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void mainErrors() {
        PublishSubject<Integer> source = PublishSubject.create();

        TestSubscriber<Integer> ts = TestSubscriber.create();

        source.concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.range(v, 2);
            }
        }).subscribe(ts);

        source.onNext(1);
        source.onNext(2);
        source.onError(new TestException());

        ts.assertValues(1, 2, 2, 3);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void innerErrors() {
        final Observable<Integer> inner = Observable.range(1, 2).concatWith(Observable.<Integer>error(new TestException()));

        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.range(1, 3).concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return inner;
            }
        }).subscribe(ts);

        ts.assertValues(1, 2, 1, 2, 1, 2);
        ts.assertError(CompositeException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void singleInnerErrors() {
        final Observable<Integer> inner = Observable.range(1, 2).concatWith(Observable.<Integer>error(new TestException()));

        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.just(1)
        .asObservable() // prevent scalar optimization
        .concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return inner;
            }
        }).subscribe(ts);

        ts.assertValues(1, 2);
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void innerNull() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.just(1)
        .asObservable() // prevent scalar optimization
        .concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return null;
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(NullPointerException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void innerThrows() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.just(1)
        .asObservable() // prevent scalar optimization
        .concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                throw new TestException();
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

    @Test
    public void innerWithEmpty() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.range(1, 3)
        .concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return v == 2 ? Observable.<Integer>empty() : Observable.range(1, 2);
            }
        }).subscribe(ts);

        ts.assertValues(1, 2, 1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void innerWithScalar() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.range(1, 3)
        .concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return v == 2 ? Observable.just(3) : Observable.range(1, 2);
            }
        }).subscribe(ts);

        ts.assertValues(1, 2, 3, 1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void backpressure() {
        TestSubscriber<Integer> ts = TestSubscriber.create(0);

        Observable.range(1, 3).concatMapDelayError(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer v) {
                return Observable.range(v, 2);
            }
        }).subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(1);
        ts.assertValues(1);
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(3);
        ts.assertValues(1, 2, 2, 3);
        ts.assertNoErrors();
        ts.assertNotCompleted();

        ts.requestMore(2);

        ts.assertValues(1, 2, 2, 3, 3, 4);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    static <T> Observable<T> withError(Observable<T> source) {
        return source.concatWith(Observable.<T>error(new TestException()));
    }


    @Test
    public void concatDelayErrorObservable() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.concatDelayError(
                Observable.just(Observable.just(1), Observable.just(2)))
        .subscribe(ts);

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void concatDelayErrorObservableError() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.concatDelayError(
                withError(Observable.just(withError(Observable.just(1)), withError(Observable.just(2)))))
        .subscribe(ts);

        ts.assertValues(1, 2);
        ts.assertError(CompositeException.class);
        ts.assertNotCompleted();

        assertEquals(3, ((CompositeException)ts.getOnErrorEvents().get(0)).getExceptions().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatDelayErrorIterable() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.concatDelayError(
                Arrays.asList(Observable.just(1), Observable.just(2)))
        .subscribe(ts);

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatDelayErrorIterableError() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.concatDelayError(
                Arrays.asList(withError(Observable.just(1)), withError(Observable.just(2))))
        .subscribe(ts);

        ts.assertValues(1, 2);
        ts.assertError(CompositeException.class);
        ts.assertNotCompleted();

        assertEquals(2, ((CompositeException)ts.getOnErrorEvents().get(0)).getExceptions().size());
    }

    @Test
    public void concatDelayError2() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> o1 = Observable.just(1);
        Observable<Integer> o2 = Observable.just(2);

        Observable.concatDelayError(o1, o2)
                .subscribe(ts);

        ts.assertValues(1, 2);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void concatDelayError2Error() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> withError1 = withError(Observable.just(1));
        Observable<Integer> withError2 = withError(Observable.just(2));

        Observable.concatDelayError(withError1, withError2)
                .subscribe(ts);

        ts.assertValues(1, 2);
        ts.assertError(CompositeException.class);
        ts.assertNotCompleted();

        assertEquals(2, ((CompositeException)ts.getOnErrorEvents().get(0)).getExceptions().size());
    }

    @Test
    public void concatDelayError3() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> o1 = Observable.just(1);
        Observable<Integer> o2 = Observable.just(2);
        Observable<Integer> o3 = Observable.just(3);

        Observable.concatDelayError(o1, o2, o3)
                .subscribe(ts);

        ts.assertValues(1, 2, 3);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void concatDelayError3Error() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> withError1 = withError(Observable.just(1));
        Observable<Integer> withError2 = withError(Observable.just(2));
        Observable<Integer> withError3 = withError(Observable.just(3));

        Observable.concatDelayError(withError1, withError2, withError3)
                .subscribe(ts);

        ts.assertValues(1, 2, 3);
        ts.assertError(CompositeException.class);
        ts.assertNotCompleted();

        assertEquals(3, ((CompositeException)ts.getOnErrorEvents().get(0)).getExceptions().size());
    }

    @Test
    public void concatDelayError4() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> o1 = Observable.just(1);
        Observable<Integer> o2 = Observable.just(2);
        Observable<Integer> o3 = Observable.just(3);
        Observable<Integer> o4 = Observable.just(4);

        Observable.concatDelayError(o1, o2, o3, o4)
                .subscribe(ts);

        ts.assertValues(1, 2, 3, 4);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void concatDelayError4Error() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> withError1 = withError(Observable.just(1));
        Observable<Integer> withError2 = withError(Observable.just(2));
        Observable<Integer> withError3 = withError(Observable.just(3));
        Observable<Integer> withError4 = withError(Observable.just(4));

        Observable.concatDelayError(withError1, withError2, withError3, withError4)
                .subscribe(ts);

        ts.assertValues(1, 2, 3, 4);
        ts.assertError(CompositeException.class);
        ts.assertNotCompleted();

        assertEquals(4, ((CompositeException)ts.getOnErrorEvents().get(0)).getExceptions().size());
    }

    @Test
    public void concatDelayError5() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> o1 = Observable.just(1);
        Observable<Integer> o2 = Observable.just(2);
        Observable<Integer> o3 = Observable.just(3);
        Observable<Integer> o4 = Observable.just(4);
        Observable<Integer> o5 = Observable.just(5);

        Observable.concatDelayError(o1, o2, o3, o4, o5)
                .subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void concatDelayError5Error() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> withError1 = withError(Observable.just(1));
        Observable<Integer> withError2 = withError(Observable.just(2));
        Observable<Integer> withError3 = withError(Observable.just(3));
        Observable<Integer> withError4 = withError(Observable.just(4));
        Observable<Integer> withError5 = withError(Observable.just(5));

        Observable.concatDelayError(withError1, withError2, withError3, withError4, withError5)
                .subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5);
        ts.assertError(CompositeException.class);
        ts.assertNotCompleted();

        assertEquals(5, ((CompositeException)ts.getOnErrorEvents().get(0)).getExceptions().size());
    }

    @Test
    public void concatDelayError6() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> o1 = Observable.just(1);
        Observable<Integer> o2 = Observable.just(2);
        Observable<Integer> o3 = Observable.just(3);
        Observable<Integer> o4 = Observable.just(4);
        Observable<Integer> o5 = Observable.just(5);
        Observable<Integer> o6 = Observable.just(6);

        Observable.concatDelayError(o1, o2, o3, o4, o5, o6)
                .subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5, 6);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void concatDelayError6Error() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> withError1 = withError(Observable.just(1));
        Observable<Integer> withError2 = withError(Observable.just(2));
        Observable<Integer> withError3 = withError(Observable.just(3));
        Observable<Integer> withError4 = withError(Observable.just(4));
        Observable<Integer> withError5 = withError(Observable.just(5));
        Observable<Integer> withError6 = withError(Observable.just(6));

        Observable.concatDelayError(withError1, withError2, withError3, withError4, withError5, withError6)
                .subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5, 6);
        ts.assertError(CompositeException.class);
        ts.assertNotCompleted();

        assertEquals(6, ((CompositeException)ts.getOnErrorEvents().get(0)).getExceptions().size());
    }

    @Test
    public void concatDelayError7() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> o1 = Observable.just(1);
        Observable<Integer> o2 = Observable.just(2);
        Observable<Integer> o3 = Observable.just(3);
        Observable<Integer> o4 = Observable.just(4);
        Observable<Integer> o5 = Observable.just(5);
        Observable<Integer> o6 = Observable.just(6);
        Observable<Integer> o7 = Observable.just(7);

        Observable.concatDelayError(o1, o2, o3, o4, o5, o6, o7)
                .subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void concatDelayError7Error() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> withError1 = withError(Observable.just(1));
        Observable<Integer> withError2 = withError(Observable.just(2));
        Observable<Integer> withError3 = withError(Observable.just(3));
        Observable<Integer> withError4 = withError(Observable.just(4));
        Observable<Integer> withError5 = withError(Observable.just(5));
        Observable<Integer> withError6 = withError(Observable.just(6));
        Observable<Integer> withError7 = withError(Observable.just(7));

        Observable.concatDelayError(withError1, withError2, withError3, withError4, withError5, withError6, withError7)
                .subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7);
        ts.assertError(CompositeException.class);
        ts.assertNotCompleted();

        assertEquals(7, ((CompositeException)ts.getOnErrorEvents().get(0)).getExceptions().size());
    }

    @Test
    public void concatDelayError8() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> o1 = Observable.just(1);
        Observable<Integer> o2 = Observable.just(2);
        Observable<Integer> o3 = Observable.just(3);
        Observable<Integer> o4 = Observable.just(4);
        Observable<Integer> o5 = Observable.just(5);
        Observable<Integer> o6 = Observable.just(6);
        Observable<Integer> o7 = Observable.just(7);
        Observable<Integer> o8 = Observable.just(8);


        Observable.concatDelayError(o1, o2, o3, o4, o5, o6, o7, o8)
                .subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void concatDelayError8Error() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> withError1 = withError(Observable.just(1));
        Observable<Integer> withError2 = withError(Observable.just(2));
        Observable<Integer> withError3 = withError(Observable.just(3));
        Observable<Integer> withError4 = withError(Observable.just(4));
        Observable<Integer> withError5 = withError(Observable.just(5));
        Observable<Integer> withError6 = withError(Observable.just(6));
        Observable<Integer> withError7 = withError(Observable.just(7));
        Observable<Integer> withError8 = withError(Observable.just(8));

        Observable.concatDelayError(withError1, withError2, withError3, withError4, withError5, withError6, withError7, withError8)
                .subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8);
        ts.assertError(CompositeException.class);
        ts.assertNotCompleted();

        assertEquals(8, ((CompositeException)ts.getOnErrorEvents().get(0)).getExceptions().size());
    }

    @Test
    public void concatDelayError9() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> o1 = Observable.just(1);
        Observable<Integer> o2 = Observable.just(2);
        Observable<Integer> o3 = Observable.just(3);
        Observable<Integer> o4 = Observable.just(4);
        Observable<Integer> o5 = Observable.just(5);
        Observable<Integer> o6 = Observable.just(6);
        Observable<Integer> o7 = Observable.just(7);
        Observable<Integer> o8 = Observable.just(8);
        Observable<Integer> o9 = Observable.just(9);


        Observable.concatDelayError(o1, o2, o3, o4, o5, o6, o7, o8, o9)
                .subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void concatDelayError9Error() {
        TestSubscriber<Integer> ts = TestSubscriber.create();
        Observable<Integer> withError1 = withError(Observable.just(1));
        Observable<Integer> withError2 = withError(Observable.just(2));
        Observable<Integer> withError3 = withError(Observable.just(3));
        Observable<Integer> withError4 = withError(Observable.just(4));
        Observable<Integer> withError5 = withError(Observable.just(5));
        Observable<Integer> withError6 = withError(Observable.just(6));
        Observable<Integer> withError7 = withError(Observable.just(7));
        Observable<Integer> withError8 = withError(Observable.just(8));
        Observable<Integer> withError9 = withError(Observable.just(9));

        Observable.concatDelayError(withError1, withError2, withError3, withError4, withError5, withError6, withError7, withError8, withError9)
                .subscribe(ts);

        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9);
        ts.assertError(CompositeException.class);
        ts.assertNotCompleted();

        assertEquals(9, ((CompositeException)ts.getOnErrorEvents().get(0)).getExceptions().size());
    }
}
