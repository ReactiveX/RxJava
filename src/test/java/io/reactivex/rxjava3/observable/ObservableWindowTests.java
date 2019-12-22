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

package io.reactivex.rxjava3.observable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class ObservableWindowTests extends RxJavaTest {

    @Test
    public void window() {
        final ArrayList<List<Integer>> lists = new ArrayList<>();

        Observable.concat(
            Observable.just(1, 2, 3, 4, 5, 6)
            .window(3)
            .map(new Function<Observable<Integer>, Observable<List<Integer>>>() {
                @Override
                public Observable<List<Integer>> apply(Observable<Integer> xs) {
                    return xs.toList().toObservable();
                }
            })
        )
        .blockingForEach(new Consumer<List<Integer>>() {
            @Override
            public void accept(List<Integer> xs) {
                lists.add(xs);
            }
        });

        assertArrayEquals(lists.get(0).toArray(new Integer[3]), new Integer[] { 1, 2, 3 });
        assertArrayEquals(lists.get(1).toArray(new Integer[3]), new Integer[] { 4, 5, 6 });
        assertEquals(2, lists.size());

    }

    @Test
    public void timeSizeWindowAlternatingBounds() {
        TestScheduler scheduler = new TestScheduler();
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<List<Integer>> to = ps.window(5, TimeUnit.SECONDS, scheduler, 2)
        .flatMapSingle(new Function<Observable<Integer>, SingleSource<List<Integer>>>() {
            @Override
            public SingleSource<List<Integer>> apply(Observable<Integer> v) throws Throwable {
                return v.toList();
            }
        })
        .test();

        ps.onNext(1);
        ps.onNext(2);
        to.assertValueCount(1); // size bound hit

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        ps.onNext(3);
        scheduler.advanceTimeBy(6, TimeUnit.SECONDS);
        to.assertValueCount(2); // time bound hit

        ps.onNext(4);
        ps.onNext(5);

        to.assertValueCount(3); // size bound hit again

        ps.onNext(4);

        scheduler.advanceTimeBy(6, TimeUnit.SECONDS);

        to.assertValueCount(4)
        .assertNoErrors()
        .assertNotComplete();

        to.dispose();
    }
}
