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

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
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
import rx.exceptions.TestException;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.functions.Func5;
import rx.functions.Func6;
import rx.functions.Func7;
import rx.functions.Func8;
import rx.functions.Func9;
import rx.functions.FuncN;
import rx.functions.Functions;
import rx.joins.PatternN;
import rx.joins.Plan0;
import rx.observables.JoinObservable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OperatorJoinsTest {
    @Mock
    Observer<Integer> observer;

    static final class Adder implements
    Func2<Integer, Integer, Integer>,
    Func3<Integer, Integer, Integer, Integer>,
    Func4<Integer, Integer, Integer, Integer, Integer>,
    Func5<Integer, Integer, Integer, Integer, Integer, Integer>,
    Func6<Integer, Integer, Integer, Integer, Integer, Integer, Integer>,
    Func7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>,
    Func8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>,
    Func9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>,
    FuncN<Integer>
    {

		@Override
		public Integer call(Object... args) {
			int sum = 0;
			
			for(Object o : args) {
				sum += (Integer)o;
			}
			
			return sum;
		}

		@Override
		public Integer call(Integer t1, Integer t2, Integer t3, Integer t4,
				Integer t5, Integer t6, Integer t7, Integer t8, Integer t9) {
			return t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8 + t9;
		}

		@Override
		public Integer call(Integer t1, Integer t2, Integer t3, Integer t4,
				Integer t5, Integer t6, Integer t7, Integer t8) {
			return t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8;
		}

		@Override
		public Integer call(Integer t1, Integer t2, Integer t3, Integer t4,
				Integer t5, Integer t6, Integer t7) {
			return t1 + t2 + t3 + t4 + t5 + t6 + t7;
		}

		@Override
		public Integer call(Integer t1, Integer t2, Integer t3, Integer t4,
				Integer t5, Integer t6) {
			return t1 + t2 + t3 + t4 + t5 + t6;
		}

		@Override
		public Integer call(Integer t1, Integer t2, Integer t3, Integer t4,
				Integer t5) {
			return t1 + t2 + t3 + t4 + t5;
		}

		@Override
		public Integer call(Integer t1, Integer t2, Integer t3, Integer t4) {
			return t1 + t2 + t3 + t4;
		}

		@Override
		public Integer call(Integer t1, Integer t2, Integer t3) {
			return t1 + t2 + t3;
		}

		@Override
		public Integer call(Integer t1, Integer t2) {
			return t1 + t2;
		}
    	
    }
    Adder add = new Adder();
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

    static final class ThrowFunc<R> implements 
    Func0<R>, 
    Func1<Integer, R>, 
    Func2<Integer, Integer, R>, 
    Func3<Integer, Integer, Integer, R>, 
    Func4<Integer, Integer, Integer, Integer, R>, 
    Func5<Integer, Integer, Integer, Integer, Integer, R>, 
    Func6<Integer, Integer, Integer, Integer, Integer, Integer, R>, 
    Func7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, R>, 
    Func8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, R>, 
    Func9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, R>, 
    FuncN<R> 
    {
    	@Override
    	public R call() {
            throw new TestException("Forced failure");
    	}
    	@Override
    	public R call(Integer t1) {
    		return call();
    	}
		@Override
		public R call(Object... args) {
    		return call();
		}
		@Override
		public R call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5,
				Integer t6, Integer t7, Integer t8, Integer t9) {
    		return call();
		}
		@Override
		public R call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5,
				Integer t6, Integer t7, Integer t8) {
    		return call();
		}
		@Override
		public R call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5,
				Integer t6, Integer t7) {
    		return call();
		}
		@Override
		public R call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5,
				Integer t6) {
    		return call();
		}
		@Override
		public R call(Integer t1, Integer t2, Integer t3, Integer t4, Integer t5) {
    		return call();
		}
		@Override
		public R call(Integer t1, Integer t2, Integer t3, Integer t4) {
    		return call();
		}
		@Override
		public R call(Integer t1, Integer t2, Integer t3) {
    		return call();
		}
		@Override
		public R call(Integer t1, Integer t2) {
    		return call();
		}
    }
    ThrowFunc<Integer> throwFunc = new ThrowFunc<Integer>();

    Observable<Integer> some = Observable.just(1);

    Observable<Integer> error = Observable.error(new TestException("Forced failure"));

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = NullPointerException.class)
    public void and2ArgumentNull() {
        JoinObservable.from(some).and(null);
    }

    @Test(expected = NullPointerException.class)
    public void and3argumentNull() {
        JoinObservable.from(some).and(some).and(null);
    }

    void verifyAnd(JoinObservable<Integer> m, int count) {
    	
    	@SuppressWarnings("unchecked")
    	Observer<Integer> o = mock(Observer.class);
    	
        m.toObservable().subscribe(o);

        verify(o, never()).onError(any(Throwable.class));
        verify(o, times(1)).onNext(count);
        verify(o, times(1)).onCompleted();
    }
    void verifyError(JoinObservable<Integer> m) {
    	@SuppressWarnings("unchecked")
    	Observer<Integer> o = mock(Observer.class);
    	
        m.toObservable().subscribe(o);

        verify(o, times(1)).onError(any(TestException.class));
        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onCompleted();
    }
    
    @Test
    public void and2() {
        verifyAnd(JoinObservable.when(JoinObservable.from(some).and(some).then(add)), 2);
    }

    @Test
    public void and2Error1() {
        verifyError(JoinObservable.when(JoinObservable.from(error).and(some).then(add)));
    }

    @Test
    public void and2Error2() {
        verifyError(JoinObservable.when(JoinObservable.from(some).and(error).then(add)));
    }

    @Test
    public void and3() {
        verifyAnd(JoinObservable.when(JoinObservable.from(some).and(some).and(some).then(add)), 3);
    }

    @Test
    public void and3Error1() {
        verifyError(JoinObservable.when(JoinObservable.from(error).and(some).and(some).then(add)));
    }

    @Test
    public void and3Error2() {
        verifyError(JoinObservable.when(JoinObservable.from(some).and(error).and(some).then(add)));
    }

    @Test
    public void and3Error3() {
        verifyError(JoinObservable.when(JoinObservable.from(some).and(some).and(error).then(add)));
    }

    @Test(expected = NullPointerException.class)
    public void thenArgumentNull() {
        JoinObservable.from(some).then(null);
    }

    @Test(expected = NullPointerException.class)
    public void then2ArgumentNull() {
        JoinObservable.from(some).and(some).then(null);
    }

    @Test(expected = NullPointerException.class)
    public void then3ArgumentNull() {
        JoinObservable.from(some).and(some).and(some).then(null);
    }
    
    @Test(expected = NullPointerException.class)
    public void then4ArgumentNull() {
        JoinObservable.from(some).and(some).and(some).and(some).then(null);
    }

    @Test(expected = NullPointerException.class)
    public void then5ArgumentNull() {
        JoinObservable.from(some).and(some).and(some).and(some).and(some).then(null);
    }

    @Test(expected = NullPointerException.class)
    public void then6ArgumentNull() {
        JoinObservable.from(some).and(some).and(some).and(some).and(some).and(some).then(null);
    }

    @Test(expected = NullPointerException.class)
    public void then7ArgumentNull() {
        JoinObservable.from(some).and(some).and(some).and(some).and(some).and(some).and(some).then(null);
    }

    @Test(expected = NullPointerException.class)
    public void then8ArgumentNull() {
        JoinObservable.from(some).and(some).and(some).and(some).and(some).and(some).and(some).and(some).then(null);
    }

    @Test(expected = NullPointerException.class)
    public void then9ArgumentNull() {
        JoinObservable.from(some).and(some).and(some).and(some).and(some).and(some).and(some).and(some).and(some).then(null);
    }

    @Test
    public void thenNArgumentNull() {
    	for (int n = 10; n < 100; n++) {
    		PatternN p = JoinObservable.from(some).and(some)
    				.and(some).and(some)
    				.and(some).and(some)
    				.and(some).and(some)
    				.and(some).and(some);
    		try {
    			for (int j = 0; j < n - 10; j++) {
    				p = p.and(some);
    			}
    			p.then(null);
    			fail("Failed to throw exception with pattern length " + n);
    		} catch (NullPointerException ex) {
    			// expected, continue
    		}
    	}
    }

    @Test(expected = NullPointerException.class)
    public void then10ArgumentNull() {
        JoinObservable.from(some).and(some).and(some).and(some).and(some).and(some).and(some).and(some).and(some).and(some).then(null);
    }

    @Test
    public void then1() {
        verifyAnd(JoinObservable.when(JoinObservable.from(some).then(Functions.<Integer> identity())), 1);
    }

    @Test
    public void then1Error() {
        verifyError(JoinObservable.when(JoinObservable.from(error).then(Functions.<Integer> identity())));
    }

    @Test
    public void then1Throws() {
        verifyError(JoinObservable.when(JoinObservable.from(some).then(throwFunc)));
    }

    @Test
    public void then2Throws() {
    	verifyError(JoinObservable.when(JoinObservable.from(some).and(some).then(throwFunc)));
    }

    @Test
    public void then3Throws() {
    	verifyError(JoinObservable.when(JoinObservable.from(some).and(some).and(some).then(throwFunc)));
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
        Observable<Integer> source1 = Observable.just(1, 2, 3);
        Observable<Integer> source2 = Observable.just(4, 5, 6);

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(source1).and(source2).then(add)).toObservable();
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(1 + 4);
        verify(observer, times(1)).onNext(2 + 5);
        verify(observer, times(1)).onNext(3 + 6);
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void whenMultipleAsymSymmetric() {
        Observable<Integer> source1 = Observable.just(1, 2, 3);
        Observable<Integer> source2 = Observable.just(4, 5);

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(source1).and(source2).then(add)).toObservable();
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

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(source1).and(source2).then(add)).toObservable();
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void whenNeverNever() {
        Observable<Integer> source1 = Observable.never();
        Observable<Integer> source2 = Observable.never();

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(source1).and(source2).then(add)).toObservable();
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(any(Integer.class));
        verify(observer, never()).onCompleted();
    }

    @Test
    public void whenThrowNonEmpty() {
        Observable<Integer> source1 = Observable.empty();
        Observable<Integer> source2 = Observable.error(new TestException("Forced failure"));

        Observable<Integer> m = JoinObservable.when(JoinObservable.from(source1).and(source2).then(add)).toObservable();
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
                JoinObservable.from(xs).and(ys).then(add), // 1+4=5, 2+5=7, 3+6=9
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
    
    // -----------------

    @Test
    public void and4() {
        verifyAnd(JoinObservable.when(JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)), 4);
    }

    @Test
    public void and4Error1() {
        verifyError(JoinObservable.when(
        		JoinObservable.from(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and4Error2() {
        verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and4Error3() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and4Error4() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.then(add)));
    }

    @Test
    public void then4Throws() {
    	verifyError(JoinObservable.when(
        		JoinObservable
        		.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(throwFunc)));
    }

    // -----------------

    @Test
    public void and5() {
        verifyAnd(JoinObservable.when(JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)), 5);
    }

    @Test
    public void and5Error1() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and5Error2() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and5Error3() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and5Error4() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.then(add)));
    }
    @Test
    public void and5Error5() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.then(add)));
    }

    @Test
    public void then5Throws() {
    	verifyError(JoinObservable.when(
        		JoinObservable
        		.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(throwFunc)));
    }

    // -----------------

    @Test
    public void and6() {
        verifyAnd(JoinObservable.when(JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)), 6);
    }

    @Test
    public void and6Error1() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and6Error2() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and6Error3() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and6Error4() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.then(add)));
    }
    @Test
    public void and6Error5() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and6Error6() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.then(add)));
    }

    @Test
    public void then6Throws() {
    	verifyError(JoinObservable.when(
        		JoinObservable
        		.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(throwFunc)));
    }
    // -----------------

    @Test
    public void and7() {
        verifyAnd(JoinObservable.when(JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)), 7);
    }

    @Test
    public void and7Error1() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and7Error2() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and7Error3() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and7Error4() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }
    @Test
    public void and7Error5() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and7Error6() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and7Error7() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.then(add)));
    }

    @Test
    public void then7Throws() {
    	verifyError(JoinObservable.when(
        		JoinObservable
        		.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(throwFunc)));
    }
    // -----------------

    @Test
    public void and8() {
        verifyAnd(JoinObservable.when(JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)), 8);
    }

    @Test
    public void and8Error1() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and8Error2() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and8Error3() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and8Error4() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }
    @Test
    public void and8Error5() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and8Error6() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and8Error7() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and8Error8() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.then(add)));
    }

    @Test
    public void then8Throws() {
    	verifyError(JoinObservable.when(
        		JoinObservable
        		.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(throwFunc)));
    }
    // -----------------

    @Test
    public void and9() {
        verifyAnd(JoinObservable.when(JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)), 9);
    }

    @Test
    public void and9Error1() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and9Error2() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and9Error3() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and9Error4() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }
    @Test
    public void and9Error5() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and9Error6() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and9Error7() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void and9Error8() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.then(add)));
    }
    @Test
    public void and9Error9() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.then(add)));
    }

    @Test
    public void then9Throws() {
    	verifyError(JoinObservable.when(
        		JoinObservable
        		.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(throwFunc)));
    }
    // -----------------

    @Test
    public void andN() {
    	int s = 10;
    	for (int n = s; n < 100; n++) {
    		System.out.println("AndN(" + n + ")");
    		PatternN p = JoinObservable.from(some)
    		.and(some)
    		.and(some)
    		.and(some)
    		.and(some)
    		.and(some)
    		.and(some)
    		.and(some)
    		.and(some)
    		.and(some);
    		
    		for (int j = 0; j < n - s; j++) {
    			p = p.and(some);
    		}
    		verifyAnd(JoinObservable.when(p.then(add)), n);
    	}
    }

    @Test
    public void andNError1() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void andNError2() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void andNError3() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void andNError4() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }
    @Test
    public void andNError5() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void andNError6() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void andNError7() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void andNError8() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void andNError9() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.and(some)
        		.then(add)));
    }

    @Test
    public void andNErrorN() {
    	verifyError(JoinObservable.when(
        		JoinObservable.from(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(some)
        		.and(error)
        		.then(add)));
    }

    @Test
    public void andNErrorNRange() {
    	for (int n = 10; n < 100; n++) {
    		PatternN p = JoinObservable.from(some)
            		.and(some)
            		.and(some)
            		.and(some)
            		.and(some)
            		.and(some)
            		.and(some)
            		.and(some)
            		.and(some)
            		.and(some);
    		
    		for (int j = 0; j < n - 10; j++) {
    			p = p.and(some);
    		}
    		p = p.and(error);
    		
    		verifyError(JoinObservable.when(p.then(add)));
    	}
    }


    @Test
    public void thenNThrows() {
    	for (int n = 10; n < 100; n++) {
    		PatternN p = JoinObservable.from(some)
            		.and(some)
            		.and(some)
            		.and(some)
            		.and(some)
            		.and(some)
            		.and(some)
            		.and(some)
            		.and(some)
            		.and(some);
    		
    		for (int j = 0; j < n - 10; j++) {
    			p = p.and(some);
    		}
    		verifyError(JoinObservable.when(p.then(throwFunc)));
    	}
    }
}
