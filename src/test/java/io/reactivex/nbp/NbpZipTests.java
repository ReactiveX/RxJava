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

import static org.junit.Assert.assertSame;

import java.util.*;

import org.junit.*;

import io.reactivex.NbpObservable;
import io.reactivex.functions.*;
import io.reactivex.nbp.NbpCovarianceTest.*;
import io.reactivex.nbp.NbpEventStream.Event;
import io.reactivex.observables.nbp.NbpGroupedObservable;

public class NbpZipTests {

    @Test
    public void testZipNbpObservableOfNbpObservables() {
        NbpEventStream.getEventStream("HTTP-ClusterB", 20)
                .groupBy(new Function<Event, String>() {
                    @Override
                    public String apply(Event e) {
                        return e.instanceId;
                    }
                })
                // now we have streams of cluster+instanceId
                .flatMap(new Function<NbpGroupedObservable<String, Event>, NbpObservable<HashMap<String, String>>>() {
                    @Override
                    public NbpObservable<HashMap<String, String>> apply(final NbpGroupedObservable<String, Event> ge) {
                            return ge.scan(new HashMap<String, String>(), new BiFunction<HashMap<String, String>, Event, HashMap<String, String>>() {
                                @Override
                                public HashMap<String, String> apply(HashMap<String, String> accum,
                                        Event perInstanceEvent) {
                                            accum.put("instance", ge.key());
                                            return accum;
                                        }
                            });
                    }
                })
                .take(10)
                .toBlocking().forEach(new Consumer<Object>() {
                    @Override
                    public void accept(Object pv) {
                        System.out.println(pv);
                    }
                });

        System.out.println("**** finished");
    }

    /**
     * This won't compile if super/extends isn't done correctly on generics
     */
    @Test
    public void testCovarianceOfZip() {
        NbpObservable<HorrorMovie> horrors = NbpObservable.just(new HorrorMovie());
        NbpObservable<CoolRating> ratings = NbpObservable.just(new CoolRating());

        NbpObservable.<Movie, CoolRating, Result> zip(horrors, ratings, combine).toBlocking().forEach(action);
        NbpObservable.<Movie, CoolRating, Result> zip(horrors, ratings, combine).toBlocking().forEach(action);
        NbpObservable.<Media, Rating, ExtendedResult> zip(horrors, ratings, combine).toBlocking().forEach(extendedAction);
        NbpObservable.<Media, Rating, Result> zip(horrors, ratings, combine).toBlocking().forEach(action);
        NbpObservable.<Media, Rating, ExtendedResult> zip(horrors, ratings, combine).toBlocking().forEach(action);

        NbpObservable.<Movie, CoolRating, Result> zip(horrors, ratings, combine);
    }

    /**
     * Occasionally zip may be invoked with 0 NbpObservables. Test that we don't block indefinitely instead
     * of immediately invoking zip with 0 argument.
     * 
     * We now expect an NoSuchElementException since last() requires at least one value and nothing will be emitted.
     */
    @Test(expected = NoSuchElementException.class)
    public void nonBlockingNbpObservable() {

        final Object invoked = new Object();

        Collection<NbpObservable<Object>> NbpObservables = Collections.emptyList();

        NbpObservable<Object> result = NbpObservable.zip(NbpObservables, new Function<Object[], Object>() {
            @Override
            public Object apply(Object[] args) {
                System.out.println("received: " + args);
                Assert.assertEquals("No argument should have been passed", 0, args.length);
                return invoked;
            }
        });

        assertSame(invoked, result.toBlocking().last());
    }

    BiFunction<Media, Rating, ExtendedResult> combine = new BiFunction<Media, Rating, ExtendedResult>() {
        @Override
        public ExtendedResult apply(Media m, Rating r) {
                return new ExtendedResult();
        }
    };

    Consumer<Result> action = new Consumer<Result>() {
        @Override
        public void accept(Result t1) {
            System.out.println("Result: " + t1);
        }
    };

    Consumer<ExtendedResult> extendedAction = new Consumer<ExtendedResult>() {
        @Override
        public void accept(ExtendedResult t1) {
            System.out.println("Result: " + t1);
        }
    };
}