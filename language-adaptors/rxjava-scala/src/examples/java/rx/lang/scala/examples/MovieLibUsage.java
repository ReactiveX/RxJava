/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang.scala.examples;

import org.junit.Test;

import rx.Observable;
import rx.util.functions.Action1;


public class MovieLibUsage {
	
	Action1<Movie> moviePrinter = new Action1<Movie>() {
		public void call(Movie m) {
			System.out.println("A movie of length " + m.lengthInSeconds() + "s");
		}
	};
	
	@Test
	public void test() {
		MovieLib lib = new MovieLib(Observable.from(new Movie(3000), new Movie(1000), new Movie(2000)));
		
		lib.longMovies().subscribe(moviePrinter);		
	}

}
