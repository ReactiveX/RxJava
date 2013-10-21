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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func2;

/**
 * Returns an Observable that applies a function of your choosing to the first item emitted by a
 * source Observable, then feeds the result of that function along with the second item emitted
 * by the source Observable into the same function, and so on until all items have been emitted
 * by the source Observable, and emits the final result from the final call to your function as
 * its sole item.
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduce.png">
 * <p>
 * This technique, which is called "reduce" or "aggregate" here, is sometimes called "fold,"
 * "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance,
 * has an <code>inject</code> method that does a similar operation on lists.
 * 
 * @param accumulator
 *            An accumulator function to be invoked on each item emitted by the source
 *            Observable, whose result will be used in the next accumulator call
 * @return an Observable that emits a single item that is the result of accumulating the
 *         output from the source Observable
 * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
 * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
 */
public final class OperationReduce {

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that function along with the second item emitted
     * by an Observable into the same function, and so on until all items have been emitted by the
     * source Observable, emitting the final result from the final call to your function as its sole
     * item.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduceSeed.png">
     * <p>
     * This technique, which is called "reduce" or "aggregate" here, is sometimes called "fold,"
     * "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance,
     * has an <code>inject</code> method that does a similar operation on lists.
     *
     * @param sequence
     *            An observable sequence of elements to reduce.
     * @param initialValue
     *            the initial (seed) accumulator value
     * @param accumulator
     *            an accumulator function to be invoked on each item emitted by the source
     *            Observable, the result of which will be used in the next accumulator call
     * @return an Observable that emits a single item that is the result of accumulating the output
     *         from the items emitted by the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public static <T, R> OnSubscribeFunc<R> reduce(
            Observable<? extends T> sequence, R initialValue,
            Func2<R, ? super T, R> accumulator) {
        return new Accumulator<T, R>(sequence, initialValue, accumulator);
    }

    /**
     * Returns an Observable that applies a function of your choosing to the first item emitted by a
     * source Observable, then feeds the result of that function along with the second item emitted
     * by the source Observable into the same function, and so on until all items have been emitted
     * by the source Observable, and emits the final result from the final call to your function as
     * its sole item.
     * <p>
     * <img width="640" src="https://raw.github.com/wiki/Netflix/RxJava/images/rx-operators/reduce.png">
     * <p>
     * This technique, which is called "reduce" or "aggregate" here, is sometimes called "fold,"
     * "accumulate," "compress," or "inject" in other programming contexts. Groovy, for instance,
     * has an <code>inject</code> method that does a similar operation on lists.
     * <p>
     * Note: You can't reduce an empty sequence without an initial value.
     *
     * @param sequence
     *            An observable sequence of elements to reduce.
     * @param accumulator
     *            An accumulator function to be invoked on each item emitted by the source
     *            Observable, whose result will be used in the next accumulator call
     * @return an Observable that emits a single item that is the result of accumulating the
     *         output from the source Observable
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229154(v%3Dvs.103).aspx">MSDN: Observable.Aggregate</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fold_(higher-order_function)">Wikipedia: Fold (higher-order function)</a>
     */
    public static <T> OnSubscribeFunc<T> reduce(
            Observable<? extends T> sequence, Func2<T, T, T> accumulator) {
        return new AccumulatorWithoutInitialValue<T>(sequence, accumulator);
    }

    private static class Accumulator<T, R> implements OnSubscribeFunc<R> {
        private final Observable<? extends T> sequence;
        private final R initialValue;
        private final Func2<R, ? super T, R> accumulatorFunction;

        private Accumulator(Observable<? extends T> sequence, R initialValue,
                Func2<R, ? super T, R> accumulator) {
            this.sequence = sequence;
            this.initialValue = initialValue;
            this.accumulatorFunction = accumulator;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super R> observer) {
            return sequence.subscribe(new Observer<T>() {
                private volatile R acc = initialValue;

                @Override
                public void onNext(T args) {
                    acc = accumulatorFunction.call(acc, args);
                }

                @Override
                public void onCompleted() {
                    observer.onNext(acc);
                    observer.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }
            });
        }
    }

