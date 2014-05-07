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
package rx.lang.scala.examples;

import rx.Observable;
import rx.functions.Action1;
import rx.lang.scala.examples.Movie;
import rx.lang.scala.examples.MovieLib;
import static rx.lang.scala.JavaConversions.toScalaObservable;

public class MovieLibUsage {

    public static void main(String[] args) {

        Observable<Movie> movies = Observable.from(
                new Movie(3000),
                new Movie(1000),
                new Movie(2000)
                );

        MovieLib lib = new MovieLib(toScalaObservable(movies));

        lib.longMovies().asJavaObservable().subscribe(new Action1<Movie>() {

            @Override
            public void call(Movie m) {
                System.out.println("A movie of length " + m.lengthInSeconds() + "s");
            }
        });
    }

}
