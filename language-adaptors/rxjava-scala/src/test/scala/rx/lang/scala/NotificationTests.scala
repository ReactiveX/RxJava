package rx.lang.scala


import org.junit.{Assert, Test}
import org.junit.Assert._
import org.scalatest.junit.JUnitSuite
import scala.concurrent.duration._
import scala.language.postfixOps
import org.mockito.Mockito._
import org.mockito.Matchers._
import rx.lang.scala.Notification.{OnCompleted, OnError, OnNext}


class NotificationTests extends JUnitSuite {
  @Test
  def creation() {

    val onNext = OnNext(42)
      assertEquals(42, onNext match { case OnNext(value) => value })

    val oops = new Exception("Oops")
    val onError = OnError(oops)
      assertEquals(oops, onError match { case OnError(error) => error })

    val onCompleted = OnCompleted()
      assertEquals((), onCompleted match { case OnCompleted() => () })
  }

  @Test
  def accept() {

    val onNext = OnNext(42)
      assertEquals(42, onNext(x=>42, e=>4711,()=>13))

    val oops = new Exception("Oops")
    val onError = OnError(oops)
      assertEquals(4711, onError(x=>42, e=>4711,()=>13))

    val onCompleted = OnCompleted()
      assertEquals(13, onCompleted(x=>42, e=>4711,()=>13))

  }
}
