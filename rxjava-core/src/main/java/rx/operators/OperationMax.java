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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import rx.Observable;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * Returns the maximum element in an observable sequence.
 */
public class OperationMax {

    public static <T extends Comparable<T>> Observable<T> max(
            Observable<T> source) {
        return source.reduce(new Func2<T, T, T>() {
            @Override
            public T call(T acc, T value) {
                if (acc.compareTo(value) > 0) {
                    return acc;
                }
                return value;
            }
        });
    }

    public static <T> Observable<T> max(Observable<T> source,
            final Comparator<T> comparator) {
        return source.reduce(new Func2<T, T, T>() {
            @Override
            public T call(T acc, T value) {
                if (comparator.compare(acc, value) > 0) {
                    return acc;
                }
                return value;
            }
        });
    }

    public static <T, R extends Comparable<R>> Observable<List<T>> maxBy(
            Observable<T> source, final Func1<T, R> selector) {
        return source.reduce(new ArrayList<T>(),
                new Func2<List<T>, T, List<T>>() {

                    @Override
                    public List<T> call(List<T> acc, T value) {
                        if (acc.isEmpty()) {
                            acc.add(value);
                        } else {
                            int flag = selector.call(acc.get(0)).compareTo(
                                    selector.call(value));
                            if (flag < 0) {
                                acc.clear();
                                acc.add(value);
                            } else if (flag == 0) {
                                acc.add(value);
                            }
                        }
                        return acc;
                    }
                });
    }

    public static <T, R> Observable<List<T>> maxBy(Observable<T> source,
            final Func1<T, R> selector, final Comparator<R> comparator) {
        return source.reduce(new ArrayList<T>(),
                new Func2<List<T>, T, List<T>>() {

                    @Override
                    public List<T> call(List<T> acc, T value) {
                        if (acc.isEmpty()) {
                            acc.add(value);
                        } else {
                            int flag = comparator.compare(
                                    selector.call(acc.get(0)),
                                    selector.call(value));
                            if (flag < 0) {
                                acc.clear();
                                acc.add(value);
                            } else if (flag == 0) {
                                acc.add(value);
                            }
                        }
                        return acc;
                    }
                });
    }

}
