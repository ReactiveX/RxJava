/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex.flowable;

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.flowables.GroupedFlowable;
import io.reactivex.functions.*;
import io.reactivex.subscribers.TestSubscriber;

/**
 * Test super/extends of generics.
 *
 * See https://github.com/Netflix/RxJava/pull/331
 */
public class FlowableCovarianceTest {

    /**
     * This won't compile if super/extends isn't done correctly on generics.
     */
    @Test
    public void testCovarianceOfFrom() {
        Flowable.<Movie> just(new HorrorMovie());
        Flowable.<Movie> fromIterable(new ArrayList<HorrorMovie>());
        // Observable.<HorrorMovie>from(new Movie()); // may not compile
    }

    @Test
    public void testSortedList() {
        Comparator<Media> SORT_FUNCTION = new Comparator<Media>() {
            @Override
            public int compare(Media t1, Media t2) {
                return 1;
            }
        };

        // this one would work without the covariance generics
        Flowable<Media> o = Flowable.just(new Movie(), new TVSeason(), new Album());
        o.toSortedList(SORT_FUNCTION);

        // this one would NOT work without the covariance generics
        Flowable<Movie> o2 = Flowable.just(new Movie(), new ActionMovie(), new HorrorMovie());
        o2.toSortedList(SORT_FUNCTION);
    }

    @Test
    public void testGroupByCompose() {
        Flowable<Movie> movies = Flowable.just(new HorrorMovie(), new ActionMovie(), new Movie());
        TestSubscriber<String> ts = new TestSubscriber<String>();

        movies
        .groupBy(new Function<Movie, Object>() {
            @Override
            public Object apply(Movie v) {
                return v.getClass();
            }
        })
        .doOnNext(new Consumer<GroupedFlowable<Object, Movie>>() {
            @Override
            public void accept(GroupedFlowable<Object, Movie> g) {
                System.out.println(g.getKey());
            }
        })
        .flatMap(new Function<GroupedFlowable<Object, Movie>, Publisher<String>>() {
            @Override
            public Publisher<String> apply(GroupedFlowable<Object, Movie> g) {
                return g
                        .doOnNext(new Consumer<Movie>() {
                            @Override
                            public void accept(Movie v) {
                                System.out.println(v);
                            }
                        })
                        .compose(new FlowableTransformer<Movie, Movie>() {
                            @Override
                            public Publisher<Movie> apply(Flowable<Movie> m) {
                                return m.concatWith(Flowable.just(new ActionMovie()));
                            }
                        }
                                )
                        .map(new Function<Object, String>() {
                            @Override
                            public String apply(Object v) {
                                return v.toString();
                            }
                        });
            }
        })
        .subscribe(ts);
        ts.assertTerminated();
        ts.assertNoErrors();
        //        System.out.println(ts.getOnNextEvents());
        assertEquals(6, ts.valueCount());
    }

    @SuppressWarnings("unused")
    @Test
    public void testCovarianceOfCompose() {
        Flowable<HorrorMovie> movie = Flowable.just(new HorrorMovie());
        Flowable<Movie> movie2 = movie.compose(new FlowableTransformer<HorrorMovie, Movie>() {
            @Override
            public Publisher<Movie> apply(Flowable<HorrorMovie> t) {
                return Flowable.just(new Movie());
            }
        });
    }

    @SuppressWarnings("unused")
    @Test
    public void testCovarianceOfCompose2() {
        Flowable<Movie> movie = Flowable.<Movie> just(new HorrorMovie());
        Flowable<HorrorMovie> movie2 = movie.compose(new FlowableTransformer<Movie, HorrorMovie>() {
            @Override
            public Publisher<HorrorMovie> apply(Flowable<Movie> t) {
                return Flowable.just(new HorrorMovie());
            }
        });
    }

    @SuppressWarnings("unused")
    @Test
    public void testCovarianceOfCompose3() {
        Flowable<Movie> movie = Flowable.<Movie>just(new HorrorMovie());
        Flowable<HorrorMovie> movie2 = movie.compose(new FlowableTransformer<Movie, HorrorMovie>() {
            @Override
            public Publisher<HorrorMovie> apply(Flowable<Movie> t) {
                return Flowable.just(new HorrorMovie()).map(new Function<HorrorMovie, HorrorMovie>() {
                    @Override
                    public HorrorMovie apply(HorrorMovie v) {
                        return v;
                    }
                });
            }
        }
        );
    }

    @SuppressWarnings("unused")
    @Test
    public void testCovarianceOfCompose4() {
        Flowable<HorrorMovie> movie = Flowable.just(new HorrorMovie());
        Flowable<HorrorMovie> movie2 = movie.compose(new FlowableTransformer<HorrorMovie, HorrorMovie>() {
            @Override
            public Publisher<HorrorMovie> apply(Flowable<HorrorMovie> t1) {
                return t1.map(new Function<HorrorMovie, HorrorMovie>() {
                    @Override
                    public HorrorMovie apply(HorrorMovie v) {
                        return v;
                    }
                });
            }
        });
    }

    @Test
    public void testComposeWithDeltaLogic() {
        List<Movie> list1 = Arrays.asList(new Movie(), new HorrorMovie(), new ActionMovie());
        List<Movie> list2 = Arrays.asList(new ActionMovie(), new Movie(), new HorrorMovie(), new ActionMovie());
        Flowable<List<Movie>> movies = Flowable.just(list1, list2);
        movies.compose(deltaTransformer);
    }

    static Function<List<List<Movie>>, Flowable<Movie>> calculateDelta = new Function<List<List<Movie>>, Flowable<Movie>>() {
        @Override
        public Flowable<Movie> apply(List<List<Movie>> listOfLists) {
            if (listOfLists.size() == 1) {
                return Flowable.fromIterable(listOfLists.get(0));
            } else {
                // diff the two
                List<Movie> newList = listOfLists.get(1);
                List<Movie> oldList = new ArrayList<Movie>(listOfLists.get(0));

                Set<Movie> delta = new LinkedHashSet<Movie>();
                delta.addAll(newList);
                // remove all that match in old
                delta.removeAll(oldList);

                // filter oldList to those that aren't in the newList
                oldList.removeAll(newList);

                // for all left in the oldList we'll create DROP events
                for (@SuppressWarnings("unused") Movie old : oldList) {
                    delta.add(new Movie());
                }

                return Flowable.fromIterable(delta);
            }
        }
    };

    static FlowableTransformer<List<Movie>, Movie> deltaTransformer = new FlowableTransformer<List<Movie>, Movie>() {
        @Override
        public Publisher<Movie> apply(Flowable<List<Movie>> movieList) {
            return movieList
                .startWith(new ArrayList<Movie>())
                .buffer(2, 1)
                .skip(1)
                .flatMap(calculateDelta);
        }
    };

    /*
     * Most tests are moved into their applicable classes such as [Operator]Tests.java
     */

    static class Media {
    }

    static class Movie extends Media {
    }

    static class HorrorMovie extends Movie {
    }

    static class ActionMovie extends Movie {
    }

    static class Album extends Media {
    }

    static class TVSeason extends Media {
    }

    static class Rating {
    }

    static class CoolRating extends Rating {
    }

    static class Result {
    }

    static class ExtendedResult extends Result {
    }
}
