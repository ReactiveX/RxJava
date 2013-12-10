package rx.lang.scala.examples

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import org.junit.Test
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.junit.JUnitSuite

import rx.lang.scala._
import rx.lang.scala.schedulers.TestScheduler

class TestSchedulerExample extends JUnitSuite {

  @Test def testInterval() {
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


