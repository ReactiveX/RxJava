package rx.lang.scala

import org.junit.{Assert, Test}
import org.scalatest.junit.JUnitSuite
import scala.concurrent.duration._
import scala.language.postfixOps
import rx.lang.scala.schedulers.TestScheduler
import rx.lang.scala.subjects.{AsyncSubject, ReplaySubject, BehaviorSubject}
import org.mockito.Mockito._
import org.mockito.Matchers._


/**
 * No fucking clue how to properly mock traits.
 * Some old-school imperative code works just as well.
 */
class SubjectTest extends JUnitSuite {

  @Test def PublishSubjectIsAChannel() {

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

    val channel: BehaviorSubject[Integer] = BehaviorSubject(2013)

    val a = channel.subscribe(observerA)

      Assert.assertEquals(2013, lastA)

    val b = channel.subscribe(observerB)

      Assert.assertEquals(2013, lastB)

    channel.onNext(42)

      Assert.assertEquals(42, lastA)
      Assert.assertEquals(42, lastB)

    a.unsubscribe()
    channel.onNext(4711)

      Assert.assertEquals(42, lastA)
      Assert.assertEquals(4711, lastB)

    channel.onCompleted()

      Assert.assertFalse(completedA)
      Assert.assertTrue(completedB)
      Assert.assertEquals(42, lastA)
      Assert.assertEquals(4711, lastB)

    val c = channel.subscribe(observerC)
    channel.onNext(13)

      Assert.assertEquals(null, lastC)
      Assert.assertTrue(completedC)

      Assert.assertFalse(completedA)
      Assert.assertTrue(completedB)
      Assert.assertEquals(42, lastA)
      Assert.assertEquals(4711, lastB)

    channel.onError(new Exception("!"))

      Assert.assertEquals(null, lastC)
      Assert.assertTrue(completedC)

      Assert.assertFalse(completedA)
      Assert.assertTrue(completedB)
      Assert.assertEquals(42, lastA)
      Assert.assertEquals(4711, lastB)


  }

  @Test def ReplaySubjectIsAChannel() {

    val channel = ReplaySubject[Int]
    
    var lastA: Integer = null
    var errorA, completedA: Boolean = false
    val a = channel.subscribe(x => { lastA = x}, e => { errorA = true} , () => { completedA = true })

    var lastB: Integer = null
    var errorB, completedB: Boolean = false
    val b = channel.subscribe(x => { lastB = x}, e => { errorB = true} , () => { completedB = true })

    channel.onNext(42)

      Assert.assertEquals(42, lastA)
      Assert.assertEquals(42, lastB)
    
    a.unsubscribe()

    channel.onNext(4711)

      Assert.assertEquals(42, lastA)
      Assert.assertEquals(4711, lastB)
    
    channel.onCompleted()

    Assert.assertEquals(42, lastA)
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

      Assert.assertEquals(42, lastA)
      Assert.assertFalse(completedA)
      Assert.assertFalse(errorA)

      Assert.assertEquals(4711, lastB)
      Assert.assertTrue(completedB)
      Assert.assertFalse(errorB)

      Assert.assertEquals(4711, lastC)
      Assert.assertTrue(completedC)
      Assert.assertFalse(errorC)

    channel.onError(new Exception("Boom"))

      Assert.assertEquals(42, lastA)
      Assert.assertFalse(completedA)
      Assert.assertFalse(errorA)

      Assert.assertEquals(4711, lastB)
      Assert.assertTrue(completedB)
      Assert.assertFalse(errorB)

      Assert.assertEquals(4711, lastC)
      Assert.assertTrue(completedC)
      Assert.assertFalse(errorC)
  }

  @Test def BehaviorSubjectIsACache() {

    val channel = BehaviorSubject(2013)

    var lastA: Integer = null
    var errorA, completedA: Boolean = false
    val a = channel.subscribe(x => { lastA = x}, e => { errorA = true} , () => { completedA = true })

    var lastB: Integer = null
    var errorB, completedB: Boolean = false
    val b = channel.subscribe(x => { lastB = x}, e => { errorB = true} , () => { completedB = true })

    Assert.assertEquals(2013, lastA)
    Assert.assertEquals(2013, lastB)

    channel.onNext(42)

    Assert.assertEquals(42, lastA)
    Assert.assertEquals(42, lastB)

    a.unsubscribe()

    channel.onNext(4711)

    Assert.assertEquals(42, lastA)
    Assert.assertEquals(4711, lastB)

    channel.onCompleted()

    var lastC: Integer = null
    var errorC, completedC: Boolean = false
    val c = channel.subscribe(x => { lastC = x}, e => { errorC = true} , () => { completedC = true })

    Assert.assertEquals(null, lastC)
    Assert.assertTrue(completedC)
    Assert.assertFalse(errorC)

    channel.onNext(13)

    Assert.assertEquals(42, lastA)
    Assert.assertFalse(completedA)
    Assert.assertFalse(errorA)

    Assert.assertEquals(4711, lastB)
    Assert.assertTrue(completedB)
    Assert.assertFalse(errorB)

    Assert.assertEquals(null, lastC)
    Assert.assertTrue(completedC)
    Assert.assertFalse(errorC)

    channel.onError(new Exception("Boom"))

    Assert.assertEquals(42, lastA)
    Assert.assertFalse(completedA)
    Assert.assertFalse(errorA)

    Assert.assertEquals(4711, lastB)
    Assert.assertTrue(completedB)
    Assert.assertFalse(errorB)

    Assert.assertEquals(null, lastC)
    Assert.assertTrue(completedC)
    Assert.assertFalse(errorC)

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