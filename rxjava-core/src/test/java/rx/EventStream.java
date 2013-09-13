package rx;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import rx.Observable.OnSubscribeFunc;
import rx.concurrency.Schedulers;
import rx.subscriptions.BooleanSubscription;
import rx.util.functions.Action0;

/**
 * Utility for retrieving a mock eventstream for testing.
 */
public class EventStream {

    public static Observable<Event> getEventStream(final String type, final int numInstances) {
        return Observable.create(new OnSubscribeFunc<Event>() {

            @Override
            public Subscription onSubscribe(final Observer<? super Event> observer) {
                final BooleanSubscription s = new BooleanSubscription();
                // run on a background thread inside the OnSubscribeFunc so unsubscribe works
                Schedulers.newThread().schedule(new Action0() {

                    @Override
                    public void call() {
                        while (!(s.isUnsubscribed() || Thread.currentThread().isInterrupted())) {
                            observer.onNext(randomEvent(type, numInstances));
                            try {
                                // slow it down somewhat
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                observer.onError(e);
                            }
                        }
                        observer.onCompleted();
                    }

                });

                return s;
            }
        });
    }

    public static Event randomEvent(String type, int numInstances) {
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        values.put("count200", randomIntFrom0to(4000));
        values.put("count4xx", randomIntFrom0to(300));
        values.put("count5xx", randomIntFrom0to(500));
        return new Event(type, "instance_" + randomIntFrom0to(numInstances), values);
    }

    private static int randomIntFrom0to(int max) {
        // XORShift instead of Math.random http://javamex.com/tutorials/random_numbers/xorshift.shtml
        long x = System.nanoTime();
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        return Math.abs((int) x % max);
    }

    public static class Event {
        public final String type;
        public final String instanceId;
        public final Map<String, Object> values;

        /**
         * @param type
         * @param instanceId
         * @param values
         *            This does NOT deep-copy, so do not mutate this Map after passing it in.
         */
        public Event(String type, String instanceId, Map<String, Object> values) {
            this.type = type;
            this.instanceId = instanceId;
            this.values = Collections.unmodifiableMap(values);
        }
    }
}
