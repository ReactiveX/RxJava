package rx.lang.scala.concurrency

import scala.concurrent.duration.Duration
import rx.lang.scala.Scheduler
import org.scalatest.junit.JUnitSuite

/**
 * Scheduler with artificial time, useful for testing.
 */
class TestScheduler extends Scheduler {
  val asJava = new rx.concurrency.TestScheduler

  def advanceTimeBy(time: Duration) {
    asJava.advanceTimeBy(time.length, time.unit)
  }

  def advanceTimeTo(time: Duration) {
    asJava.advanceTimeTo(time.length, time.unit)
  }

  def triggerActions() {
    asJava.triggerActions()
  }
}

/**
 * Provides constructors for `TestScheduler`.
 */
object TestScheduler {
  def apply(): TestScheduler = {
    new TestScheduler
  }
}

private class UnitTest extends JUnitSuite {
  import org.mockito.Matchers._
  import org.mockito.Mockito._
  import org.junit.{Test, Before}
  import scala.concurrent.duration._
  import scala.language.postfixOps
  import rx.lang.scala.{Observable, Observer}

  var scheduler: TestScheduler = null
  var observer: Observer[Long] = null
  var observer2: Observer[Long] = null

  @Before def before() {
    scheduler = TestScheduler()
    observer = mock(classOf[rx.Observer[Long]])
  }

  @Test def testInterval() {
    val w = Observable.interval(1 second, scheduler)
    val sub = w.subscribe(observer)

    verify(observer, never()).onNext(0L)
    verify(observer, never()).onCompleted()
    verify(observer, never()).onError(any(classOf[Throwable]))

    scheduler.advanceTimeTo(2 seconds)

    val inOrdr = inOrder(observer);
    inOrdr.verify(observer, times(1)).onNext(0L)
    inOrdr.verify(observer, times(1)).onNext(1L)
    inOrdr.verify(observer, never()).onNext(2L)
    verify(observer, never()).onCompleted()
    verify(observer, never()).onError(any(classOf[Throwable]))

    sub.unsubscribe();
    scheduler.advanceTimeTo(4 seconds)
    verify(observer, never()).onNext(2L)
    verify(observer, times(1)).onCompleted()
    verify(observer, never()).onError(any(classOf[Throwable]))
  }
}

