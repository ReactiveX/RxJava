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
import Scalaz._
import scalaz.scalacheck.ScalazProperties._
import rx.lang.scala.Observable
import org.specs2.scalaz.Spec
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class ObservableTraverseSpec extends Spec {

  import rx.lang.scala.scalaz._
  import ImplicitsForTest._

  "Observable" should {
    "satisfies traverse laws" in {
      checkAll(traverse.laws[Observable])
    }
  }
}