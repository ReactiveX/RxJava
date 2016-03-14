/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.nbp;

import org.junit.Test;

import io.reactivex.NbpObservable;
import io.reactivex.functions.*;
import io.reactivex.nbp.NbpEventStream.Event;
import io.reactivex.observables.nbp.NbpGroupedObservable;

public class NbpGroupByTests {

    @Test
    public void testTakeUnsubscribesOnGroupBy() {
        NbpObservable.merge(
            NbpEventStream.getEventStream("HTTP-ClusterA", 50),
            NbpEventStream.getEventStream("HTTP-ClusterB", 20)
        )
        // group by type (2 clusters)
        .groupBy(new Function<Event, String>() {
            @Override
            public String apply(Event event) {
                return event.type;
            }
        })
        .take(1)
        .toBlocking()
        .forEach(new Consumer<NbpGroupedObservable<String, Event>>() {
            @Override
            public void accept(NbpGroupedObservable<String, Event> v) {
                System.out.println(v);
                v.take(1).subscribe();  // FIXME groups need consumption to a certain degree to cancel upstream
            }
        });

        System.out.println("**** finished");
    }

    @Test
    public void testTakeUnsubscribesOnFlatMapOfGroupBy() {
        NbpObservable.merge(
            NbpEventStream.getEventStream("HTTP-ClusterA", 50),
            NbpEventStream.getEventStream("HTTP-ClusterB", 20)
        )
        // group by type (2 clusters)
        .groupBy(new Function<Event, String>() {
            @Override
            public String apply(Event event) {
                return event.type;
            }
        })
        .flatMap(new Function<NbpGroupedObservable<String, Event>, NbpObservable<Object>>() {
            @Override
            public NbpObservable<Object> apply(NbpGroupedObservable<String, Event> g) {
                return g.map(new Function<Event, Object>() {
                    @Override
                    public Object apply(Event event) {
                        return event.instanceId + " - " + event.values.get("count200");
                    }
                });
            }
        })
        .take(20)
        .toBlocking()
        .forEach(new Consumer<Object>() {
            @Override
            public void accept(Object pv) {
                System.out.println(pv);
            }
        });

        System.out.println("**** finished");
    }
}