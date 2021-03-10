/*
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

import static org.junit.Assert.assertSame;

import java.util.*;

import org.junit.*;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.observable.ObservableCovarianceTest.*;
import io.reactivex.rxjava3.observable.ObservableEventStream.Event;
import io.reactivex.rxjava3.observables.GroupedObservable;

public class ObservableZipTests extends RxJavaTest {

    @Test
    public void zipObservableOfObservables() throws Exception {
        ObservableEventStream.getEventStream("HTTP-ClusterB", 20)
                .groupBy(e -> e.instanceId)
                // now we have streams of cluster+instanceId
                .flatMap((Function<GroupedObservable<String, Event>, Observable<HashMap<String, String>>>) ge -> ge.scan(new HashMap<>(), (accum, perInstanceEvent) -> {
                    synchronized (accum) {
                        accum.put("instance", ge.getKey());
                    }
                    return accum;
                }))
                .take(10)
                .blockingForEach((Consumer<Object>) pv -> {
                    synchronized (pv) {
                        System.out.println(pv);
                    }
                });

        System.out.println("**** finished");

        Thread.sleep(200); // make sure the event streams receive their interrupt
    }

    /**
     * This won't compile if super/extends isn't done correctly on generics.
     */
    @Test
    public void covarianceOfZip() {
        Observable<HorrorMovie> horrors = Observable.just(new HorrorMovie());
        Observable<CoolRating> ratings = Observable.just(new CoolRating());

        Observable.<Movie, CoolRating, Result> zip(horrors, ratings, combine).blockingForEach(action);
        Observable.<Movie, CoolRating, Result> zip(horrors, ratings, combine).blockingForEach(action);
        Observable.<Media, Rating, ExtendedResult> zip(horrors, ratings, combine).blockingForEach(extendedAction);
        Observable.<Media, Rating, Result> zip(horrors, ratings, combine).blockingForEach(action);
        Observable.<Media, Rating, ExtendedResult> zip(horrors, ratings, combine).blockingForEach(action);

        Observable.<Movie, CoolRating, Result> zip(horrors, ratings, combine);
    }

    /**
     * Occasionally zip may be invoked with 0 observables. Test that we don't block indefinitely instead
     * of immediately invoking zip with 0 argument.
     *
     * We now expect an NoSuchElementException since last() requires at least one value and nothing will be emitted.
     */
    @Test(expected = NoSuchElementException.class)
    public void nonBlockingObservable() {

        final Object invoked = new Object();

        Collection<Observable<Object>> observables = Collections.emptyList();

        Observable<Object> result = Observable.zip(observables, args -> {
            System.out.println("received: " + Arrays.toString(args));
            Assert.assertEquals("No argument should have been passed", 0, args.length);
            return invoked;
        });

        assertSame(invoked, result.blockingLast());
    }

    BiFunction<Media, Rating, ExtendedResult> combine = (m, r) -> new ExtendedResult();

    Consumer<Result> action = t1 -> System.out.println("Result: " + t1);

    Consumer<ExtendedResult> extendedAction = t1 -> System.out.println("Result: " + t1);

    @Test
    public void zipWithDelayError() {
        Observable.just(1)
        .zipWith(Observable.just(2), Integer::sum, true)
        .test()
        .assertResult(3);
    }

    @Test
    public void zipWithDelayErrorBufferSize() {
        Observable.just(1)
        .zipWith(Observable.just(2), Integer::sum, true, 16)
        .test()
        .assertResult(3);
    }

}
