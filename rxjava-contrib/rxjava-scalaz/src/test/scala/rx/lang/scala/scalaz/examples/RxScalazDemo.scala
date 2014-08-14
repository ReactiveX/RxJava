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
package rx.lang.scala.scalaz.examples

import org.specs2.scalaz.Spec
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import rx.lang.scala.Observable
import rx.lang.scala.Observable.just
import scalaz._
import Scalaz._

/**
 * This demonstrates how you can apply Scalaz's operators to Observables.
 */
@RunWith(classOf[JUnitRunner])
class RxScalazDemo extends Spec {

  import rx.lang.scala.scalaz._
  import ImplicitsForTest._

  "RxScalazDemo" should {
    "Monoid Operators" >> {
      "can apply to Observables" in {
        (just(1, 2) |+| just(3, 4)) === just(1, 2, 3, 4)
        (just(1, 2) ⊹ just(3, 4)) === just(1, 2, 3, 4)
        mzero[Observable[Int]] === Observable.empty
      }
    }

    "Functor Operators" >> {
      "can apply to Observables" in {
        (just(1, 2) ∘ {_ + 1}) === just(2, 3)
        (just(1, 2) >| 5) === just(5, 5)
        (just(1, 2) as 4) === just(4, 4)

        just(1, 2).fpair === just((1, 1), (2, 2))
        just(1, 2).fproduct {_ + 1} === just((1, 2), (2, 3))
        just(1, 2).strengthL("x") === just(("x", 1), ("x", 2))
        just(1, 2).strengthR("x") === just((1, "x"), (2, "x"))
        Functor[Observable].lift {(_: Int) + 1}(just(1, 2)) === just(2, 3)
      }
    }

    "Applicative Operators" >> {
      "can apply to Observables" in {
        1.point[Observable] === just(1)
        1.η[Observable] === just(1)
        (just(1, 2) |@| just(3, 4)) {_ + _} === just(4, 5, 5, 6)

        (just(1) <*> {(_: Int) + 1}.η[Observable]) === just(2)
        just(1) <*> {just(2) <*> {(_: Int) + (_: Int)}.curried.η[Observable]} === just(3)
        just(1) <* just(2) === just(1)
        just(1) *> just(2) === just(2)

        Apply[Observable].ap(just(2)) {{(_: Int) + 3}.η[Observable]} === just(5)
        Apply[Observable].lift2 {(_: Int) * (_: Int)}(just(1, 2), just(3, 4)) === just(3, 4, 6, 8)
      }
    }

    "Monad and MonadPlus Opeartors" >> {
      "can apply to Observables" in {
        (just(3) >>= {(i: Int) => just(i + 1)}) === just(4)
        just(3) >> just(2) === just(2)
        just(just(1, 2), just(3, 4)).μ === just(1, 2, 3, 4)
        just(1, 2) <+> just(3, 4) === just(1, 2, 3, 4)

        PlusEmpty[Observable].empty[Int] === Observable.empty
      }
    }

    "Traverse and Foldable Opearators" >> {
      "can apply to Observables" in {
        just(1, 2, 3).foldMap {_.toString} === "123"
        just(1, 2, 3).foldLeftM(0)((acc, v) => (acc * v).some) === 6.some
        just(1, 2, 3).suml === 6
        just(1, 2, 3).∀(_ > 3) === true
        just(1, 2, 3).∃(_ > 3) === false
        just(1, 2, 3).traverse(x => (x + 1).some) === just(2, 3, 4).some
        just(1.some, 2.some).sequence === just(1, 2).some
      }
    }
  }
}
