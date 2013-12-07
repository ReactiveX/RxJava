package rx.lang.scala


import org.junit.{Assert, Test}
import org.junit.Assert
import org.scalatest.junit.JUnitSuite
import scala.concurrent.duration._
import scala.language.postfixOps
import org.mockito.Mockito._
import org.mockito.Matchers._
import rx.lang.scala.Notification.{OnError, OnNext}


class NotificationTests extends JUnitSuite {
  @Test
  def creation() {

    val onNext = OnNext(42)
    Assert.assertEquals(42, onNext match { case OnNext(value) => value })
  }
}
