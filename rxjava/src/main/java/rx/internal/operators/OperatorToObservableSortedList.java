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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func2;

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
    private final Func2<? super T, ? super T, Integer> sortFunction;

    @SuppressWarnings("unchecked")
    public OperatorToObservableSortedList() {
        this.sortFunction = defaultSortFunction;
    }

    public OperatorToObservableSortedList(Func2<? super T, ? super T, Integer> sortFunction) {
        this.sortFunction = sortFunction;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super List<T>> o) {
        return new Subscriber<T>(o) {

            final List<T> list = new ArrayList<T>();

            @Override
            public void onStart() {
                request(Long.MAX_VALUE);
            }

            @Override
            public void onCompleted() {
                try {

                    // sort the list before delivery
                    Collections.sort(list, new Comparator<T>() {

                        @Override
                        public int compare(T o1, T o2) {
                            return sortFunction.call(o1, o2);
                        }

                    });

                    o.onNext(Collections.unmodifiableList(list));
                    o.onCompleted();
                } catch (Throwable e) {
                    onError(e);
                }
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onNext(T value) {
                list.add(value);
            }

        };
    }

    // raw because we want to support Object for this default
    @SuppressWarnings("rawtypes")
    private static Func2 defaultSortFunction = new DefaultComparableFunction();

    private static class DefaultComparableFunction implements Func2<Object, Object, Integer> {

        // unchecked because we want to support Object for this default
        @SuppressWarnings("unchecked")
        @Override
        public Integer call(Object t1, Object t2) {
            Comparable<Object> c1 = (Comparable<Object>) t1;
            Comparable<Object> c2 = (Comparable<Object>) t2;
            return c1.compareTo(c2);
        }

    }
}
