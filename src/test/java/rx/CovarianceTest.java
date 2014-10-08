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

import org.junit.Test;

import rx.Observable.Transformer;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * Test super/extends of generics.
 * 
 * See https://github.com/Netflix/RxJava/pull/331
 */
public class CovarianceTest {

    /**
     * This won't compile if super/extends isn't done correctly on generics.
     */
    @Test
    public void testCovarianceOfFrom() {
        Observable.<Movie> just(new HorrorMovie());
        Observable.<Movie> from(new ArrayList<HorrorMovie>());
        // Observable.<HorrorMovie>from(new Movie()); // may not compile
    }

    @Test
    public void testSortedList() {
        Func2<Media, Media, Integer> sortFunction = new Func2<Media, Media, Integer>() {

            @Override
            public Integer call(Media t1, Media t2) {
                return 1;
            }
        };

        // this one would work without the covariance generics
        Observable<Media> o = Observable.just(new Movie(), new TVSeason(), new Album());
        o.toSortedList(sortFunction);

        // this one would NOT work without the covariance generics
        Observable<Movie> o2 = Observable.just(new Movie(), new ActionMovie(), new HorrorMovie());
        o2.toSortedList(sortFunction);
    }


    @Test
    public void testCovarianceOfCompose() {
        Observable<HorrorMovie> movie = Observable.just(new HorrorMovie());
        @SuppressWarnings("unused")
		Observable<Movie> movie2 = movie.compose(new Transformer<Movie, Movie>() {

            @Override
            public Observable<? extends Movie> call(Observable<? extends Movie> t1) {
                return Observable.just(new Movie());
            }

        });
    }

    @Test
    public void testCovarianceOfCompose2() {
        Observable<Movie> movie = Observable.<Movie> just(new HorrorMovie());
        @SuppressWarnings("unused")
		Observable<HorrorMovie> movie2 = movie.compose(new Transformer<Movie, HorrorMovie>() {
            @Override
            public Observable<? extends HorrorMovie> call(Observable<? extends Movie> t1) {
                return Observable.just(new HorrorMovie());
            }
        });
    }

    @Test
    public void testCovarianceOfCompose3() {
        Observable<Movie> movie = Observable.<Movie>just(new HorrorMovie());
        @SuppressWarnings("unused")
		Observable<HorrorMovie> movie2 = movie.compose(new Transformer<Movie, HorrorMovie>() {
            @Override
            public Observable<? extends HorrorMovie> call(Observable<? extends Movie> t1) {
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
        @SuppressWarnings("unused")
		Observable<HorrorMovie> movie2 = movie.compose(new Transformer<HorrorMovie, HorrorMovie>() {
            @Override
            public Observable<? extends HorrorMovie> call(Observable<? extends HorrorMovie> t1) {
                return t1.map(new Func1<HorrorMovie, HorrorMovie>() {

                    @Override
                    public HorrorMovie call(HorrorMovie horrorMovie) {
                        return horrorMovie;
                    }
                });
            }
        });
    }

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
