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
package rx.lang.scala.scalaz

import scalaz._
import rx.lang.scala.Observable
import org.scalacheck.Arbitrary
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration

/**
 * This object provides implicits for tests.
 */
object ImplicitsForTest {

  // Equality based on sequenceEqual() method.
  implicit def observableEqual[A](implicit eqA: Equal[A]) = new Equal[Observable[A]]{
    def equal(a1: Observable[A], a2: Observable[A]) = {
      val p = Promise[Boolean]
      val sub = a1.sequenceEqual(a2).firstOrElse(false).subscribe(v => p.success(v))
      try {
        Await.result(p.future, Duration.Inf)
      } finally {
        sub.unsubscribe()
      }
    }
  }

  implicit def observableArbitrary[A](implicit a: Arbitrary[A], array: Arbitrary[Array[A]]): Arbitrary[Observable[A]]
  = Arbitrary(for (arr <- array.arbitrary) yield Observable.items(arr:_*))

}
