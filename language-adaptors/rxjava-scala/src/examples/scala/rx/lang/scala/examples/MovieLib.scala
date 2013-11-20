/**
 * Copyright 2013 Netflix, Inc.
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
package rx.lang.scala.examples

import rx.lang.scala.Observable

class Movie(val lengthInSeconds: Int) { }

class MovieLib(val moviesStream: Observable[Movie]) {

  val threshold = 1200

  def shortMovies: Observable[Movie] = moviesStream.filter(_.lengthInSeconds <= threshold)

  def longMovies: Observable[Movie] = moviesStream.filter(_.lengthInSeconds > threshold)

}
