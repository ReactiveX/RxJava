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
package rx.joins.operators;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.Functions;
import rx.joins.Plan0;
import rx.observables.JoinObservable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OperatorJoinsTest {
    @Mock
    Observer<Integer> observer;

    Func2<Integer, Integer, Integer> add2 = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer t1, Integer t2) {
            return t1 + t2;
        }
    };
    Func2<Integer, Integer, Integer> mul2 = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer t1, Integer t2) {
            return t1 * t2;
        }
    };
    Func2<Integer, Integer, Integer> sub2 = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer t1, Integer t2) {
            return t1 - t2;
        }
    };

    Func3<Integer, Integer, Integer, Integer> add3 = new Func3<Integer, Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer t1, Integer t2, Integer t3) {
            return t1 + t2 + t3;
        }
    };
    Func1<Integer, Integer> func1Throw = new Func1<Integer, Integer>() {
        @Override
        public Integer call(Integer t1) {
            throw new RuntimeException("Forced failure");
        }
    };
    Func2<Integer, Integer, Integer> func2Throw = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer t1, Integer t2) {
            throw new RuntimeException("Forced failure");
        }
    };
    Func3<Integer, Integer, Integer, Integer> func3Throw = new Func3<Integer, Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer t1, Integer t2, Integer t3) {
            throw new RuntimeException("Forced failure");
        }
    };

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = NullPointerException.class)
    public void and2ArgumentNull() {
        Observable<Integer> some = Observable.just(1);
        JoinObservable.from(some).and(null);
    }

    @Test(expected = NullPointerException.class)
    public void and3argumentNull() {
        Observable<Integer> some = Observable.just(1);
        JoinObservable.from(some).and(some).and(null);
    }

    @Test
    public void and2() {
        Observable<Integer> some = Observable.just(1);

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(some).and(some).then(add2)).toObservable();

        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void and2Error1() {
        Observable<Integer> error = Observable.error(new RuntimeException("Forced failure"));

        Observable<Integer> some = Observable.just(1);

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(error).and(some).then(add2)).toObservable();

        m.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void and2Error2() {
        Observable<Integer> error = Observable.error(new RuntimeException("Forced failure"));

        Observable<Integer> some = Observable.just(1);

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(some).and(error).then(add2)).toObservable();

        m.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void and3() {
        Observable<Integer> some = Observable.just(1);

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(some).and(some).and(some).then(add3)).toObservable();

        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void and3Error1() {
        Observable<Integer> error = Observable.error(new RuntimeException("Forced failure"));

        Observable<Integer> some = Observable.just(1);

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(error).and(some).and(some).then(add3)).toObservable();

        m.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void and3Error2() {
        Observable<Integer> error = Observable.error(new RuntimeException("Forced failure"));

        Observable<Integer> some = Observable.just(1);

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(some).and(error).and(some).then(add3)).toObservable();

        m.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void and3Error3() {
        Observable<Integer> error = Observable.error(new RuntimeException("Forced failure"));

        Observable<Integer> some = Observable.just(1);

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(some).and(some).and(error).then(add3)).toObservable();

        m.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, never()).onCompleted();
    }

    @Test(expected = NullPointerException.class)
    public void thenArgumentNull() {
        Observable<Integer> some = Observable.just(1);

        JoinObservable.from(some).then(null);
    }

    @Test(expected = NullPointerException.class)
    public void then2ArgumentNull() {
        Observable<Integer> some = Observable.just(1);

        JoinObservable.from(some).and(some).then(null);
    }

    @Test(expected = NullPointerException.class)
    public void then3ArgumentNull() {
        Observable<Integer> some = Observable.just(1);

        JoinObservable.from(some).and(some).and(some).then(null);
    }

    @Test
    public void then1() {
        Observable<Integer> some = Observable.just(1);

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(some).then(Functions.<Integer> identity())).toObservable();
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void then1Error() {
        Observable<Integer> some = Observable.error(new RuntimeException("Forced failure"));

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(some).then(Functions.<Integer> identity())).toObservable();
        m.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void then1Throws() {
        Observable<Integer> some = Observable.just(1);

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(some).then(func1Throw)).toObservable();
        m.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void then2Throws() {
        Observable<Integer> some = Observable.just(1);

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(some).and(some).then(func2Throw)).toObservable();
        m.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void then3Throws() {
        Observable<Integer> some = Observable.just(1);

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(some).and(some).and(some).then(func3Throw)).toObservable();
        m.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, never()).onCompleted();
    }

    @Test(expected = NullPointerException.class)
    public void whenArgumentNull1() {
        JoinObservable.when((Plan0<Object>[]) null);
    }

    @Test(expected = NullPointerException.class)
    public void whenArgumentNull2() {
        JoinObservable.when((Iterable<Plan0<Object>>) null);
    }

    @Test
    public void whenMultipleSymmetric() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);
        Observable<Integer> source2 = Observable.from(4, 5, 6);

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(source1).and(source2).then(add2)).toObservable();
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(1 + 4);
        verify(observer, times(1)).onNext(2 + 5);
        verify(observer, times(1)).onNext(3 + 6);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void whenMultipleAsymSymmetric() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);
        Observable<Integer> source2 = Observable.from(4, 5);

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(source1).and(source2).then(add2)).toObservable();
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(1 + 4);
        verify(observer, times(1)).onNext(2 + 5);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void whenEmptyEmpty() {
        Observable<Integer> source1 = Observable.empty();
        Observable<Integer> source2 = Observable.empty();

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(source1).and(source2).then(add2)).toObservable();
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void whenNeverNever() {
        Observable<Integer> source1 = Observable.never();
        Observable<Integer> source2 = Observable.never();

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(source1).and(source2).then(add2)).toObservable();
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void whenThrowNonEmpty() {
        Observable<Integer> source1 = Observable.empty();
        Observable<Integer> source2 = Observable.error(new RuntimeException("Forced failure"));

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(source1).and(source2).then(add2)).toObservable();
        m.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void whenComplicated() {
        PublishSubject<Integer> xs = PublishSubject.create();
        PublishSubject<Integer> ys = PublishSubject.create();
        PublishSubject<Integer> zs = PublishSubject.create();

        Observable<Integer> m = JoinObservable.when(
                JoinObservable.from(xs).and(ys).then(add2), // 1+4=5, 2+5=7, 3+6=9
                JoinObservable.from(xs).and(zs).then(mul2), // 1*7=7, 2*8=16, 3*9=27
                JoinObservable.from(ys).and(zs).then(sub2)  // 4-7=-3, 5-8=-3, 6-9=-3
                ).toObservable();

        TestSubscriber<Integer> to = new TestSubscriber<Integer>(observer);
        m.subscribe(to);

        xs.onNext(1); // t == 210, xs[1], ys[], zs[]

        xs.onNext(2); // t == 220, xs[1, 2], ys[], zs[]
        zs.onNext(7); // t == 220, xs[1, 2], ys[], zs[7] triggers x and z; emit 1 * 7, remains xs[2], ys[], zs[]

        xs.onNext(3); // t == 230, xs[2,3], ys[], zs[]
        zs.onNext(8); // t == 230, xs[2,3], ys[], zs[8] triggers x and z, emit 2 * 8, remains xs[3], ys[], zs[]

        ys.onNext(4); // t == 240, xs[], ys[4], zs[] triggers x and y, emit 3 + 4, remains xs[], ys[], zs[]
        zs.onNext(9); // t == 240, xs[], ys[], zs[9]
        xs.onCompleted(); // t == 240, completed 1

        ys.onNext(5); // t == 250, xs[], ys[5], zs[9], triggers ys and zs, emits 5 - 9, remains xs[], ys[], zs[]

        ys.onNext(6); // t == 260, xs[], ys[6], zs[]

        ys.onCompleted(); // t == 270, completed 2

        zs.onCompleted(); // t == 300, completed 3, triggers when() oncompleted

        System.out.println("Events: " + to.getOnNextEvents());

        to.assertReceivedOnNext(Arrays.asList(7, 16, 7, -4));
        to.assertTerminalEvent();

        InOrder inOrder = inOrder(observer);

        inOrder.verify(observer, times(1)).onNext(1 * 7);
        inOrder.verify(observer, times(1)).onNext(2 * 8);
        inOrder.verify(observer, times(1)).onNext(3 + 4);
        inOrder.verify(observer, times(1)).onNext(5 - 9);
        inOrder.verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }
}