    private static class AccumulatorWithoutInitialValue<T> implements
            OnSubscribeFunc<T> {
        private final Observable<? extends T> sequence;
        private final Func2<T, T, T> accumulatorFunction;

        private AccumulatorWithoutInitialValue(
                Observable<? extends T> sequence, Func2<T, T, T> accumulator) {
            this.sequence = sequence;
            this.accumulatorFunction = accumulator;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            return sequence.subscribe(new Observer<T>() {
                private volatile T acc = null;
                private volatile boolean isSourceSequenceEmpty = true;

                @Override
                public void onNext(T args) {
                    if (isSourceSequenceEmpty) {
                        acc = args;
                        isSourceSequenceEmpty = false;
                    } else {
                        acc = accumulatorFunction.call(acc, args);
                    }
                }

                @Override
                public void onCompleted() {
                    if (isSourceSequenceEmpty) {
                        observer.onError(new UnsupportedOperationException(
                                "Can not reduce on an empty sequence without an initial value"));
                    } else {
                        observer.onNext(acc);
                        observer.onCompleted();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    observer.onError(e);
                }
            });
        }
    }

    public static class UnitTest {

        @Test
        public void testReduceIntegersWithInitialValue() {
            Observable<Integer> observable = Observable.from(1, 2, 3);

            Observable<String> m = Observable.create(reduce(observable, "0",
                    new Func2<String, Integer, String>() {

                        @Override
                        public String call(String s, Integer n) {
                            return s + n;
                        }

                    }));

            @SuppressWarnings("unchecked")
            Observer<String> observer = mock(Observer.class);
            m.subscribe(observer);

            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext("0123");
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testReduceIntegersWithoutInitialValue() {
            Observable<Integer> observable = Observable.from(1, 2, 3);

            Observable<Integer> m = Observable.create(reduce(observable,
                    new Func2<Integer, Integer, Integer>() {

                        @Override
                        public Integer call(Integer t1, Integer t2) {
                            return t1 + t2;
                        }

                    }));

            @SuppressWarnings("unchecked")
            Observer<Integer> observer = mock(Observer.class);
            m.subscribe(observer);

            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext(6);
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testReduceIntegersWithoutInitialValueAndOnlyOneValue() {
            Observable<Integer> observable = Observable.from(1);

            Observable<Integer> m = Observable.create(reduce(observable,
                    new Func2<Integer, Integer, Integer>() {

                        @Override
                        public Integer call(Integer t1, Integer t2) {
                            return t1 + t2;
                        }

                    }));

            @SuppressWarnings("unchecked")
            Observer<Integer> observer = mock(Observer.class);
            m.subscribe(observer);

            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext(1);
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testReduceIntegersWithInitialValueAndEmpty() {
            Observable<Integer> observable = Observable.empty();

            Observable<String> m = Observable.create(reduce(observable, "1",
                    new Func2<String, Integer, String>() {

                        @Override
                        public String call(String s, Integer n) {
                            return s + n;
                        }

                    }));

            @SuppressWarnings("unchecked")
            Observer<String> observer = mock(Observer.class);
            m.subscribe(observer);

            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onNext("1");
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testReduceIntegersWithoutInitialValueAndEmpty() {
            Observable<Integer> observable = Observable.empty();

            Observable<Integer> m = Observable.create(reduce(observable,
                    new Func2<Integer, Integer, Integer>() {

                        @Override
                        public Integer call(Integer t1, Integer t2) {
                            return t1 + t2;
                        }

                    }));

            @SuppressWarnings("unchecked")
            Observer<Integer> observer = mock(Observer.class);
            m.subscribe(observer);

            InOrder inOrder = inOrder(observer);
            inOrder.verify(observer, times(1)).onError(
                    any(UnsupportedOperationException.class));
            inOrder.verifyNoMoreInteractions();
        }
    }

}
