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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.exceptions.TestException;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;

public class OperatorMergeMapTest {
    @Test
    public void testNormal() {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        final List<Integer> list = Arrays.asList(1, 2, 3);

        Func1<Integer, List<Integer>> func = new Func1<Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer t1) {
                return list;
            }
        };
        Func2<Integer, Integer, Integer> resFunc = new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.from(source).mergeMapIterable(func, resFunc).subscribe(o);

        for (Integer s : source) {
            for (Integer v : list) {
                verify(o).onNext(s | v);
            }
        }
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testCollectionFunctionThrows() {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Func1<Integer, List<Integer>> func = new Func1<Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer t1) {
                throw new TestException();
            }
        };
        Func2<Integer, Integer, Integer> resFunc = new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.from(source).mergeMapIterable(func, resFunc).subscribe(o);

        verify(o, never()).onCompleted();
        verify(o, never()).onNext(any());
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testResultFunctionThrows() {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        final List<Integer> list = Arrays.asList(1, 2, 3);

        Func1<Integer, List<Integer>> func = new Func1<Integer, List<Integer>>() {
            @Override
            public List<Integer> call(Integer t1) {
                return list;
            }
        };
        Func2<Integer, Integer, Integer> resFunc = new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                throw new TestException();
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.from(source).mergeMapIterable(func, resFunc).subscribe(o);

        verify(o, never()).onCompleted();
        verify(o, never()).onNext(any());
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void testMergeError() {
        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        Func1<Integer, Observable<Integer>> func = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return Observable.error(new TestException());
            }
        };
        Func2<Integer, Integer, Integer> resFunc = new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1 | t2;
            }
        };

        List<Integer> source = Arrays.asList(16, 32, 64);

        Observable.from(source).mergeMap(func, resFunc).subscribe(o);

        verify(o, never()).onCompleted();
        verify(o, never()).onNext(any());
        verify(o).onError(any(TestException.class));
    }

    <T, R> Func1<T, R> just(final R value) {
        return new Func1<T, R>() {

            @Override
            public R call(T t1) {
                return value;
            }
        };
    }

    <R> Func0<R> just0(final R value) {
        return new Func0<R>() {

            @Override
            public R call() {
                return value;
            }
        };
    }

    @Test
    public void testFlatMapTransformsNormal() {
        Observable<Integer> onNext = Observable.from(Arrays.asList(1, 2, 3));
        Observable<Integer> onCompleted = Observable.from(Arrays.asList(4));
        Observable<Integer> onError = Observable.from(Arrays.asList(5));

        Observable<Integer> source = Observable.from(Arrays.asList(10, 20, 30));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.mergeMap(just(onNext), just(onError), just0(onCompleted)).subscribe(o);

        verify(o, times(3)).onNext(1);
        verify(o, times(3)).onNext(2);
        verify(o, times(3)).onNext(3);
        verify(o).onNext(4);
        verify(o).onCompleted();

        verify(o, never()).onNext(5);
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testFlatMapTransformsException() {
        Observable<Integer> onNext = Observable.from(Arrays.asList(1, 2, 3));
        Observable<Integer> onCompleted = Observable.from(Arrays.asList(4));
        Observable<Integer> onError = Observable.from(Arrays.asList(5));

        Observable<Integer> source = Observable.concat(
                Observable.from(Arrays.asList(10, 20, 30))
                , Observable.<Integer> error(new RuntimeException("Forced failure!"))
                );

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.mergeMap(just(onNext), just(onError), just0(onCompleted)).subscribe(o);

        verify(o, times(3)).onNext(1);
        verify(o, times(3)).onNext(2);
        verify(o, times(3)).onNext(3);
        verify(o).onNext(5);
        verify(o).onCompleted();
        verify(o, never()).onNext(4);

        verify(o, never()).onError(any(Throwable.class));
    }

    <R> Func0<R> funcThrow0(R r) {
        return new Func0<R>() {
            @Override
            public R call() {
                throw new TestException();
            }
        };
    }

    <T, R> Func1<T, R> funcThrow(T t, R r) {
        return new Func1<T, R>() {
            @Override
            public R call(T t) {
                throw new TestException();
            }
        };
    }

    @Test
    public void testFlatMapTransformsOnNextFuncThrows() {
        Observable<Integer> onCompleted = Observable.from(Arrays.asList(4));
        Observable<Integer> onError = Observable.from(Arrays.asList(5));

        Observable<Integer> source = Observable.from(Arrays.asList(10, 20, 30));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.mergeMap(funcThrow(1, onError), just(onError), just0(onCompleted)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
    }

    @Test
    public void testFlatMapTransformsOnErrorFuncThrows() {
        Observable<Integer> onNext = Observable.from(Arrays.asList(1, 2, 3));
        Observable<Integer> onCompleted = Observable.from(Arrays.asList(4));
        Observable<Integer> onError = Observable.from(Arrays.asList(5));

        Observable<Integer> source = Observable.error(new TestException());

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.mergeMap(just(onNext), funcThrow((Throwable) null, onError), just0(onCompleted)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
    }

    @Test
    public void testFlatMapTransformsOnCompletedFuncThrows() {
        Observable<Integer> onNext = Observable.from(Arrays.asList(1, 2, 3));
        Observable<Integer> onCompleted = Observable.from(Arrays.asList(4));
        Observable<Integer> onError = Observable.from(Arrays.asList(5));

        Observable<Integer> source = Observable.from(Arrays.<Integer> asList());

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.mergeMap(just(onNext), just(onError), funcThrow0(onCompleted)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
    }

    @Test
    public void testFlatMapTransformsMergeException() {
        Observable<Integer> onNext = Observable.error(new TestException());
        Observable<Integer> onCompleted = Observable.from(Arrays.asList(4));
        Observable<Integer> onError = Observable.from(Arrays.asList(5));

        Observable<Integer> source = Observable.from(Arrays.asList(10, 20, 30));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.mergeMap(just(onNext), just(onError), funcThrow0(onCompleted)).subscribe(o);

        verify(o).onError(any(TestException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
    }
}
