package rx.lang.scala

import org.junit.{Assert, Test}
import org.scalatest.junit.JUnitSuite
import scala.concurrent.duration._
import scala.language.postfixOps
import rx.lang.scala.schedulers.TestScheduler
import rx.lang.scala.subjects.BehaviorSubject
import org.mockito.Mockito._
import org.mockito.Matchers._


/**
 * No fucking clue how to properly mock traits.
 * Some old-school imperative code works just as well.
 */
class SubjectTest extends JUnitSuite {

  @Test def PublishSubjectIsAChannel() {

    val zzz = Observer[Integer]()

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

}