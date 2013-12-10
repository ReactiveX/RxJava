package rx.lang.scala

import org.junit.{Assert, Test}
import org.scalatest.junit.JUnitSuite
import scala.concurrent.duration._
import scala.language.postfixOps
import rx.lang.scala.schedulers.TestScheduler
import rx.lang.scala.subjects.{AsyncSubject, ReplaySubject, BehaviorSubject}
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Ignore
import org.junit.Test
import org.scalatest.junit.JUnitSuite

class SubjectTest extends JUnitSuite {

  @Test def SubjectIsAChannel() {

    var lastA: Integer = null
    var errorA: Throwable = null
    var completedA: Boolean = false
    val observerA = Observer[Integer](
      (next: Integer) => { lastA = next },
      (error: Throwable) => { errorA = error },
      () => { completedA = true }
    )

    var lastB: Integer = null
    var errorB: Throwable = null
    var completedB: Boolean = false
    val observerB = Observer[Integer](
      (next: Integer) => { lastB = next },
      (error: Throwable) => { errorB = error },
      () => { completedB = true }
    )

    var lastC: Integer = null
    var errorC: Throwable = null
    var completedC: Boolean = false
    val observerC = Observer[Integer](
      (next: Integer) => { lastC = next },
      (error: Throwable) => { errorC = error },
      () => { completedC = true }
    )

    val channel: Subject[Integer] = Subject[Integer]()

    val a = channel(observerA)
    val b = channel(observerB)

      assertEquals(null, lastA)
      assertEquals(null, lastB)

    channel.onNext(42)

      assertEquals(42, lastA)
      assertEquals(42, lastB)

    a.unsubscribe()
    channel.onNext(4711)

      assertEquals(42, lastA)
      assertEquals(4711, lastB)

    channel.onCompleted()

      assertFalse(completedA)
      assertTrue(completedB)
      assertEquals(42, lastA)
      assertEquals(4711, lastB)

    val c = channel.subscribe(observerC)
    channel.onNext(13)

      assertEquals(null, lastC)
      assertTrue(completedC)

      assertFalse(completedA)
      assertTrue(completedB)
      assertEquals(42, lastA)
      assertEquals(4711, lastB)

    channel.onError(new Exception("!"))

      assertEquals(null, lastC)
      assertTrue(completedC)

      assertFalse(completedA)
      assertTrue(completedB)
      assertEquals(42, lastA)
      assertEquals(4711, lastB)
  }

  @Test def ReplaySubjectIsAChannel() {

    val channel = ReplaySubject[Integer]

    var lastA: Integer = null
    var errorA, completedA: Boolean = false
    val a = channel.subscribe(x => { lastA = x}, e => { errorA = true} , () => { completedA = true })

    var lastB: Integer = null
    var errorB, completedB: Boolean = false

    val b = channel(new Observer[Integer] {
      override def onNext(value: Integer): Unit = { lastB = value }
      override def onError(error: Throwable): Unit = { errorB = true }
      override def onCompleted(): Unit = { completedB = true }
    })

    channel.onNext(42)

      assertEquals(42, lastA)
      assertEquals(42, lastB)
    
    a.unsubscribe()

    channel.onNext(4711)

      assertEquals(42, lastA)
      assertEquals(4711, lastB)
    
    channel.onCompleted()

      assertEquals(42, lastA)
      assertFalse(completedA)
      assertFalse(errorA)

      assertEquals(4711, lastB)
      assertTrue(completedB)
      assertFalse(errorB)

    var lastC: Integer = null
    var errorC, completedC: Boolean = false
    val c = channel.subscribe(x => { lastC = x}, e => { errorC = true} , () => { completedC = true })

      assertEquals(4711, lastC)
      assertTrue(completedC)
      assertFalse(errorC)

    channel.onNext(13)

      assertEquals(42, lastA)
      assertFalse(completedA)
      assertFalse(errorA)

      assertEquals(4711, lastB)
      assertTrue(completedB)
      assertFalse(errorB)

      assertEquals(4711, lastC)
      assertTrue(completedC)
      assertFalse(errorC)

    channel.onError(new Exception("Boom"))

      assertEquals(42, lastA)
      assertFalse(completedA)
      assertFalse(errorA)

      assertEquals(4711, lastB)
      assertTrue(completedB)
      assertFalse(errorB)

      assertEquals(4711, lastC)
      assertTrue(completedC)
      assertFalse(errorC)
  }

