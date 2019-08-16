/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableFlattenIterableTest extends RxJavaTest {

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().flatMapIterable(new Function<Object, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Object v) throws Exception {
                return Arrays.asList(10, 20);
            }
        }));
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceObservable(new Function<Observable<Integer>, Object>() {
            @Override
            public Object apply(Observable<Integer> o) throws Exception {
                return o.flatMapIterable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        return Arrays.asList(10, 20);
                    }
                });
            }
        }, false, 1, 1, 10, 20);
    }

    @Test
    public void failingInnerCancelsSource() {
        final AtomicInteger counter = new AtomicInteger();
        Observable.range(1, 5)
        .doOnNext(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                counter.getAndIncrement();
            }
        })
        .flatMapIterable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v)
                    throws Exception {
                return new Iterable<Integer>() {
                    @Override
                    public Iterator<Integer> iterator() {
                        return new Iterator<Integer>() {
                            @Override
                            public boolean hasNext() {
                                return true;
                            }

                            @Override
                            public Integer next() {
                                throw new TestException();
                            }

                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, counter.get());
    }
}
