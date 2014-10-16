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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import rx.functions.Func1;
import rx.functions.Func2;

/**
 * Test super/extends of generics.
 * 
 * See https://github.com/Netflix/RxJava/pull/331
 */
public class CovarianceTest {

    /**
     * This won't compile if super/extends isn't done correctly on generics
     */
    @Test
    public void testCovarianceOfFrom() {
        Observable.<Movie> just(new HorrorMovie());
        Observable.<Movie> from(new ArrayList<HorrorMovie>());
        // Observable.<HorrorMovie>from(new Movie()); // may not compile
    }

    @Test
    public void testSortedList() {
        Func2<Media, Media, Integer> SORT_FUNCTION = new Func2<Media, Media, Integer>() {

            @Override
            public Integer call(Media t1, Media t2) {
                return 1;
            }
        };

        // this one would work without the covariance generics
        Observable<Media> o = Observable.just(new Movie(), new TVSeason(), new Album());
        o.toSortedList(SORT_FUNCTION);

        // this one would NOT work without the covariance generics
        Observable<Movie> o2 = Observable.just(new Movie(), new ActionMovie(), new HorrorMovie());
        o2.toSortedList(SORT_FUNCTION);
    }

    
    @Test
    public void testCovarianceOfCompose() {
        Observable<HorrorMovie> movie = Observable.just(new HorrorMovie());
        Observable<Movie> movie2 = movie.compose(new Func1<Observable<Movie>, Observable<? extends Movie>>() {

            @Override
            public Observable<? extends Movie> call(Observable<Movie> t1) {
                return Observable.just(new Movie());
            }
            
        });
    }

    @Test
    public void testCovarianceOfCompose2() {
        Observable<Movie> movie = Observable.<Movie> just(new HorrorMovie());
        Observable<HorrorMovie> movie2 = movie.compose(new Func1<Observable<Movie>, Observable<? extends HorrorMovie>>() {
            @Override
            public Observable<? extends HorrorMovie> call(Observable<Movie> t1) {
                return Observable.just(new HorrorMovie());
            }
        });
    }

    @Test
    public void testCovarianceOfCompose3() {
        Observable<Movie> movie = Observable.<Movie>just(new HorrorMovie());
        Observable<HorrorMovie> movie2 = movie.compose(new Func1<Observable<Movie>, Observable<? extends HorrorMovie>>() {
            @Override
            public Observable<? extends HorrorMovie> call(Observable<Movie> t1) {
                return Observable.just(new HorrorMovie()).map(new Func1<HorrorMovie, HorrorMovie>() {

                    @Override
                    public HorrorMovie call(HorrorMovie horrorMovie) {
                        return horrorMovie;
                    }
                });
            }
        });
    }

    @Test
    public void testCovarianceOfCompose4() {
        Observable<HorrorMovie> movie = Observable.just(new HorrorMovie());
        Observable<HorrorMovie> movie2 = movie.compose(new Func1<Observable<HorrorMovie>, Observable<? extends HorrorMovie>>() {
            @Override
            public Observable<? extends HorrorMovie> call(Observable<HorrorMovie> t1) {
                return t1.map(new Func1<HorrorMovie, HorrorMovie>() {

                    @Override
                    public HorrorMovie call(HorrorMovie horrorMovie) {
                        return horrorMovie;
                    }
                });
            }
        });
    }
    
    @Test
    public void testComposeWithDeltaLogic() {
        List<Movie> list1 = Arrays.asList(new Movie(), new HorrorMovie(), new ActionMovie());
        List<Movie> list2 = Arrays.asList(new ActionMovie(), new Movie(), new HorrorMovie(), new ActionMovie());
        Observable<List<Movie>> movies = Observable.just(list1, list2);
        movies.compose(deltaTransformer);
    }

    static Func1<Observable<List<Movie>>, Observable<? extends Movie>> deltaTransformer = new Func1<Observable<List<Movie>>, Observable<? extends Movie>>() {
        @Override
        public Observable<Movie> call(Observable<List<Movie>> movieList) {
            return movieList
                .startWith(new ArrayList<Movie>())
                .buffer(2, 1)
                .skip(1)
                .flatMap(calculateDelta);
        }
    };

    static Func1<List<List<Movie>>, Observable<Movie>> calculateDelta = new Func1<List<List<Movie>>, Observable<Movie>>() {
        public Observable<Movie> call(List<List<Movie>> listOfLists) {
            if (listOfLists.size() == 1) {
                return Observable.from(listOfLists.get(0));
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
                for (Movie old : oldList) {
                    delta.add(new Movie());
                }

                return Observable.from(delta);
            }
        };
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
