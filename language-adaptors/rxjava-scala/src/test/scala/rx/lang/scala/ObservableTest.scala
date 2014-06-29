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

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import org.junit.Assert._
import org.junit.{ Ignore, Test }
import org.scalatest.junit.JUnitSuite
import scala.concurrent.duration._
import scala.language.postfixOps
import rx.lang.scala.schedulers.TestScheduler
import rx.lang.scala.subjects.BehaviorSubject
import org.mockito.Mockito._
import org.mockito.Matchers._

class ObservableTests extends JUnitSuite {

  // Tests which needn't be run:

  @Ignore
  def testCovariance = {
    //println("hey, you shouldn't run this test")

    val o1: Observable[Nothing] = Observable.empty
    val o2: Observable[Int] = o1
    val o3: Observable[App] = o1
    val o4: Observable[Any] = o2
    val o5: Observable[Any] = o3
  }

  // Tests which have to be run:

  @Test
  def testDematerialize() {
    val o = List(1, 2, 3).toObservable
    val mat = o.materialize
    val demat = mat.dematerialize

    //correctly rejected:
    //val wrongDemat = Observable("hello").dematerialize

    assertEquals(demat.toBlocking.toIterable.toList, List(1, 2, 3))
}

  @Test def TestScan() {
     val xs = Observable.items(0,1,2,3)
     val ys = xs.scan(0)(_+_)
     assertEquals(List(0,0,1,3,6), ys.toBlocking.toList)
     val zs = xs.scan((x: Int, y:Int) => x*y)
     assertEquals(List(0, 0, 0, 0), zs.toBlocking.toList)
  }

  // Test that Java's firstOrDefault propagates errors.
  // If this changes (i.e. it suppresses errors and returns default) then Scala's firstOrElse
  // should be changed accordingly.
  @Test def testJavaFirstOrDefault() {
    assertEquals(1, rx.Observable.from(1, 2).firstOrDefault(10).toBlocking().single)
    assertEquals(10, rx.Observable.empty().firstOrDefault(10).toBlocking().single)
    val msg = "msg6251"
    var receivedMsg = "none"
    try {
      rx.Observable.error(new Exception(msg)).firstOrDefault(10).toBlocking().single
    } catch {
      case e: Exception => receivedMsg = e.getCause().getMessage()
    }
    assertEquals(receivedMsg, msg)
  }

  @Test def testFirstOrElse() {
    def mustNotBeCalled: String = sys.error("this method should not be called")
    def mustBeCalled: String = "this is the default value"
    assertEquals("hello", Observable.items("hello").firstOrElse(mustNotBeCalled).toBlocking.single)
    assertEquals("this is the default value", Observable.empty.firstOrElse(mustBeCalled).toBlocking.single)
  }

  @Test def testTestWithError() {
    val msg = "msg6251"
    var receivedMsg = "none"
    try {
      Observable.error[Int](new Exception(msg)).firstOrElse(10).toBlocking.single
    } catch {
      case e: Exception => receivedMsg = e.getCause().getMessage()
    }
    assertEquals(receivedMsg, msg)
  }

  @Test def testFromFuture() {
    val o = Observable from Future { 5 }
    assertEquals(5, o.toBlocking.single)
  }

  @Test def testFromFutureWithDelay() {
    val o = Observable from Future { Thread.sleep(200); 42 }
    assertEquals(42, o.toBlocking.single)
  }

  @Test def testFromFutureWithError() {
    val err = new Exception("ooops42")
    val o: Observable[Int] = Observable from Future { Thread.sleep(200); throw err }
    assertEquals(List(Notification.OnError(err)), o.materialize.toBlocking.toList)
  }

  @Test def testFromFutureWithSubscribeOnlyAfterCompletion() {
    val f = Future { Thread.sleep(200); 6 }
    val o = Observable from f
    val res = Await.result(f, Duration.Inf)
    assertEquals(6, res)
    assertEquals(6, o.toBlocking.single)
  }

  @Test def testJoin() {
     val xs = Observable.items(1,2,3)
     val ys = Observable.items("a")
     val zs = xs.join[String,String](ys, x => Observable.never, y => Observable.never, (x,y) => y+x)
     assertEquals(List("a1", "a2", "a3"),zs.toBlocking.toList)
  }

  @Test def testTimestampWithScheduler() {
    val c = 10
    val s = TestScheduler()
    val o1 = Observable interval (1.milliseconds, s) map (_ + 1)
    val o2 = o1 timestamp s
    val l = ListBuffer[(Long, Long)]()
    o2.subscribe (
      onNext = (l += _)
    )
    s advanceTimeTo c.milliseconds
    val (l1, l2) = l.toList.unzip
    assertTrue(l1.size == c)
    assertEquals(l2, l1)
  }

  @Test def testHead() {
    val o: Observable[String] = List("alice", "bob", "carol").toObservable.head
    assertEquals(List("alice"), o.toBlocking.toList)
  }

  @Test(expected = classOf[NoSuchElementException])
  def testHeadWithEmptyObservable() {
    val o: Observable[String] = List[String]().toObservable.head
    o.toBlocking.toList
  }

  @Test def testTail() {
    val o: Observable[String] = List("alice", "bob", "carol").toObservable.tail
    assertEquals(List("bob", "carol"), o.toBlocking.toList)
    assertEquals(List("bob", "carol"), o.toBlocking.toList)
  }

  @Test(expected = classOf[UnsupportedOperationException])
  def testTailWithEmptyObservable() {
    val o: Observable[String] = List[String]().toObservable.tail
    o.toBlocking.toList
  }

