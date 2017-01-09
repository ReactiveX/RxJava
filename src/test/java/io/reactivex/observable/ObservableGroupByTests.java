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

package io.reactivex.observable;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.functions.*;
import io.reactivex.observable.ObservableEventStream.Event;
import io.reactivex.observables.GroupedObservable;

public class ObservableGroupByTests {

    @Test
    public void testTakeUnsubscribesOnGroupBy() throws Exception {
        Observable.merge(
            ObservableEventStream.getEventStream("HTTP-ClusterA", 50),
            ObservableEventStream.getEventStream("HTTP-ClusterB", 20)
        )
        // group by type (2 clusters)
        .groupBy(new Function<Event, String>() {
            @Override
            public String apply(Event event) {
                return event.type;
            }
        })
        .take(1)
        .blockingForEach(new Consumer<GroupedObservable<String, Event>>() {
            @Override
            public void accept(GroupedObservable<String, Event> v) {
                System.out.println(v);
                v.take(1).subscribe();  // FIXME groups need consumption to a certain degree to cancel upstream
            }
        });

        System.out.println("**** finished");

        Thread.sleep(200); // make sure the event streams receive their interrupt
    }

    @Test
    public void testTakeUnsubscribesOnFlatMapOfGroupBy() throws Exception {
        Observable.merge(
            ObservableEventStream.getEventStream("HTTP-ClusterA", 50),
            ObservableEventStream.getEventStream("HTTP-ClusterB", 20)
        )
        // group by type (2 clusters)
        .groupBy(new Function<Event, String>() {
            @Override
            public String apply(Event event) {
                return event.type;
            }
        })
        .flatMap(new Function<GroupedObservable<String, Event>, Observable<Object>>() {
            @Override
            public Observable<Object> apply(GroupedObservable<String, Event> g) {
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
            public void accept(Object pv) {
                System.out.println(pv);
            }
        });

        System.out.println("**** finished");

        Thread.sleep(200); // make sure the event streams receive their interrupt
    }
}
