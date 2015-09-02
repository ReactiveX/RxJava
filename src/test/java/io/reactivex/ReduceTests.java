/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.reactivex.CovarianceTest.*;

public class ReduceTests {

    @Test
    public void reduceInts() {
        Observable<Integer> o = Observable.just(1, 2, 3);
        int value = o.reduce((t1, t2) -> t1 + t2).toBlocking().single();

        assertEquals(6, value);
    }

    @SuppressWarnings("unused")
    @Test
    public void reduceWithObjects() {
        Observable<Movie> horrorMovies = Observable.<Movie> just(new HorrorMovie());

        Observable<Movie> reduceResult = horrorMovies.scan((t1, t2) -> t2).takeLast(1);

        Observable<Movie> reduceResult2 = horrorMovies.reduce((t1, t2) -> t2);
    }

    /**
     * Reduce consumes and produces T so can't do covariance.
     * 
     * https://github.com/ReactiveX/RxJava/issues/360#issuecomment-24203016
     */
    @SuppressWarnings("unused")
    @Test
    public void reduceWithCovariantObjects() {
        Observable<Movie> horrorMovies = Observable.<Movie> just(new HorrorMovie());

        Observable<Movie> reduceResult2 = horrorMovies.reduce((t1, t2) -> t2);
    }

    /**
     * Reduce consumes and produces T so can't do covariance.
     * 
     * https://github.com/ReactiveX/RxJava/issues/360#issuecomment-24203016
     */
    @Test
    public void reduceCovariance() {
        // must type it to <Movie>
        Observable<Movie> horrorMovies = Observable.<Movie> just(new HorrorMovie());
        libraryFunctionActingOnMovieObservables(horrorMovies);
    }

    /*
     * This accepts <Movie> instead of <? super Movie> since `reduce` can't handle covariants
     */
    public void libraryFunctionActingOnMovieObservables(Observable<Movie> obs) {

        obs.reduce((t1, t2) -> t2);
    }

}