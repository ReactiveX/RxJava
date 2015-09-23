/**
 * Copyright 2015 Netflix, Inc.
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

public class NbpGroupByTests {

    @Test
    public void testTakeUnsubscribesOnGroupBy() {
        NbpObservable.merge(
            NbpEventStream.getEventStream("HTTP-ClusterA", 50),
            NbpEventStream.getEventStream("HTTP-ClusterB", 20)
        )
        // group by type (2 clusters)
        .groupBy(event -> event.type)
        .take(1)
        .toBlocking()
        .forEach(v -> {
            System.out.println(v);
            v.take(1).subscribe();  // FIXME groups need consumption to a certain degree to cancel upstream
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
        .groupBy(event -> event.type)
        .flatMap(g -> g.map(event -> event.instanceId + " - " + event.values.get("count200")))
        .take(20)
        .toBlocking()
        .forEach(System.out::println);

        System.out.println("**** finished");
    }
}