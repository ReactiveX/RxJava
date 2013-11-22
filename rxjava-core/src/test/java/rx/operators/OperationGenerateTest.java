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
import rx.concurrency.Schedulers;
import rx.util.TimeSpan;
import rx.util.functions.Func1;

/**
 *
 */
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
    <R> Func1<Integer, R> fail(R dummy) {
        return new Func1<Integer, R>() {
            @Override
            public R call(Integer t1) {
                throw new RuntimeException("Forced failure");
            }
        };
    }
    <R> Func1<Integer, R> fail(final R value, final int call) {
        return new Func1<Integer, R>() {
            int i;
            @Override
            public R call(Integer t1) {
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
        Observable<Integer> m = Observable.generate(0, lessThan(5), increment, dbl, Schedulers.immediate());
        
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
        Observable<Integer> m = Observable.generate(0, lessThan(0), increment, dbl, Schedulers.immediate());
        
        m.subscribe(observer);

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, times(1)).onCompleted();

    }
    @Test
    public void timedForLoop() throws InterruptedException {
        Observable<Integer> m = Observable.generate(0, lessThan(5), 
                increment, dbl, just(TimeSpan.of(50, TimeUnit.MILLISECONDS)));
        
        m.subscribe(observer);
        
        Thread.sleep(600); // FIXME Shaky

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
        Observable<Integer> m = Observable.generate(0, lessThan(0), 
                increment, dbl, just(TimeSpan.of(50, TimeUnit.MILLISECONDS)));
        
        m.subscribe(observer);
        
        Thread.sleep(200); // FIXME Shaky

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, times(1)).onCompleted();
        
    }
    @Test
    public void absoluteTimedForLoop() throws InterruptedException {
        Observable<Integer> m = Observable.generateAbsoluteTime(0, lessThan(5), 
                increment, dbl, delay(50));
        
        m.subscribe(observer);
        
        Thread.sleep(500); // FIXME Shaky

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
        Observable<Integer> m = Observable.generateAbsoluteTime(0, lessThan(0), 
                increment, dbl, delay(50));
        
        m.subscribe(observer);
        
        Thread.sleep(100); // FIXME Shaky

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, times(1)).onCompleted();
    }
    @Test
    public void conditionFails() {
        Observable<Integer> m = Observable.generate(0, fail(false), increment, dbl, Schedulers.immediate());
        
        m.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void iterateFails() {
        Observable<Integer> m = Observable.generate(0, lessThan(5), fail(0), dbl, Schedulers.immediate());
        
        m.subscribe(observer);

        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void resultSelectorFails() {
        Observable<Integer> m = Observable.generate(0, lessThan(5), increment, fail(0), Schedulers.immediate());
        
        m.subscribe(observer);

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void timedConditionFails() throws InterruptedException {
        Observable<Integer> m = Observable.generate(0, fail(false), increment, dbl, 
                just(TimeSpan.of(50, TimeUnit.MILLISECONDS)));
        
        m.subscribe(observer);

        Thread.sleep(200); // FIXME Shaky

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void timedIterateFails() throws InterruptedException  {
        Observable<Integer> m = Observable.generate(0, lessThan(5), fail(0), dbl, just(TimeSpan.of(50, TimeUnit.MILLISECONDS)));
        
        m.subscribe(observer);

        Thread.sleep(200); // FIXME Shaky

        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void timedResultSelectorFails() throws InterruptedException  {
        Observable<Integer> m = Observable.generate(0, lessThan(5), increment, fail(0), just(TimeSpan.of(50, TimeUnit.MILLISECONDS)));
        
        m.subscribe(observer);

        Thread.sleep(200); // FIXME Shaky

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void timedTimeSelectorFails() throws InterruptedException  {
        Observable<Integer> m = Observable.generate(0, lessThan(5), increment, dbl, 
                fail((TimeSpan)null));
        
        m.subscribe(observer);

        Thread.sleep(200); // FIXME Shaky

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void timedTimeSelectorFails2ndCall() throws InterruptedException  {
        Observable<Integer> m = Observable.generate(0, lessThan(5), increment, dbl, 
                fail(TimeSpan.of(50, TimeUnit.MILLISECONDS), 2));
        
        m.subscribe(observer);

        Thread.sleep(200); // FIXME Shaky

        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void absoluteTimedConditionFails() throws InterruptedException {
        Observable<Integer> m = Observable.generateAbsoluteTime(0, fail(false), increment, dbl, 
                delay(50));
        
        m.subscribe(observer);

        Thread.sleep(200); // FIXME Shaky

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void absoluteTimedIterateFails() throws InterruptedException  {
        Observable<Integer> m = Observable.generateAbsoluteTime(0, lessThan(5), fail(0), dbl, 
                delay(50));
        
        m.subscribe(observer);

        Thread.sleep(200); // FIXME Shaky

        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void absoluteTimedResultSelectorFails() throws InterruptedException  {
        Observable<Integer> m = Observable.generateAbsoluteTime(0, lessThan(5), increment, fail(0), 
                delay(50));
        
        m.subscribe(observer);

        Thread.sleep(200); // FIXME Shaky

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void absoluteTimedTimeSelectorFails() throws InterruptedException  {
        Observable<Integer> m = Observable.generateAbsoluteTime(0, lessThan(5), increment, dbl, 
                fail((Date)null));
        
        m.subscribe(observer);

        Thread.sleep(200); // FIXME Shaky

        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
        
    }
    @Test
    public void absoluteTimedTimeSelectorFails2ndCall() throws InterruptedException  {
        Observable<Integer> m = Observable.generateAbsoluteTime(0, lessThan(5), increment, dbl, 
                fail(delay(50), 2));
        
        m.subscribe(observer);

        Thread.sleep(200); // FIXME Shaky

        verify(observer, times(1)).onNext(0);
        verify(observer, times(1)).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
        
    }
}
