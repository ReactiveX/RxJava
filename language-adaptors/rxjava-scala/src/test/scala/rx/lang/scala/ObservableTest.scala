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

    assertEquals(demat.toBlockingObservable.toIterable.toList, List(1, 2, 3))
}

  @Test def TestScan() {
     val xs = Observable.items(0,1,2,3)
     val ys = xs.scan(0)(_+_)
     assertEquals(List(0,0,1,3,6), ys.toBlockingObservable.toList)
     val zs = xs.scan((x: Int, y:Int) => x*y)
     assertEquals(List(0, 0, 0, 0), zs.toBlockingObservable.toList)
  }

  // Test that Java's firstOrDefault propagates errors.
  // If this changes (i.e. it suppresses errors and returns default) then Scala's firstOrElse
  // should be changed accordingly.
  @Test def testJavaFirstOrDefault() {
    assertEquals(1, rx.Observable.from(1, 2).firstOrDefault(10).toBlockingObservable().single)
    assertEquals(10, rx.Observable.empty().firstOrDefault(10).toBlockingObservable().single)
    val msg = "msg6251"
    var receivedMsg = "none"
    try {
      rx.Observable.error(new Exception(msg)).firstOrDefault(10).toBlockingObservable().single
    } catch {
      case e: Exception => receivedMsg = e.getCause().getMessage()
    }
    assertEquals(receivedMsg, msg)
  }

  @Test def testFirstOrElse() {
    def mustNotBeCalled: String = sys.error("this method should not be called")
    def mustBeCalled: String = "this is the default value"
    assertEquals("hello", Observable.items("hello").firstOrElse(mustNotBeCalled).toBlockingObservable.single)
    assertEquals("this is the default value", Observable.empty.firstOrElse(mustBeCalled).toBlockingObservable.single)
  }

  @Test def testTestWithError() {
    val msg = "msg6251"
    var receivedMsg = "none"
    try {
      Observable.error[Int](new Exception(msg)).firstOrElse(10).toBlockingObservable.single
    } catch {
      case e: Exception => receivedMsg = e.getCause().getMessage()
    }
    assertEquals(receivedMsg, msg)
  }

  @Test def testFromFuture() {
    val o = Observable from Future { 5 }
    assertEquals(5, o.toBlockingObservable.single)
  }

  @Test def testFromFutureWithDelay() {
    val o = Observable from Future { Thread.sleep(200); 42 }
    assertEquals(42, o.toBlockingObservable.single)
  }

  @Test def testFromFutureWithError() {
    val err = new Exception("ooops42")
    val o: Observable[Int] = Observable from Future { Thread.sleep(200); throw err }
    assertEquals(List(Notification.OnError(err)), o.materialize.toBlockingObservable.toList)
  }

  @Test def testFromFutureWithSubscribeOnlyAfterCompletion() {
    val f = Future { Thread.sleep(200); 6 }
    val o = Observable from f
    val res = Await.result(f, Duration.Inf)
    assertEquals(6, res)
    assertEquals(6, o.toBlockingObservable.single)
  }

  @Test def testJoin() {
     val xs = Observable.items(1,2,3)
     val ys = Observable.items("a")
     val zs = xs.join[String,String](ys, x => Observable.never, y => Observable.never, (x,y) => y+x)
     assertEquals(List("a1", "a2", "a3"),zs.toBlockingObservable.toList)
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
    assertEquals(List("alice"), o.toBlockingObservable.toList)
  }

  @Test(expected = classOf[NoSuchElementException])
  def testHeadWithEmptyObservable() {
    val o: Observable[String] = List[String]().toObservable.head
    o.toBlockingObservable.toList
  }

  @Test def testTail() {
    val o: Observable[String] = List("alice", "bob", "carol").toObservable.tail
    assertEquals(List("bob", "carol"), o.toBlockingObservable.toList)
    assertEquals(List("bob", "carol"), o.toBlockingObservable.toList)
  }

  @Test(expected = classOf[UnsupportedOperationException])
  def testTailWithEmptyObservable() {
    val o: Observable[String] = List[String]().toObservable.tail
    o.toBlockingObservable.toList
  }

  @Test
  def testZipWithIndex() {
    val o = List("alice", "bob", "carol").toObservable.zipWithIndex.map(_._2)
    assertEquals(List(0, 1, 2), o.toBlockingObservable.toList)
    assertEquals(List(0, 1, 2), o.toBlockingObservable.toList)
  }

}
