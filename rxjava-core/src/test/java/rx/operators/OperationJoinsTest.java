/**
 * Copyright 2013 Netflix, Inc.
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
package rx.operators;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import static org.mockito.Matchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;
import rx.Observable;
import rx.Observer;
import rx.joins.Plan0;
import rx.subjects.PublishSubject;
import rx.util.functions.Func1;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Functions;

public class OperationJoinsTest {
    @Mock
    Observer<Object> observer;
    
    
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
        some.and(null);
    }
    @Test(expected = NullPointerException.class)
    public void and3argumentNull() {
        Observable<Integer> some = Observable.just(1);
        some.and(some).and(null);
    }
    @Test
    public void and2() {
        Observable<Integer> some = Observable.just(1);
        
        Observable<Integer> m = Observable.when(some.and(some).then(add2));
        
        m.subscribe(observer);
        
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onCompleted();
    }
    @Test
    public void and2Error1() {
        Observable<Integer> error = Observable.error(new RuntimeException("Forced failure"));
        
        Observable<Integer> some = Observable.just(1);
        
        Observable<Integer> m = Observable.when(error.and(some).then(add2));
        
        m.subscribe(observer);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
    }
    @Test
    public void and2Error2() {
        Observable<Integer> error = Observable.error(new RuntimeException("Forced failure"));
        
        Observable<Integer> some = Observable.just(1);
        
        Observable<Integer> m = Observable.when(some.and(error).then(add2));
        
        m.subscribe(observer);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
    }
    @Test
    public void and3() {
        Observable<Integer> some = Observable.just(1);
        
        Observable<Integer> m = Observable.when(some.and(some).and(some).then(add3));
        
        m.subscribe(observer);
        
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onCompleted();
    }
    @Test
    public void and3Error1() {
        Observable<Integer> error = Observable.error(new RuntimeException("Forced failure"));
        
        Observable<Integer> some = Observable.just(1);
        
        Observable<Integer> m = Observable.when(error.and(some).and(some).then(add3));
        
        m.subscribe(observer);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
    }
    @Test
    public void and3Error2() {
        Observable<Integer> error = Observable.error(new RuntimeException("Forced failure"));
        
        Observable<Integer> some = Observable.just(1);
        
        Observable<Integer> m = Observable.when(some.and(error).and(some).then(add3));
        
        m.subscribe(observer);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
    }
    @Test
    public void and3Error3() {
        Observable<Integer> error = Observable.error(new RuntimeException("Forced failure"));
        
        Observable<Integer> some = Observable.just(1);
        
        Observable<Integer> m = Observable.when(some.and(some).and(error).then(add3));
        
        m.subscribe(observer);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
    }
    @Test(expected = NullPointerException.class)
    public void thenArgumentNull() {
        Observable<Integer> some = Observable.just(1);
        
        some.then(null);
    }
    @Test(expected = NullPointerException.class)
    public void then2ArgumentNull() {
        Observable<Integer> some = Observable.just(1);
        
        some.and(some).then(null);
    }
    @Test(expected = NullPointerException.class)
    public void then3ArgumentNull() {
        Observable<Integer> some = Observable.just(1);
        
        some.and(some).and(some).then(null);
    }
    @Test
    public void then1() {
        Observable<Integer> some = Observable.just(1);
        
        Observable<Integer> m = Observable.when(some.then(Functions.<Integer>identity()));
        m.subscribe(observer);
        
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onCompleted();
    }
    @Test
    public void then1Error() {
        Observable<Integer> some = Observable.error(new RuntimeException("Forced failure"));
        
        Observable<Integer> m = Observable.when(some.then(Functions.<Integer>identity()));
        m.subscribe(observer);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
    }
    @Test
    public void then1Throws() {
        Observable<Integer> some = Observable.just(1);
        
        Observable<Integer> m = Observable.when(some.then(func1Throw));
        m.subscribe(observer);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
    }
    @Test
    public void then2Throws() {
        Observable<Integer> some = Observable.just(1);
        
        Observable<Integer> m = Observable.when(some.and(some).then(func2Throw));
        m.subscribe(observer);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
    }
    @Test
    public void then3Throws() {
        Observable<Integer> some = Observable.just(1);
        
        Observable<Integer> m = Observable.when(some.and(some).and(some).then(func3Throw));
        m.subscribe(observer);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
    }
    @Test(expected = NullPointerException.class)
    public void whenArgumentNull1() {
        Observable.when((Plan0<Object>[])null);
    }
    @Test(expected = NullPointerException.class)
    public void whenArgumentNull2() {
        Observable.when((Iterable<Plan0<Object>>)null);
    }
    @Test
    public void whenMultipleSymmetric() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);
        Observable<Integer> source2 = Observable.from(4, 5, 6);
        
        Observable<Integer> m = Observable.when(source1.and(source2).then(add2));
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
        
        Observable<Integer> m = Observable.when(source1.and(source2).then(add2));
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
        
        Observable<Integer> m = Observable.when(source1.and(source2).then(add2));
        m.subscribe(observer);
        
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, times(1)).onCompleted();        
    }
    
    @Test
    public void whenNeverNever() {
        Observable<Integer> source1 = Observable.never();
        Observable<Integer> source2 = Observable.never();
        
        Observable<Integer> m = Observable.when(source1.and(source2).then(add2));
        m.subscribe(observer);
        
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();        
    }
    @Test
    public void whenThrowNonEmpty() {
        Observable<Integer> source1 = Observable.empty();
        Observable<Integer> source2 = Observable.error(new RuntimeException("Forced failure"));
        
        Observable<Integer> m = Observable.when(source1.and(source2).then(add2));
        m.subscribe(observer);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();        
    }
    @Test
    public void whenComplicated() {
        PublishSubject<Integer> xs = PublishSubject.create();
        PublishSubject<Integer> ys = PublishSubject.create();
        PublishSubject<Integer> zs = PublishSubject.create();
        
        Observable<Integer> m = Observable.when(
            xs.and(ys).then(add2),
            xs.and(zs).then(mul2),
            ys.and(zs).then(sub2)
        );
        m.subscribe(observer);
        
        xs.onNext(1); // t == 210
        
        xs.onNext(2); // t == 220
        zs.onNext(7); // t == 220
        
        xs.onNext(3); // t == 230
        zs.onNext(8); // t == 230

        ys.onNext(4); // t == 240
        zs.onNext(9); // t == 240
        xs.onCompleted(); // t == 240

        ys.onNext(5); // t == 250
        
        ys.onNext(6); // t == 260
        
        ys.onCompleted(); // t == 270
        
        zs.onCompleted(); // t == 300
        
        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1 * 7);
        inOrder.verify(observer, times(1)).onNext(2 * 8);
        inOrder.verify(observer, times(1)).onNext(3 + 4);
        inOrder.verify(observer, times(1)).onNext(5 - 9);
        inOrder.verify(observer, times(1)).onCompleted();         
        verify(observer, never()).onError(any(Throwable.class));
    }
}
