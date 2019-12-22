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

package io.reactivex.rxjava3.flowable;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class FlowableWindowTests extends RxJavaTest {

    @Test
    public void window() {
        final ArrayList<List<Integer>> lists = new ArrayList<>();

        Flowable.concat(
            Flowable.just(1, 2, 3, 4, 5, 6)
            .window(3)
            .map(new Function<Flowable<Integer>, Flowable<List<Integer>>>() {
                @Override
                public Flowable<List<Integer>> apply(Flowable<Integer> xs) {
                    return xs.toList().toFlowable();
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
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<List<Integer>> ts = pp.window(5, TimeUnit.SECONDS, scheduler, 2)
        .flatMapSingle(new Function<Flowable<Integer>, SingleSource<List<Integer>>>() {
            @Override
            public SingleSource<List<Integer>> apply(Flowable<Integer> v) throws Throwable {
                return v.toList();
            }
        })
        .test();

        pp.onNext(1);
        pp.onNext(2);
        ts.assertValueCount(1); // size bound hit

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);
        pp.onNext(3);
        scheduler.advanceTimeBy(6, TimeUnit.SECONDS);
        ts.assertValueCount(2); // time bound hit

        pp.onNext(4);
        pp.onNext(5);

        ts.assertValueCount(3); // size bound hit again

        pp.onNext(4);

        scheduler.advanceTimeBy(6, TimeUnit.SECONDS);

        ts.assertValueCount(4)
        .assertNoErrors()
        .assertNotComplete();

        ts.cancel();
    }
}
