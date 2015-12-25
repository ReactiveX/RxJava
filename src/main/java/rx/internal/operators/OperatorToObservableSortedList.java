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
package rx.internal.operators;

import java.util.*;

import rx.Observable.Operator;
import rx.exceptions.Exceptions;
import rx.*;
import rx.functions.Func2;
import rx.internal.producers.SingleDelayedProducer;

/**
 * Return an {@code Observable} that emits the items emitted by the source {@code Observable}, in a sorted order
 * (each item emitted by the {@code Observable} must implement {@link Comparable} with respect to all other
 * items in the sequence, or you must pass in a sort function).
 * <p>
 * <img width="640" height="310" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/toSortedList.png" alt="">
 * 
 * @param <T>
 *          the type of the items emitted by the source and the resulting {@code Observable}s
 */
public final class OperatorToObservableSortedList<T> implements Operator<List<T>, T> {
    final Comparator<? super T> sortFunction;
    final int initialCapacity;

    @SuppressWarnings("unchecked")
    public OperatorToObservableSortedList(int initialCapacity) {
        this.sortFunction = DEFAULT_SORT_FUNCTION;
        this.initialCapacity = initialCapacity;
    }

    public OperatorToObservableSortedList(final Func2<? super T, ? super T, Integer> sortFunction, int initialCapacity) {
        this.initialCapacity = initialCapacity;
        this.sortFunction = new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return sortFunction.call(o1, o2);
            }
        };
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super List<T>> child) {
        final SingleDelayedProducer<List<T>> producer = new SingleDelayedProducer<List<T>>(child);
        Subscriber<T> result = new Subscriber<T>() {

            List<T> list = new ArrayList<T>(initialCapacity);
            boolean completed;
            
            @Override
            public void onStart() {
                request(Long.MAX_VALUE);
            }

            @Override
            public void onCompleted() {
                if (!completed) {
                    completed = true;
                    List<T> a = list;
                    list = null;
                    try {
                        // sort the list before delivery
                        Collections.sort(a, sortFunction);
                    } catch (Throwable e) {
                        Exceptions.throwOrReport(e, this);
                        return;
                    }
                    producer.setValue(a);
                }
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(T value) {
                if (!completed) {
                    list.add(value);
                }
            }

        };
        child.add(result);
        child.setProducer(producer);
        return result;
    }
    // raw because we want to support Object for this default
    @SuppressWarnings("rawtypes")
    private static Comparator DEFAULT_SORT_FUNCTION = new DefaultComparableFunction();

    private static class DefaultComparableFunction implements Comparator<Object> {
        DefaultComparableFunction() {
        }

        // unchecked because we want to support Object for this default
        @SuppressWarnings("unchecked")
        @Override
        public int compare(Object t1, Object t2) {
            Comparable<Object> c1 = (Comparable<Object>) t1;
            Comparable<Object> c2 = (Comparable<Object>) t2;
            return c1.compareTo(c2);
        }

    }
}
