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
package rx.lang.scala
import scala.language.postfixOps
import org.junit.Assert._
import org.junit.Test
import org.scalatest.junit.JUnitSuite

class ConstructorTest extends JUnitSuite {

  @Test def toObservable() {
    val xs = List(1,2,3).toObservable.toBlockingObservable.toList
    assertEquals(List(1,2,3), xs)

    val ys = Observable.from(List(1,2,3)).toBlockingObservable.toList
    assertEquals(List(1,2,3), xs)

    val zs = Observable.items(1,2,3).toBlockingObservable.toList
    assertEquals(List(1,2,3), xs)

  }
}
