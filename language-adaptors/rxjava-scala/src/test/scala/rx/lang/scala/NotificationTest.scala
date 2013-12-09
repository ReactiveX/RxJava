package rx.lang.scala

import org.scalatest.junit.JUnitSuite
import org.junit.Test
import rx.lang.scala.Notification.OnCompleted

class NotificationTest extends JUnitSuite {
  @Test
  def testOnCompletePatternMatch(): Unit = {
    val o = Observable()

    val o2 = o.materialize map {
      case OnCompleted() => true
      case _ => false
    }
    assert(o2.toBlockingObservable.toIterable.toList === List(true))
  }
}
