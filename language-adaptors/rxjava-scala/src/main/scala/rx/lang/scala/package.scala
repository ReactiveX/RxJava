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
package rx.lang

import java.util.concurrent.TimeUnit
import java.util.Date
import rx.lang.scala.concurrency.Scheduler

/**
 * This package contains all classes that RxScala users need.
 * 
 * It mirrors the structure of package `rx`, but implementation classes that RxScala users
 * will not need are left out.
 */
package object scala {

  /**
   * Allows to construct observables in a similar way as futures.
   * 
   * Example:
   *
   * {{{
   * implicit val scheduler = Schedulers.threadPoolForIO
   * val o: Observable[List[Friend]] = observable {
   *    session.getFriends
   * }
   * o.subscribe(
   *   friendList => println(friendList),
   *   err => println(err.getMessage)
   * )
   * }}} 
   */
  def observable[T](body: => T)(implicit scheduler: Scheduler): Observable[T] = {
    Observable(1).observeOn(scheduler).map(_ => body)
  }
}
