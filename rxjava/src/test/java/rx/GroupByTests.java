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
package rx;

import org.junit.Test;

import rx.EventStream.Event;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.GroupedObservable;

public class GroupByTests {

    @Test
    public void testTakeUnsubscribesOnGroupBy() {
        Observable.merge(
                EventStream.getEventStream("HTTP-ClusterA", 50),
                EventStream.getEventStream("HTTP-ClusterB", 20))
                // group by type (2 clusters)
                .groupBy(new Func1<Event, String>() {

                    @Override
                    public String call(Event event) {
                        return event.type;
                    }

                }).take(1)
                .toBlocking().forEach(new Action1<GroupedObservable<String, Event>>() {

                    @Override
                    public void call(GroupedObservable<String, Event> g) {
                        System.out.println(g);
                    }

                });

        System.out.println("**** finished");
    }

    @Test
    public void testTakeUnsubscribesOnFlatMapOfGroupBy() {
        Observable.merge(
                EventStream.getEventStream("HTTP-ClusterA", 50),
                EventStream.getEventStream("HTTP-ClusterB", 20))
                // group by type (2 clusters)
                .groupBy(new Func1<Event, String>() {

                    @Override
                    public String call(Event event) {
                        return event.type;
                    }

                })
                .flatMap(new Func1<GroupedObservable<String, Event>, Observable<String>>() {

                    @Override
                    public Observable<String> call(GroupedObservable<String, Event> g) {
                        return g.map(new Func1<Event, String>() {

                            @Override
                            public String call(Event event) {
                                return event.instanceId + " - " + event.values.get("count200");
                            }
                        });
                    }

                })
                .take(20)
                .toBlocking().forEach(new Action1<String>() {

                    @Override
                    public void call(String v) {
                        System.out.println(v);
                    }

                });

        System.out.println("**** finished");
    }
}
