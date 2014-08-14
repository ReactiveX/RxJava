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
package rx.lang.scala.observables

import scala.concurrent.Await
import scala.concurrent.duration._
import org.junit.Assert._
import org.junit.Test
import org.scalatest.junit.JUnitSuite
import scala.language.postfixOps
import rx.lang.scala.Observable

class BlockingObservableTest extends JUnitSuite {

  @Test
  def testSingleOption() {
    val o = Observable.just(1)
    assertEquals(Some(1), o.toBlocking.singleOption)
  }

  @Test
  def testSingleOptionWithEmpty() {
    val o = Observable.empty
    assertEquals(None, o.toBlocking.singleOption)
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testSingleOptionWithMultipleItems() {
    Observable.just(1, 2).toBlocking.singleOption
  }

  @Test
  def testSingleOrElse() {
    val o = Observable.just(1)
    assertEquals(1, o.toBlocking.singleOrElse(2))
  }

  @Test
  def testSingleOrElseWithEmpty() {
    val o = Observable.empty
    assertEquals(2, o.toBlocking.singleOrElse(2))
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testSingleOrElseWithMultipleItems() {
    Observable.just(1, 2).toBlocking.singleOrElse(2)
  }

  @Test
  def testHeadOption() {
    val o = Observable.just(1)
    assertEquals(Some(1), o.toBlocking.headOption)
  }

  @Test
  def testHeadOptionWithEmpty() {
    val o = Observable.empty
    assertEquals(None, o.toBlocking.headOption)
  }

  @Test
  def testHeadOptionWithMultipleItems() {
    val o = Observable.just(1, 2)
    assertEquals(Some(1), o.toBlocking.headOption)
  }

  @Test
  def testHeadOrElse() {
    val o = Observable.just(1)
    assertEquals(1, o.toBlocking.headOrElse(2))
  }

  @Test
  def testHeadOrElseWithEmpty() {
    val o = Observable.empty
    assertEquals(2, o.toBlocking.headOrElse(2))
  }

  @Test
  def testHeadOrElseWithMultipleItems() {
    val o = Observable.just(1, 2)
    assertEquals(1, o.toBlocking.headOrElse(2))
  }

  @Test
  def testLastOption() {
    val o = Observable.just(1)
    assertEquals(Some(1), o.toBlocking.lastOption)
  }

  @Test
  def testLastOptionWithEmpty() {
    val o = Observable.empty
    assertEquals(None, o.toBlocking.lastOption)
  }

  @Test
  def testLastOptionWithMultipleItems() {
    val o = Observable.just(1, 2)
    assertEquals(Some(2), o.toBlocking.lastOption)
  }

  @Test
  def testLastOrElse() {
    val o = Observable.just(1)
    assertEquals(1, o.toBlocking.lastOrElse(2))
  }

  @Test
  def testLastOrElseWithEmpty() {
    val o = Observable.empty
    assertEquals(2, o.toBlocking.lastOrElse(2))
  }

  @Test
  def testLastOrElseWithMultipleItems() {
    val o = Observable.just(1, 2)
    assertEquals(2, o.toBlocking.lastOrElse(3))
  }

  @Test
  def testToFuture() {
    val o = Observable.just(1)
    val r = Await.result(o.toBlocking.toFuture, 10 seconds)
    assertEquals(1, r)
  }

  @Test(expected = classOf[NoSuchElementException])
  def testToFutureWithEmpty() {
    val o = Observable.empty
    Await.result(o.toBlocking.toFuture, 10 seconds)
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testToFutureWithMultipleItems() {
    val o = Observable.just(1, 2)
    Await.result(o.toBlocking.toFuture, 10 seconds)
  }
}
