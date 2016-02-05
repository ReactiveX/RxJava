/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.nbp;

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.Test;

import io.reactivex.NbpObservable;
import io.reactivex.NbpObservable.NbpTransformer;
import io.reactivex.functions.*;
import io.reactivex.observables.nbp.NbpGroupedObservable;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;

/**
 * Test super/extends of generics.
 * 
 * See https://github.com/Netflix/RxJava/pull/331
 */
public class NbpCovarianceTest {

    /**
     * This won't compile if super/extends isn't done correctly on generics
     */
    @Test
    public void testCovarianceOfFrom() {
        NbpObservable.<Movie> just(new HorrorMovie());
        NbpObservable.<Movie> fromIterable(new ArrayList<HorrorMovie>());
        // NbpObservable.<HorrorMovie>from(new Movie()); // may not compile
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
        NbpObservable<Media> o = NbpObservable.just(new Movie(), new TVSeason(), new Album());
        o.toSortedList(SORT_FUNCTION);

        // this one would NOT work without the covariance generics
        NbpObservable<Movie> o2 = NbpObservable.just(new Movie(), new ActionMovie(), new HorrorMovie());
        o2.toSortedList(SORT_FUNCTION);
    }

    @Test
    public void testGroupByCompose() {
        NbpObservable<Movie> movies = NbpObservable.just(new HorrorMovie(), new ActionMovie(), new Movie());
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<String>();
        movies
        .groupBy(new Function<Movie, Object>() {
            @Override
            public Object apply(Movie v) {
                return v.getClass();
            }
        })
        .doOnNext(new Consumer<NbpGroupedObservable<Object, Movie>>() {
            @Override
            public void accept(NbpGroupedObservable<Object, Movie> g) {
                System.out.println(g.key());
            }
        })
        .flatMap(new Function<NbpGroupedObservable<Object, Movie>, NbpObservable<String>>() {
            @Override
            public NbpObservable<String> apply(NbpGroupedObservable<Object, Movie> g) {
                return g
                .doOnNext(new Consumer<Movie>() {
                    @Override
                    public void accept(Movie pv) {
                        System.out.println(pv);
                    }
                })
                .compose(new Function<NbpObservable<Movie>, NbpObservable<Movie>>() {
                    @Override
                    public NbpObservable<Movie> apply(NbpObservable<Movie> m) {
                        return m.concatWith(NbpObservable.just(new ActionMovie()));
                    }
                }
                )
                .map(new Function<Movie, String>() {
                    @Override
                    public String apply(Movie v) {
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
        NbpObservable<HorrorMovie> movie = NbpObservable.just(new HorrorMovie());
        NbpObservable<Movie> movie2 = movie.compose(new Function<NbpObservable<HorrorMovie>, NbpObservable<Movie>>() {
            @Override
            public NbpObservable<Movie> apply(NbpObservable<HorrorMovie> t) {
                return NbpObservable.just(new Movie());
            }
        });
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testCovarianceOfCompose2() {
        NbpObservable<Movie> movie = NbpObservable.<Movie> just(new HorrorMovie());
        NbpObservable<HorrorMovie> movie2 = movie.compose(new Function<NbpObservable<Movie>, NbpObservable<HorrorMovie>>() {
            @Override
            public NbpObservable<HorrorMovie> apply(NbpObservable<Movie> t) {
                return NbpObservable.just(new HorrorMovie());
            }
        });
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testCovarianceOfCompose3() {
        NbpObservable<Movie> movie = NbpObservable.<Movie>just(new HorrorMovie());
        NbpObservable<HorrorMovie> movie2 = movie.compose(new Function<NbpObservable<Movie>, NbpObservable<HorrorMovie>>() {
            @Override
            public NbpObservable<HorrorMovie> apply(NbpObservable<Movie> t) {
                return NbpObservable.just(new HorrorMovie()).map(new Function<HorrorMovie, HorrorMovie>() {
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
        NbpObservable<HorrorMovie> movie = NbpObservable.just(new HorrorMovie());
        NbpObservable<HorrorMovie> movie2 = movie.compose(new Function<NbpObservable<HorrorMovie>, NbpObservable<HorrorMovie>>() {
            @Override
            public NbpObservable<HorrorMovie> apply(NbpObservable<HorrorMovie> t1) {
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
        NbpObservable<List<Movie>> movies = NbpObservable.just(list1, list2);
        movies.compose(deltaTransformer);
    }

    static Function<List<List<Movie>>, NbpObservable<Movie>> calculateDelta = new Function<List<List<Movie>>, NbpObservable<Movie>>() {
        @Override
        public NbpObservable<Movie> apply(List<List<Movie>> listOfLists) {
            if (listOfLists.size() == 1) {
                return NbpObservable.fromIterable(listOfLists.get(0));
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

                return NbpObservable.fromIterable(delta);
            }
        }
    };
    
    static NbpTransformer<List<Movie>, Movie> deltaTransformer = new NbpTransformer<List<Movie>, Movie>() {
        @Override
        public NbpObservable<Movie> apply(NbpObservable<List<Movie>> movieList) {
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
