package rx.lang.scala.examples

import org.junit.Test
import org.scalatest.junit.JUnitSuite
import scala.concurrent.duration._
import scala.language.postfixOps
import rx.lang.scala.{ Observable, Observer }
import rx.lang.scala.concurrency.TestScheduler

class TestSchedulerExample extends JUnitSuite {

  @Test def testInterval() {
    import org.mockito.Matchers._
    import org.mockito.Mockito._

    val scheduler = TestScheduler()
    // Use a Java Observer for Mockito
    val observer = mock(classOf[rx.Observer[Long]])

    val o = Observable.interval(1 second, scheduler)

    // Wrap Java Observer in Scala Observer, then subscribe
    val sub = o.subscribe(Observer(observer))

    verify(observer, never).onNext(0L)
    verify(observer, never).onCompleted()
    verify(observer, never).onError(any(classOf[Throwable]))

    scheduler.advanceTimeTo(2 seconds)

    val inOrdr = inOrder(observer)
    inOrdr.verify(observer, times(1)).onNext(0L)
    inOrdr.verify(observer, times(1)).onNext(1L)
    inOrdr.verify(observer, never).onNext(2L)
    verify(observer, never).onCompleted()
    verify(observer, never).onError(any(classOf[Throwable]))

    verify(observer, never).onNext(2L)
    
    sub.unsubscribe()

    scheduler.advanceTimeTo(4 seconds)
    
    // after unsubscription we expect no further events
    verifyNoMoreInteractions(observer)
  }

}
