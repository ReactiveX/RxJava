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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.Test;

import rx.CovarianceTest.CoolRating;
import rx.CovarianceTest.ExtendedResult;
import rx.CovarianceTest.HorrorMovie;
import rx.CovarianceTest.Media;
import rx.CovarianceTest.Movie;
import rx.CovarianceTest.Rating;
import rx.CovarianceTest.Result;
import rx.EventStream.Event;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.FuncN;
import rx.observables.GroupedObservable;

public class ZipTests {

    @Test
    public void testZipObservableOfObservables() {
        EventStream.getEventStream("HTTP-ClusterB", 20)
                .groupBy(new Func1<Event, String>() {

                    @Override
                    public String call(Event e) {
                        return e.instanceId;
                    }

                    // now we have streams of cluster+instanceId
                }).flatMap(new Func1<GroupedObservable<String, Event>, Observable<Map<String, String>>>() {

                    @Override
                    public Observable<Map<String, String>> call(final GroupedObservable<String, Event> ge) {
                        return ge.scan(new HashMap<String, String>(), new Func2<Map<String, String>, Event, Map<String, String>>() {

                            @Override
                            public Map<String, String> call(Map<String, String> accum, Event perInstanceEvent) {
                                accum.put("instance", ge.getKey());
                                return accum;
                            }

                        });
                    }
                })
                .take(10)
                .toBlocking().forEach(new Action1<Map<String, String>>() {

                    @Override
                    public void call(Map<String, String> v) {
                        System.out.println(v);
                    }

                });

        System.out.println("**** finished");
    }

    /**
     * This won't compile if super/extends isn't done correctly on generics
     */
    @Test
    public void testCovarianceOfZip() {
        Observable<HorrorMovie> horrors = Observable.from(new HorrorMovie());
        Observable<CoolRating> ratings = Observable.from(new CoolRating());

        Observable.<Movie, CoolRating, Result> zip(horrors, ratings, combine).toBlocking().forEach(action);
        Observable.<Movie, CoolRating, Result> zip(horrors, ratings, combine).toBlocking().forEach(action);
        Observable.<Media, Rating, ExtendedResult> zip(horrors, ratings, combine).toBlocking().forEach(extendedAction);
        Observable.<Media, Rating, Result> zip(horrors, ratings, combine).toBlocking().forEach(action);
        Observable.<Media, Rating, ExtendedResult> zip(horrors, ratings, combine).toBlocking().forEach(action);

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

        Observable<Object> result = Observable.zip(observables, new FuncN<Object>() {
            @Override
            public Object call(final Object... args) {
                System.out.println("received: " + args);
                assertEquals("No argument should have been passed", 0, args.length);
                return invoked;
            }
        });

        assertSame(invoked, result.toBlocking().last());
    }

    Func2<Media, Rating, ExtendedResult> combine = new Func2<Media, Rating, ExtendedResult>() {
        @Override
        public ExtendedResult call(Media m, Rating r) {
            return new ExtendedResult();
        }
    };

    Action1<Result> action = new Action1<Result>() {
        @Override
        public void call(Result t1) {
            System.out.println("Result: " + t1);
        }
    };

    Action1<ExtendedResult> extendedAction = new Action1<ExtendedResult>() {
        @Override
        public void call(ExtendedResult t1) {
            System.out.println("Result: " + t1);
        }
    };
}
