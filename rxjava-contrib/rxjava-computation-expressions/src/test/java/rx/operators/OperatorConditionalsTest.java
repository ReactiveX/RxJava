/**
 * Copyright 2014 Netflix, Inc.
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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observer;
import rx.Statement;
import rx.Subscription;
import rx.functions.Func0;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

public class OperatorConditionalsTest {
    @Mock
    Observer<Object> observer;
    TestScheduler scheduler;
    Func0<Integer> func;
    Func0<Integer> funcError;
    Func0<Boolean> condition;
    Func0<Boolean> conditionError;
    int numRecursion = 250;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        scheduler = new TestScheduler();
        func = new Func0<Integer>() {
            int count = 1;

            @Override
            public Integer call() {
                return count++;
            }
        };
        funcError = new Func0<Integer>() {
            int count = 1;

            @Override
            public Integer call() {
                if (count == 2) {
                    throw new RuntimeException("Forced failure!");
                }
                return count++;
            }
        };
        condition = new Func0<Boolean>() {
            boolean r;

            @Override
            public Boolean call() {
                r = !r;
                return r;
            }

        };
        conditionError = new Func0<Boolean>() {
            boolean r;

            @Override
            public Boolean call() {
                r = !r;
                if (!r) {
                    throw new RuntimeException("Forced failure!");
                }
                return r;
            }

        };
    }

    <T> Func0<T> just(final T value) {
        return new Func0<T>() {
            @Override
            public T call() {
                return value;
            }
        };
    }

    @SuppressWarnings("unchecked")
    <T> void observe(Observable<? extends T> source, T... values) {
        Observer<T> o = mock(Observer.class);

        Subscription s = source.subscribe(new TestObserver<T>(o));

        InOrder inOrder = inOrder(o);

        for (T v : values) {
            inOrder.verify(o, times(1)).onNext(v);
        }
        inOrder.verify(o, times(1)).onCompleted();
        verify(o, never()).onError(any(Throwable.class));

        s.unsubscribe();

        inOrder.verifyNoMoreInteractions();
    }

    @SuppressWarnings("unchecked")
    <T> void observeSequence(Observable<? extends T> source, Iterable<? extends T> values) {
        Observer<T> o = mock(Observer.class);

        TestObserver<T> testObserver = new TestObserver<T>(o);
        Subscription s = source.subscribe(testObserver);

        InOrder inOrder = inOrder(o);

        for (T v : values) {
            inOrder.verify(o, times(1)).onNext(v);
        }
        inOrder.verify(o, times(1)).onCompleted();
        verify(o, never()).onError(any(Throwable.class));

        s.unsubscribe();

        inOrder.verifyNoMoreInteractions();
    }

    @SuppressWarnings("unchecked")
    <T> void observeError(Observable<? extends T> source, Class<? extends Throwable> error, T... valuesBeforeError) {
        Observer<T> o = mock(Observer.class);

        Subscription s = source.subscribe(new TestObserver<T>(o));

        InOrder inOrder = inOrder(o);

        for (T v : valuesBeforeError) {
            inOrder.verify(o, times(1)).onNext(v);
        }
        inOrder.verify(o, times(1)).onError(any(error));
        verify(o, never()).onCompleted();

        s.unsubscribe();

        inOrder.verifyNoMoreInteractions();
    }

    @SuppressWarnings("unchecked")
    <T> void observeSequenceError(Observable<? extends T> source, Class<? extends Throwable> error, Iterable<? extends T> valuesBeforeError) {
        Observer<T> o = mock(Observer.class);

        Subscription s = source.subscribe(new TestObserver<T>(o));

        InOrder inOrder = inOrder(o);

        for (T v : valuesBeforeError) {
            inOrder.verify(o, times(1)).onNext(v);
        }
        inOrder.verify(o, times(1)).onError(any(error));
        verify(o, never()).onCompleted();

        s.unsubscribe();

        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSimple() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);
        Observable<Integer> source2 = Observable.from(4, 5, 6);

        Map<Integer, Observable<Integer>> map = new HashMap<Integer, Observable<Integer>>();
        map.put(1, source1);
        map.put(2, source2);

        Observable<Integer> result = Statement.switchCase(func, map);

        observe(result, 1, 2, 3);
        observe(result, 4, 5, 6);
        observe(result);
    }

    @Test
    public void testDefaultCase() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);
        Observable<Integer> source2 = Observable.from(4, 5, 6);

        Map<Integer, Observable<Integer>> map = new HashMap<Integer, Observable<Integer>>();
        map.put(1, source1);

        Observable<Integer> result = Statement.switchCase(func, map, source2);

        observe(result, 1, 2, 3);
        observe(result, 4, 5, 6);
        observe(result, 4, 5, 6);
    }

    @Test
    public void testCaseSelectorThrows() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);

        Map<Integer, Observable<Integer>> map = new HashMap<Integer, Observable<Integer>>();
        map.put(1, source1);

        Observable<Integer> result = Statement.switchCase(funcError, map);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testMapGetThrows() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);
        Observable<Integer> source2 = Observable.from(4, 5, 6);

        Map<Integer, Observable<Integer>> map = new HashMap<Integer, Observable<Integer>>() {
            private static final long serialVersionUID = -4342868139960216388L;

            @Override
            public Observable<Integer> get(Object key) {
                if (key.equals(2)) {
                    throw new RuntimeException("Forced failure!");
                }
                return super.get(key);
            }

        };
        map.put(1, source1);
        map.put(2, source2);

        Observable<Integer> result = Statement.switchCase(func, map);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testMapContainsKeyThrows() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);

        Map<Integer, Observable<Integer>> map = new HashMap<Integer, Observable<Integer>>() {
            private static final long serialVersionUID = 1975411728567003983L;

            @Override
            public boolean containsKey(Object key) {
                if (key.equals(2)) {
                    throw new RuntimeException("Forced failure!");
                }
                return super.containsKey(key);
            }

        };
        map.put(1, source1);

        Observable<Integer> result = Statement.switchCase(func, map);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testChosenObservableThrows() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);
        Observable<Integer> source2 = Observable.error(new RuntimeException("Forced failure"));

        Map<Integer, Observable<Integer>> map = new HashMap<Integer, Observable<Integer>>();
        map.put(1, source1);
        map.put(2, source2);

        Observable<Integer> result = Statement.switchCase(func, map);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testIfThen() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);

        Observable<Integer> result = Statement.ifThen(condition, source1);

        observe(result, 1, 2, 3);
        observe(result);
        observe(result, 1, 2, 3);
        observe(result);
    }

    @Test
    public void testIfThenElse() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);
        Observable<Integer> source2 = Observable.from(4, 5, 6);

        Observable<Integer> result = Statement.ifThen(condition, source1, source2);

        observe(result, 1, 2, 3);
        observe(result, 4, 5, 6);
        observe(result, 1, 2, 3);
        observe(result, 4, 5, 6);
    }

    @Test
    public void testIfThenConditonThrows() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);

        Observable<Integer> result = Statement.ifThen(conditionError, source1);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testIfThenObservableThrows() {
        Observable<Integer> source1 = Observable.error(new RuntimeException("Forced failure!"));

        Observable<Integer> result = Statement.ifThen(condition, source1);

        observeError(result, RuntimeException.class);
        observe(result);

        observeError(result, RuntimeException.class);
        observe(result);
    }

    @Test
    public void testIfThenElseObservableThrows() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);
        Observable<Integer> source2 = Observable.error(new RuntimeException("Forced failure!"));

        Observable<Integer> result = Statement.ifThen(condition, source1, source2);

        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
        observe(result, 1, 2, 3);
        observeError(result, RuntimeException.class);
    }

    @Test
    public void testDoWhile() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);

        Observable<Integer> result = Statement.doWhile(source1, condition);

        observe(result, 1, 2, 3, 1, 2, 3);
    }

    @Test
    public void testDoWhileOnce() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);

        condition.call(); // toggle to false
        Observable<Integer> result = Statement.doWhile(source1, condition);

        observe(result, 1, 2, 3);
    }

    @Test
    public void testDoWhileConditionThrows() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);
        Observable<Integer> result = Statement.doWhile(source1, conditionError);

        observeError(result, RuntimeException.class, 1, 2, 3);
    }

    @Test
    public void testDoWhileSourceThrows() {
        Observable<Integer> source1 = Observable.concat(Observable.from(1, 2, 3),
                Observable.<Integer> error(new RuntimeException("Forced failure!")));

        Observable<Integer> result = Statement.doWhile(source1, condition);

        observeError(result, RuntimeException.class, 1, 2, 3);
    }

    Func0<Boolean> countdown(final int n) {
        return new Func0<Boolean>() {
            int count = n;

            @Override
            public Boolean call() {
                return count-- > 0;
            }
        };
    }

    @Test
    public void testDoWhileManyTimes() {
        Observable<Integer> source1 = Observable.from(1, 2, 3).subscribeOn(Schedulers.trampoline());

        List<Integer> expected = new ArrayList<Integer>(numRecursion * 3);
        for (int i = 0; i < numRecursion; i++) {
            expected.add(1);
            expected.add(2);
            expected.add(3);
        }

        Observable<Integer> result = Statement.doWhile(source1, countdown(numRecursion));

        observeSequence(result, expected);
    }

    @Test
    public void testWhileDo() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);
        Observable<Integer> result = Statement.whileDo(source1, countdown(2));

        observe(result, 1, 2, 3, 1, 2, 3);
    }

    @Test
    public void testWhileDoOnce() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);
        Observable<Integer> result = Statement.whileDo(source1, countdown(1));

        observe(result, 1, 2, 3);
    }

    @Test
    public void testWhileDoZeroTimes() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);
        Observable<Integer> result = Statement.whileDo(source1, countdown(0));

        observe(result);
    }

    @Test
    public void testWhileDoManyTimes() {
        Observable<Integer> source1 = Observable.from(1, 2, 3).subscribeOn(Schedulers.trampoline());

        List<Integer> expected = new ArrayList<Integer>(numRecursion * 3);
        for (int i = 0; i < numRecursion; i++) {
            expected.add(1);
            expected.add(2);
            expected.add(3);
        }

        Observable<Integer> result = Statement.whileDo(source1, countdown(numRecursion));

        observeSequence(result, expected);
    }

    @Test
    public void testWhileDoConditionThrows() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);
        Observable<Integer> result = Statement.whileDo(source1, conditionError);

        observeError(result, RuntimeException.class, 1, 2, 3);
    }

    @Test
    public void testWhileDoConditionThrowsImmediately() {
        Observable<Integer> source1 = Observable.from(1, 2, 3);
        conditionError.call();
        Observable<Integer> result = Statement.whileDo(source1, conditionError);

        observeError(result, RuntimeException.class);
    }

    @Test
    public void testWhileDoSourceThrows() {
        Observable<Integer> source1 = Observable.concat(Observable.from(1, 2, 3),
                Observable.<Integer> error(new RuntimeException("Forced failure!")));

        Observable<Integer> result = Statement.whileDo(source1, condition);

        observeError(result, RuntimeException.class, 1, 2, 3);
    }
}