  @Test
  def testZipWithIndex() {
    val o = List("alice", "bob", "carol").toObservable.zipWithIndex.map(_._2)
    assertEquals(List(0, 1, 2), o.toBlocking.toList)
    assertEquals(List(0, 1, 2), o.toBlocking.toList)
  }

  @Test
  def testSingleOrElse() {
    val o = Observable.items(1).singleOrElse(2)
    assertEquals(1, o.toBlocking.single)
  }

  @Test
  def testSingleOrElseWithEmptyObservable() {
    val o: Observable[Int] = Observable.empty.singleOrElse(1)
    assertEquals(1, o.toBlocking.single)
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testSingleOrElseWithTooManyItems() {
    Observable.items(1, 2).singleOrElse(1).toBlocking.single
  }

  @Test
  def testSingleOrElseWithCallByName() {
    var called = false
    val o: Observable[Int] = Observable.empty.singleOrElse {
      called = true
      1
    }
    assertFalse(called)
    o.subscribe()
    assertTrue(called)
  }

  @Test
  def testSingleOrElseWithCallByName2() {
    var called = false
    val o = Observable.items(1).singleOrElse {
      called = true
      2
    }
    assertFalse(called)
    o.subscribe()
    assertFalse(called)
  }

  @Test
  def testOrElse() {
    val o = Observable.items(1, 2, 3).orElse(4)
    assertEquals(List(1, 2, 3), o.toBlocking.toList)
  }

  @Test
  def testOrElseWithEmpty() {
    val o = Observable.empty.orElse(-1)
    assertEquals(List(-1), o.toBlocking.toList)
  }

  @Test
  def testToMultimap() {
    val o = Observable.items("a", "b", "cc", "dd").toMultimap(_.length)
    val expected = Map(1 -> List("a", "b"), 2 -> List("cc", "dd"))
    assertEquals(expected, o.toBlocking.single)
  }

  @Test
  def testToMultimapWithValueSelector() {
    val o = Observable.items("a", "b", "cc", "dd").toMultimap(_.length, s => s + s)
    val expected = Map(1 -> List("aa", "bb"), 2 -> List("cccc", "dddd"))
    assertEquals(expected, o.toBlocking.single)
  }

  @Test
  def testToMultimapWithMapFactory() {
    val m = mutable.Map[Int, mutable.Buffer[String]]()
    val o = Observable.items("a", "b", "cc", "dd").toMultimap(_.length, s => s, () => m)
    val expected = Map(1 -> List("a", "b"), 2 -> List("cc", "dd"))
    val r = o.toBlocking.single
    // r should be the same instance created by the `mapFactory`
    assertTrue(m eq r)
    assertEquals(expected, r)
  }

  @Test
  def testToMultimapWithBufferFactory() {
    val m = mutable.Map[Int, mutable.Buffer[String]]()
    val ls = List(mutable.Buffer[String](), mutable.Buffer[String]())
    val o = Observable.items("a", "b", "cc", "dd").toMultimap(_.length, s => s, () => m, (i: Int) => ls(i - 1))
    val expected = Map(1 -> List("a", "b"), 2 -> List("cc", "dd"))
    val r = o.toBlocking.single
    // r should be the same instance created by the `mapFactory`
    assertTrue(m eq r)
    // r(1) should be the same instance created by the first calling `bufferFactory`
    assertTrue(ls(0) eq r(1))
    // r(2) should be the same instance created by the second calling `bufferFactory`
    assertTrue(ls(1) eq r(2))
    assertEquals(expected, r)
  }

  @Test
  def testCreate() {
    var called = false
    val o = Observable.create[String](observer => {
      observer.onNext("a")
      observer.onNext("b")
      observer.onNext("c")
      observer.onCompleted()
      Subscription {
        called = true
      }
    })
    assertEquals(List("a", "b", "c"), o.toBlocking.toList)
    assertTrue(called)
  }

  @Test
  def testToTraversable() {
    val o = Observable.items(1, 2, 3).toTraversable
    assertEquals(Seq(1, 2, 3), o.toBlocking.single)
  }

  @Test
  def testToList() {
    val o = Observable.items(1, 2, 3).toList
    assertEquals(Seq(1, 2, 3), o.toBlocking.single)
  }

  @Test
  def testToIterable() {
    val o = Observable.items(1, 2, 3).toIterable
    assertEquals(Seq(1, 2, 3), o.toBlocking.single)
  }

  @Test
  def testToIterator() {
    val o = Observable.items(1, 2, 3).toIterator
    assertEquals(Seq(1, 2, 3), o.toBlocking.single.toSeq)
  }

  @Test
  def testToStream() {
    val o = Observable.items(1, 2, 3).toStream
    assertEquals(Seq(1, 2, 3), o.toBlocking.single)
  }

  @Test
  def testToIndexedSeq() {
    val o = Observable.items(1, 2, 3).toIndexedSeq
    assertEquals(Seq(1, 2, 3), o.toBlocking.single)
  }

  @Test
  def testToBuffer() {
    val o = Observable.items(1, 2, 3).toBuffer
    assertEquals(Seq(1, 2, 3), o.toBlocking.single)
  }

  @Test
  def testToSet() {
    val o = Observable.items(1, 2, 2).toSet
    assertEquals(Set(1, 2), o.toBlocking.single)
  }

  @Test
  def testToVector() {
    val o = Observable.items(1, 2, 3).toVector
    assertEquals(Seq(1, 2, 3), o.toBlocking.single)
  }

  @Test
  def testToArray() {
    val o = Observable.items(1, 2, 3).toArray
    assertArrayEquals(Array(1, 2, 3), o.toBlocking.single)
  }
}
