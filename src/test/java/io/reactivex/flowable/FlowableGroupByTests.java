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

package io.reactivex.flowable;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.reactivex.flowable.FlowableEventStream.Event;
import io.reactivex.flowables.GroupedFlowable;
import io.reactivex.functions.*;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableGroupByTests {

    @Test
    public void testTakeUnsubscribesOnGroupBy() {
        Flowable.merge(
            FlowableEventStream.getEventStream("HTTP-ClusterA", 50),
            FlowableEventStream.getEventStream("HTTP-ClusterB", 20)
        )
        // group by type (2 clusters)
        .groupBy(new Function<Event, Object>() {
            @Override
            public Object apply(Event event) {
                return event.type;
            }
        })
        .take(1)
        .blockingForEach(new Consumer<GroupedFlowable<Object, Event>>() {
            @Override
            public void accept(GroupedFlowable<Object, Event> v) {
                System.out.println(v);
                v.take(1).subscribe();  // FIXME groups need consumption to a certain degree to cancel upstream
            }
        });

        System.out.println("**** finished");
    }

    @Test
    public void testTakeUnsubscribesOnFlatMapOfGroupBy() {
        Flowable.merge(
            FlowableEventStream.getEventStream("HTTP-ClusterA", 50),
            FlowableEventStream.getEventStream("HTTP-ClusterB", 20)
        )
        // group by type (2 clusters)
        .groupBy(new Function<Event, Object>() {
            @Override
            public Object apply(Event event) {
                return event.type;
            }
        })
        .flatMap(new Function<GroupedFlowable<Object, Event>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(GroupedFlowable<Object, Event> g) {
                return g.map(new Function<Event, Object>() {
                    @Override
                    public Object apply(Event event) {
                        return event.instanceId + " - " + event.values.get("count200");
                    }
                });
            }
        })
        .take(20)
        .blockingForEach(new Consumer<Object>() {
            @Override
            public void accept(Object v) {
                System.out.println(v);
            }
        });

        System.out.println("**** finished");
    }

    @Test
    public void groupsCompleteAsSoonAsMainCompletes() {
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Flowable.range(0, 20)
        .groupBy(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer i) {
                return i % 5;
            }
        })
        .concatMap(new Function<GroupedFlowable<Integer, Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(GroupedFlowable<Integer, Integer> v) {
                return v;
            }
        }).subscribe(ts);

        ts.assertValues(0, 5, 10, 15, 1, 6, 11, 16, 2, 7, 12, 17, 3, 8, 13, 18, 4, 9, 14, 19);
        ts.assertComplete();
        ts.assertNoErrors();
    }
}
