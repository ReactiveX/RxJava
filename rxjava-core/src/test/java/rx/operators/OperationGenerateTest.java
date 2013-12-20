 /**
  * Copyright 2013 Netflix, Inc.
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

import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.MockitoAnnotations;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;
import rx.util.functions.Func1;
import static rx.Observable.create;
import static rx.operators.OperationGenerate.*;

public class OperationGenerateTest {
    @Mock
            Observer<Object> observer;
    
    Func1<Integer, Integer> increment = new Func1<Integer, Integer>() {
        @Override
        public Integer call(Integer t1) {
            return t1 + 1;
        }
    };
    Func1<Integer, Boolean> lessThan(final int value) {
        return new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer t1) {
                return t1 < value;
            }
        };
    }
    Func1<Integer, Integer> dbl = new Func1<Integer, Integer>() {
        @Override
        public Integer call(Integer t1) {
            return t1 * 2;
        }
    };
    <R> Func1<Integer, R> just(final R value) {
        return new Func1<Integer, R>() {
            @Override
            public R call(Integer t1) {
                return value;
            }
        };
    }
    <R> Func1<Integer, Long> justL(final long value) {
        return new Func1<Integer, Long>() {
            @Override
            public Long call(Integer t1) {
                return value;
            }
        };
    }
    <R> Func1<Integer, R> fail(R dummy) {
        return new Func1<Integer, R>() {
            @Override
            public R call(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };
    }
    Func1<Integer, Long> failL() {
        return new Func1<Integer, Long>() {
            @Override
            public Long call(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };
    }
    Func1<Integer, Long> fail(final long value, final int call) {
        return new Func1<Integer, Long>() {
            int i;
            @Override
            public Long call(Integer t1) {
                if (++i >= call) {
                    throw new RuntimeException("Forced failure");
                }
                return value;
            }
        };
    }
    <R> Func1<Integer, R> fail(final Func1<Integer, R> valueFactory, final int call) {
        return new Func1<Integer, R>() {
            int i;
            @Override
            public R call(Integer t1) {
                if (++i >= call) {
                    throw new RuntimeException("Forced failure");
                }
                return valueFactory.call(t1);
            }
        };
    }
    Func1<Integer, Date> delay(final int byMillis) {
        return new Func1<Integer, Date>() {
            @Override
            public Date call(Integer t1) {
                return new Date(System.currentTimeMillis() + byMillis);
            }
        };
    };
    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }
    @Test
    public void basicForLoop() {
        Observable<Integer> m = create(generate(0, lessThan(5), increment, dbl, Schedulers.immediate()));
        
        m.subscribe(observer);
        
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(4);
        verify(observer, times(1)).onNext(6);
        verify(observer, times(1)).onNext(8);
        verify(observer, times(1)).onCompleted();
        
    }
    @Test
    public void emptyForLoop() {
        Observable<Integer> m = create(generate(0, lessThan(0), increment, dbl, Schedulers.immediate()));
        
        m.subscribe(observer);
        
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, times(1)).onCompleted();
        
    }
    @Test
    public void timedForLoop() throws InterruptedException {
        
        TestScheduler scheduler = new TestScheduler();
        
        Observable<Integer> m = create(generateTimed(0, lessThan(5),
                increment, dbl, justL(50), TimeUnit.MILLISECONDS, scheduler));
        
        m.subscribe(observer);
        
        scheduler.advanceTimeBy(300, TimeUnit.MILLISECONDS);
        
        
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(4);
        verify(observer, times(1)).onNext(6);
        verify(observer, times(1)).onNext(8);
        verify(observer, times(1)).onCompleted();
        
    }
    @Test
    public void timedEmptyForLoop() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        
        Observable<Integer> m = create(generateTimed(0, lessThan(0),
                increment, dbl, justL(50), TimeUnit.MILLISECONDS, scheduler));
        
        m.subscribe(observer);
        
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, times(1)).onCompleted();
        
    }
    @Test
    public void absoluteTimedForLoop() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        
        scheduler.advanceTimeBy(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        
        Observable<Integer> m = create(generateAbsoluteTime(0, lessThan(5),
                increment, dbl, delay(50), scheduler));
        
        m.subscribe(observer);
        
        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);
        
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(4);
        verify(observer, times(1)).onNext(6);
        verify(observer, times(1)).onNext(8);
        verify(observer, times(1)).onCompleted();
        
    }
    @Test
    public void absoluteTimedEmptyForLoop() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        
        scheduler.advanceTimeBy(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        
        Observable<Integer> m = create(generateAbsoluteTime(0, lessThan(0),
                increment, dbl, delay(50), scheduler));
        
        m.subscribe(observer);
        
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, times(1)).onCompleted();
    }
    @Test
    public void conditionFails() {
        Observable<Integer> m = create(generate(0, fail(false), 
                increment, dbl, Schedulers.immediate()));
        
        m.subscribe(observer);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void iterateFails() {
        Observable<Integer> m = create(generate(0, lessThan(5), fail(0), 
                dbl, Schedulers.immediate()));
        
        m.subscribe(observer);
        
        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void resultSelectorFails() {
        Observable<Integer> m = create(generate(0, lessThan(5), 
                increment, fail(0), Schedulers.immediate()));
        
        m.subscribe(observer);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void timedConditionFails() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        
        Observable<Integer> m = create(generateTimed(0, fail(false), increment, dbl,
                justL(50), TimeUnit.MILLISECONDS, scheduler));
        
        m.subscribe(observer);
        
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void timedIterateFails() throws InterruptedException  {
        TestScheduler scheduler = new TestScheduler();
        
        Observable<Integer> m = create(generateTimed(0, lessThan(5), fail(0), dbl, justL(50), TimeUnit.MILLISECONDS, scheduler));
        
        m.subscribe(observer);
        
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        
        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void timedResultSelectorFails() throws InterruptedException  {
        TestScheduler scheduler = new TestScheduler();
        
        Observable<Integer> m = create(generateTimed(0, lessThan(5), increment, fail(0), justL(50), TimeUnit.MILLISECONDS, scheduler));
        
        m.subscribe(observer);
        
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void timedTimeSelectorFails() throws InterruptedException  {
        TestScheduler scheduler = new TestScheduler();
        
        Observable<Integer> m = create(generateTimed(0, lessThan(5), increment, dbl,
                failL(), TimeUnit.MILLISECONDS, scheduler));
        
        m.subscribe(observer);
        
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void timedTimeSelectorFails2ndCall() throws InterruptedException  {
        TestScheduler scheduler = new TestScheduler();
        
        Observable<Integer> m = create(generateTimed(0, lessThan(5), increment, dbl,
                fail(50L, 2), TimeUnit.MILLISECONDS, scheduler));
        
        m.subscribe(observer);
        
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        
        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void absoluteTimedConditionFails() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        
        scheduler.advanceTimeBy(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        
        Observable<Integer> m = create(generateAbsoluteTime(0, fail(false), increment, dbl,
                delay(50), scheduler));
        
        m.subscribe(observer);
        
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void absoluteTimedIterateFails() throws InterruptedException  {
        TestScheduler scheduler = new TestScheduler();
        
        scheduler.advanceTimeBy(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        
        Observable<Integer> m = create(generateAbsoluteTime(0, lessThan(5), fail(0), dbl,
                delay(50), scheduler));
        
        m.subscribe(observer);
        
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        
        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void absoluteTimedResultSelectorFails() throws InterruptedException  {
        TestScheduler scheduler = new TestScheduler();
        
        scheduler.advanceTimeBy(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        
        Observable<Integer> m = create(generateAbsoluteTime(0, lessThan(5), increment, fail(0),
                delay(50), scheduler));
        
        m.subscribe(observer);
        
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void absoluteTimedTimeSelectorFails() throws InterruptedException  {
        TestScheduler scheduler = new TestScheduler();
        
        scheduler.advanceTimeBy(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        
        Observable<Integer> m = create(generateAbsoluteTime(0, lessThan(5), increment, dbl,
                fail((Date)null), scheduler));
        
        m.subscribe(observer);
        
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void absoluteTimedTimeSelectorFails2ndCall() throws InterruptedException  {
        TestScheduler scheduler = new TestScheduler();
        
        scheduler.advanceTimeBy(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        
        Observable<Integer> m = create(generateAbsoluteTime(0, lessThan(5), increment, dbl,
                fail(delay(50), 2), scheduler));
        
        m.subscribe(observer);
        
        scheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS);
        
        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        
    }
}