  @Test def BehaviorSubjectIsACache() {

    val channel = BehaviorSubject(2013)

    var lastA: Integer = null
    var errorA, completedA: Boolean = false
    val a = channel.subscribe(x => { lastA = x}, e => { errorA = true} , () => { completedA = true })

    var lastB: Integer = null
    var errorB, completedB: Boolean = false
    val b = channel.subscribe(x => { lastB = x}, e => { errorB = true} , () => { completedB = true })

      assertEquals(2013, lastA)
      assertEquals(2013, lastB)

    channel.onNext(42)

      assertEquals(42, lastA)
      assertEquals(42, lastB)

    a.unsubscribe()

    channel.onNext(4711)

      assertEquals(42, lastA)
      assertEquals(4711, lastB)

    channel.onCompleted()

    var lastC: Integer = null
    var errorC, completedC: Boolean = false
    val c = channel.subscribe(x => { lastC = x}, e => { errorC = true} , () => { completedC = true })

      assertEquals(null, lastC)
      assertTrue(completedC)
      assertFalse(errorC)

    channel.onNext(13)

      assertEquals(42, lastA)
      assertFalse(completedA)
      assertFalse(errorA)

      assertEquals(4711, lastB)
      assertTrue(completedB)
      assertFalse(errorB)

      assertEquals(null, lastC)
      assertTrue(completedC)
      assertFalse(errorC)

    channel.onError(new Exception("Boom"))

      assertEquals(42, lastA)
      assertFalse(completedA)
      assertFalse(errorA)

      assertEquals(4711, lastB)
      assertTrue(completedB)
      assertFalse(errorB)

      assertEquals(null, lastC)
      assertTrue(completedC)
      assertFalse(errorC)

  }

  @Test def AsyncSubjectIsAFuture() {

    val channel = AsyncSubject[Int]()

    var lastA: Integer = null
    var errorA, completedA: Boolean = false
    val a = channel.subscribe(x => { lastA = x}, e => { errorA = true} , () => { completedA = true })

    var lastB: Integer = null
    var errorB, completedB: Boolean = false
    val b = channel.subscribe(x => { lastB = x}, e => { errorB = true} , () => { completedB = true })

    channel.onNext(42)

      Assert.assertEquals(null, lastA)
      Assert.assertEquals(null, lastB)

    a.unsubscribe()
    channel.onNext(4711)
    channel.onCompleted()

      Assert.assertEquals(null, lastA)
      Assert.assertFalse(completedA)
      Assert.assertFalse(errorA)

      Assert.assertEquals(4711, lastB)
      Assert.assertTrue(completedB)
      Assert.assertFalse(errorB)


    var lastC: Integer = null
    var errorC, completedC: Boolean = false
    val c = channel.subscribe(x => { lastC = x}, e => { errorC = true} , () => { completedC = true })

      Assert.assertEquals(4711, lastC)
      Assert.assertTrue(completedC)
      Assert.assertFalse(errorC)

    channel.onNext(13)

      Assert.assertEquals(null, lastA)
      Assert.assertFalse(completedA)
      Assert.assertFalse(errorA)

      Assert.assertEquals(4711, lastB)
      Assert.assertTrue(completedB)
      Assert.assertFalse(errorB)

      Assert.assertEquals(4711, lastC)
      Assert.assertTrue(completedC)
      Assert.assertFalse(errorC)

    channel.onError(new Exception("Boom"))

      Assert.assertEquals(null, lastA)
      Assert.assertFalse(completedA)
      Assert.assertFalse(errorA)

      Assert.assertEquals(4711, lastB)
      Assert.assertTrue(completedB)
      Assert.assertFalse(errorB)

      Assert.assertEquals(4711, lastC)
      Assert.assertTrue(completedC)
      Assert.assertFalse(errorC)

  }

}