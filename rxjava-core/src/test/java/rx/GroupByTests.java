package rx;

import org.junit.Test;

import rx.EventStream.Event;
import rx.observables.GroupedObservable;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

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
                .toBlockingObservable().forEach(new Action1<GroupedObservable<String, Event>>() {

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
                .toBlockingObservable().forEach(new Action1<String>() {

                    @Override
                    public void call(String v) {
                        System.out.println(v);
                    }

                });

        System.out.println("**** finished");
    }
}
